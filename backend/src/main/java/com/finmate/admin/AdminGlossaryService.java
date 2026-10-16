package com.finmate.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finmate.common.ApiException;
import com.finmate.glossary.GlossaryCategoryRepository;
import com.finmate.glossary.GlossaryContentChangedEvent;
import com.finmate.glossary.GlossaryStatus;
import com.finmate.glossary.GlossaryTerm;
import com.finmate.glossary.GlossaryTermRepository;
import com.finmate.learning.LearningCatalog;
import com.finmate.user.User;
import com.finmate.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자의 용어 편집·게시 흐름.
 *
 * <p>공개 중인 용어는 바로 고치지 않고 초안(glossary_term_drafts)에 저장한 뒤 publish로 반영한다.
 * 새 용어는 비공개(DRAFT)로 만들어지고, 공개를 내리면 다시 DRAFT가 된다. 공개 상태가 바뀔 때마다
 * 불변 기록(glossary_term_revisions)을 남기고, 커밋 뒤 검색 인덱스가 다시 만들어지도록 이벤트를 보낸다.
 */
@Service
public class AdminGlossaryService {

    private static final List<String> LEVELS = List.of("BASIC", "INTERMEDIATE", "ADVANCED");

    private final AdminAuthorizer authorizer;
    private final GlossaryTermRepository terms;
    private final GlossaryTermDraftRepository drafts;
    private final GlossaryTermRevisionRepository revisions;
    private final GlossaryCategoryRepository categories;
    private final LearningCatalog concepts;
    private final UserRepository users;
    private final EntityManager entityManager;
    private final ObjectMapper mapper;
    private final ApplicationEventPublisher events;

