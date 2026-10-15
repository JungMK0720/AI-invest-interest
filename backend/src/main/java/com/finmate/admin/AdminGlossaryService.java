package com.finmate.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finmate.common.ApiException;
import com.finmate.glossary.GlossaryCategoryRepository;
import com.finmate.glossary.GlossaryStatus;
import com.finmate.glossary.GlossaryTerm;
import com.finmate.glossary.GlossaryTermRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminGlossaryService {

    private final AdminAuthorizer authorizer;
    private final GlossaryTermRepository terms;
    private final GlossaryTermDraftRepository drafts;
    private final GlossaryCategoryRepository categories;
    private final EntityManager entityManager;
    private final ObjectMapper mapper;

    public AdminGlossaryService(AdminAuthorizer authorizer, GlossaryTermRepository terms,
            GlossaryTermDraftRepository drafts, GlossaryCategoryRepository categories,
            EntityManager entityManager, ObjectMapper mapper) {
        this.authorizer = authorizer;
        this.terms = terms;
        this.drafts = drafts;
        this.categories = categories;
        this.entityManager = entityManager;
        this.mapper = mapper;
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
    public AdminGlossaryDtos.Term edit(String userId, String slug, AdminGlossaryDtos.EditRequest request) {
        authorizer.requireAdmin(userId);
        GlossaryTerm term = requireTerm(slug);
        validate(request);
        String baseVersion = drafts.findById(slug).map(GlossaryTermDraft::getBaseVersion)
                .orElse(term.getContentVersion());
        GlossaryTermDraft draft = new GlossaryTermDraft(slug, baseVersion, userId, toJson(request), Instant.now());
        drafts.save(draft);
        return response(term, draft);
    }

    @Transactional
    public AdminGlossaryDtos.Term publish(String userId, String slug) {
        authorizer.requireAdmin(userId);
        GlossaryTerm term = entityManager.find(GlossaryTerm.class, slug, LockModeType.PESSIMISTIC_WRITE);
        if (term == null) {
            throw ApiException.notFound("용어를 찾을 수 없습니다: " + slug);
        }
        GlossaryTermDraft draft = drafts.findById(slug)
                .orElseThrow(() -> ApiException.conflict("게시할 초안이 없습니다."));
        if (!draft.getBaseVersion().equals(term.getContentVersion())) {
            throw ApiException.conflict("게시된 용어가 변경되었습니다. 초안을 다시 작성하세요.");
        }
        AdminGlossaryDtos.EditRequest request = fromJson(draft.getDraftJson());
        validate(request);
        apply(term, request);
        term.setStatus(GlossaryStatus.PUBLISHED);
        term.setContentVersion("admin-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8));
        terms.saveAndFlush(term);
        drafts.delete(draft);
        return response(term, null);
    }

    @Transactional
    public void discard(String userId, String slug) {
        authorizer.requireAdmin(userId);
        requireTerm(slug);
        drafts.deleteById(slug);
    }

    private GlossaryTerm requireTerm(String slug) {
        return terms.findById(slug)
                .orElseThrow(() -> ApiException.notFound("용어를 찾을 수 없습니다: " + slug));
    }

    private void validate(AdminGlossaryDtos.EditRequest request) {
        if (!categories.existsById(request.categoryId())) {
            throw ApiException.badRequest("존재하지 않는 용어 분류입니다: " + request.categoryId());
        }
        if (!List.of("BASIC", "INTERMEDIATE", "ADVANCED").contains(request.level())) {
            throw ApiException.badRequest("학습 수준은 BASIC, INTERMEDIATE, ADVANCED 중 하나여야 합니다.");
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

    private AdminGlossaryDtos.Term response(GlossaryTerm term, GlossaryTermDraft draft) {
        AdminGlossaryDtos.EditRequest editable = draft == null ? null : fromJson(draft.getDraftJson());
        return new AdminGlossaryDtos.Term(term.getSlug(),
                editable == null ? term.getCategoryId() : editable.categoryId(),
                editable == null ? term.getTerm() : editable.term(),
                editable == null ? term.getFullName() : editable.fullName(),
                editable == null ? term.getEnglish() : editable.english(),
                editable == null ? term.getAliases() : editable.aliases(),
                editable == null ? term.getLevel() : editable.level(),
                editable == null ? term.getDefinition() : editable.definition(),
                editable == null ? term.getExplanation() : editable.explanation(),
                editable == null ? term.getFormula() : editable.formula(),
                editable == null ? term.getExample() : editable.example(),
                editable == null ? term.getCaution() : editable.caution(),
                editable == null ? term.getRelated() : editable.related(),
                editable == null ? term.getConceptSlug() : editable.conceptSlug(),
                editable == null ? term.getSourceName() : editable.sourceName(),
                editable == null ? term.getSourceUrl() : editable.sourceUrl(),
                term.getStatus().name(), term.getContentVersion(),
                draft == null ? term.getUpdatedAt() : draft.getUpdatedAt(), draft != null);
    }

    private String toJson(AdminGlossaryDtos.EditRequest request) {
        try {
            return mapper.writeValueAsString(request);
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