    public AdminGlossaryService(AdminAuthorizer authorizer, GlossaryTermRepository terms,
            GlossaryTermDraftRepository drafts, GlossaryTermRevisionRepository revisions,
            GlossaryCategoryRepository categories, LearningCatalog concepts, UserRepository users,
            EntityManager entityManager, ObjectMapper mapper, ApplicationEventPublisher events) {
        this.authorizer = authorizer;
        this.terms = terms;
        this.drafts = drafts;
        this.revisions = revisions;
        this.categories = categories;
        this.concepts = concepts;
        this.users = users;
        this.entityManager = entityManager;
        this.mapper = mapper;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public AdminGlossaryDtos.Terms list(String userId) {
        authorizer.requireAdmin(userId);
        Map<String, GlossaryTermDraft> bySlug = drafts.findAll().stream()
                .collect(Collectors.toMap(GlossaryTermDraft::getSlug, Function.identity()));
        return new AdminGlossaryDtos.Terms(terms.findAll().stream()
                .sorted(Comparator.comparing(GlossaryTerm::getCategoryId)
                        .thenComparing(GlossaryTerm::getTerm))
                .map(term -> response(term, bySlug.get(term.getSlug())))
                .toList());
    }

    @Transactional(readOnly = true)
    public AdminGlossaryDtos.Term get(String userId, String slug) {
        authorizer.requireAdmin(userId);
        GlossaryTerm term = requireTerm(slug);
        return response(term, drafts.findById(slug).orElse(null));
    }

    @Transactional
    public AdminGlossaryDtos.Term create(String userId, AdminGlossaryDtos.CreateRequest request) {
        authorizer.requireAdmin(userId);
        if (terms.existsById(request.slug())) {
            throw ApiException.conflict("이미 있는 용어 slug입니다: " + request.slug());
        }
        validate(request.content(), request.slug());
        GlossaryTerm term = new GlossaryTerm(request.slug());
        apply(term, request.content());
        term.setStatus(GlossaryStatus.DRAFT);
        term.setContentVersion(newVersion());
        terms.saveAndFlush(term);
        record(term, "CREATED", userId);
        return response(term, null);
    }

    @Transactional
    public AdminGlossaryDtos.Term edit(String userId, String slug, AdminGlossaryDtos.EditRequest request) {
        authorizer.requireAdmin(userId);
        GlossaryTerm term = requireTerm(slug);
        validate(request, slug);
        String baseVersion = drafts.findById(slug).map(GlossaryTermDraft::getBaseVersion)
                .orElse(term.getContentVersion());
        GlossaryTermDraft draft = new GlossaryTermDraft(slug, baseVersion, userId, toJson(request), Instant.now());
        drafts.save(draft);
        return response(term, draft);
    }

    /** 초안이 있으면 초안을 반영해 공개하고, 초안 없는 비공개 용어는 현재 내용 그대로 공개한다. */
    @Transactional
    public AdminGlossaryDtos.Term publish(String userId, String slug) {
        authorizer.requireAdmin(userId);
        GlossaryTerm term = entityManager.find(GlossaryTerm.class, slug, LockModeType.PESSIMISTIC_WRITE);
        if (term == null) {
            throw ApiException.notFound("용어를 찾을 수 없습니다: " + slug);
        }
        GlossaryTermDraft draft = drafts.findById(slug).orElse(null);
        if (draft == null && term.getStatus() == GlossaryStatus.PUBLISHED) {
            throw ApiException.conflict("게시할 초안이 없습니다.");
        }
        if (draft != null) {
            if (!draft.getBaseVersion().equals(term.getContentVersion())) {
                throw ApiException.conflict("게시된 용어가 변경되었습니다. 초안을 다시 작성하세요.");
            }
            AdminGlossaryDtos.EditRequest request = fromJson(draft.getDraftJson());
            validate(request, slug);
            apply(term, request);
            term.setContentVersion(newVersion());
            drafts.delete(draft);
        }
        term.setStatus(GlossaryStatus.PUBLISHED);
        terms.saveAndFlush(term);
        record(term, "PUBLISHED", userId);
        events.publishEvent(new GlossaryContentChangedEvent(slug, "PUBLISHED"));
        return response(term, null);
    }

    /** 공개를 내린다. 내용은 그대로 두므로 다시 publish하면 같은 내용으로 복귀한다. */
    @Transactional
    public AdminGlossaryDtos.Term unpublish(String userId, String slug) {
        authorizer.requireAdmin(userId);
        GlossaryTerm term = entityManager.find(GlossaryTerm.class, slug, LockModeType.PESSIMISTIC_WRITE);
        if (term == null) {
            throw ApiException.notFound("용어를 찾을 수 없습니다: " + slug);
        }
        if (term.getStatus() != GlossaryStatus.PUBLISHED) {
            throw ApiException.conflict("이미 비공개 상태인 용어입니다.");
        }
        term.setStatus(GlossaryStatus.DRAFT);
        terms.saveAndFlush(term);
        record(term, "UNPUBLISHED", userId);
        events.publishEvent(new GlossaryContentChangedEvent(slug, "UNPUBLISHED"));
        return response(term, drafts.findById(slug).orElse(null));
    }

    @Transactional
    public void discard(String userId, String slug) {
        authorizer.requireAdmin(userId);
        requireTerm(slug);
        drafts.deleteById(slug);
    }

    @Transactional(readOnly = true)
    public AdminGlossaryDtos.Revisions revisions(String userId, String slug) {
        authorizer.requireAdmin(userId);
        requireTerm(slug);
        List<GlossaryTermRevision> found = revisions.findBySlugOrderByCreatedAtDesc(slug);
        Map<String, String> emails = users.findAllById(found.stream().map(GlossaryTermRevision::getEditorUserId)
                        .distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, User::getEmail));
        return new AdminGlossaryDtos.Revisions(slug, found.stream()
                .map(revision -> new AdminGlossaryDtos.Revision(revision.getId(), revision.getAction(),
                        revision.getContentVersion(), emails.getOrDefault(revision.getEditorUserId(), "(삭제된 계정)"),
                        revision.getCreatedAt()))
                .toList());
    }

    private void record(GlossaryTerm term, String action, String userId) {
        revisions.save(new GlossaryTermRevision(UUID.randomUUID().toString(), term.getSlug(), action,
                term.getContentVersion(), userId, toJson(snapshot(term)), Instant.now()));
    }

    private static String newVersion() {
        return "admin-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private GlossaryTerm requireTerm(String slug) {
        return terms.findById(slug)
                .orElseThrow(() -> ApiException.notFound("용어를 찾을 수 없습니다: " + slug));
    }

    /** JSON 시드 검증(GlossarySeedData)과 같은 수준의 규칙을 관리자 입력에도 적용한다. */
    private void validate(AdminGlossaryDtos.EditRequest request, String slug) {
        if (!categories.existsById(request.categoryId())) {
            throw ApiException.badRequest("존재하지 않는 용어 분류입니다: " + request.categoryId());
        }
        if (!LEVELS.contains(request.level())) {
            throw ApiException.badRequest("학습 수준은 BASIC, INTERMEDIATE, ADVANCED 중 하나여야 합니다.");
        }
        if (!concepts.contains(request.conceptSlug())) {
            throw ApiException.badRequest("존재하지 않는 학습 개념입니다: " + request.conceptSlug());
        }
        URI uri;
        try {
            uri = URI.create(request.sourceUrl());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("출처 URL 형식이 올바르지 않습니다.");
        }
        if (uri.getHost() == null || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) {
            throw ApiException.badRequest("출처 URL은 http 또는 https 주소여야 합니다.");
        }
        if (request.aliases().stream().anyMatch(value -> value == null || value.isBlank())
                || request.related().stream().anyMatch(value -> value == null || value.isBlank())) {
            throw ApiException.badRequest("별칭과 관련 용어에 빈 항목을 넣을 수 없습니다.");
        }
        for (String related : request.related()) {
            if (related.equals(slug)) {
                throw ApiException.badRequest("자기 자신을 관련 용어로 지정할 수 없습니다.");
            }
            if (!terms.existsById(related)) {
                throw ApiException.badRequest("존재하지 않는 관련 용어입니다: " + related);
            }
        }
    }

    private static void apply(GlossaryTerm term, AdminGlossaryDtos.EditRequest request) {
        term.setCategoryId(request.categoryId());
        term.setTerm(request.term());
        term.setFullName(request.fullName());
        term.setEnglish(request.english());
        term.setAliases(request.aliases());
        term.setLevel(request.level());
        term.setDefinition(request.definition());
        term.setExplanation(request.explanation());
        term.setFormula(request.formula());
        term.setExample(request.example());
        term.setCaution(request.caution());
        term.setRelated(request.related());
        term.setConceptSlug(request.conceptSlug());
        term.setSourceName(request.sourceName());
        term.setSourceUrl(request.sourceUrl());
    }

    private static AdminGlossaryDtos.EditRequest snapshot(GlossaryTerm term) {
        return new AdminGlossaryDtos.EditRequest(term.getCategoryId(), term.getTerm(), term.getFullName(),
                term.getEnglish(), term.getAliases(), term.getLevel(), term.getDefinition(), term.getExplanation(),
                term.getFormula(), term.getExample(), term.getCaution(), term.getRelated(), term.getConceptSlug(),
                term.getSourceName(), term.getSourceUrl());
    }

    private AdminGlossaryDtos.Term response(GlossaryTerm term, GlossaryTermDraft draft) {
        AdminGlossaryDtos.EditRequest editable = draft == null ? snapshot(term) : fromJson(draft.getDraftJson());
        return new AdminGlossaryDtos.Term(term.getSlug(), editable.categoryId(), editable.term(),
                editable.fullName(), editable.english(), editable.aliases(), editable.level(),
                editable.definition(), editable.explanation(), editable.formula(), editable.example(),
                editable.caution(), editable.related(), editable.conceptSlug(), editable.sourceName(),
                editable.sourceUrl(), term.getStatus().name(), term.getContentVersion(),
                draft == null ? term.getUpdatedAt() : draft.getUpdatedAt(), draft != null);
    }

    private String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("용어 초안을 직렬화하지 못했습니다.", e);
        }
    }

    private AdminGlossaryDtos.EditRequest fromJson(String json) {
        try {
            return mapper.readValue(json, AdminGlossaryDtos.EditRequest.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("저장된 용어 초안을 읽지 못했습니다.", e);
        }
    }
}
