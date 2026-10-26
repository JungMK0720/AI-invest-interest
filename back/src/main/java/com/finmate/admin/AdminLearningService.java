package com.finmate.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finmate.common.ApiException;
import com.finmate.learning.LearningCatalog;
import com.finmate.learning.LearningConcept;
import com.finmate.learning.LearningConceptRepository;
import com.finmate.learning.LearningConceptStatus;
import com.finmate.learning.LearningContent;
import com.finmate.learning.LearningContentChangedEvent;
import com.finmate.learning.LearningContentStore;
import com.finmate.learning.LearningQuizItem;
import com.finmate.learning.LearningQuizItemRepository;
import com.finmate.learning.LearningTree;
import com.finmate.learning.SimulationCatalog;
import com.finmate.user.User;
import com.finmate.user.UserRepository;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자의 학습 개념·확인 문제 편집·게시 흐름.
 *
 * <p>용어 운영(AdminGlossaryService)과 같은 모양이다. 공개 중인 개념은 바로 고치지 않고 초안
 * (learning_concept_drafts)에 저장한 뒤 publish로 반영하고, 공개 상태가 바뀔 때마다 불변 기록
 * (learning_concept_revisions)을 남긴 뒤 커밋 후 카탈로그 캐시를 버린다.
 *
 * <p>용어와 다른 점은 개념이 트리라는 것이다. 게시·공개 내리기 직전에 "그렇게 하면 공개될 트리"를
 * {@link LearningTree#of}로 다시 만들어 보고, 부모·레벨·정답·해설·출처 규칙이 깨지면 409로 거절한다.
 * 이 검사가 다른 관리자의 동시 게시와 엇갈리지 않도록 개념 행 전체를 잠근 채 진행한다.
 */
@Service
public class AdminLearningService {

    private static final List<String> DIFFICULTIES = List.of("BASIC", "INTERMEDIATE", "ADVANCED");

    private final AdminAuthorizer authorizer;
    private final LearningConceptRepository concepts;
    private final LearningQuizItemRepository quizItems;
    private final LearningConceptDraftRepository drafts;
    private final LearningConceptRevisionRepository revisions;
    private final LearningContentStore store;
    private final LearningCatalog catalog;
    private final SimulationCatalog simulations;
    private final UserRepository users;
    private final ObjectMapper mapper;
    private final ApplicationEventPublisher events;

    public AdminLearningService(AdminAuthorizer authorizer, LearningConceptRepository concepts,
            LearningQuizItemRepository quizItems, LearningConceptDraftRepository drafts,
            LearningConceptRevisionRepository revisions, LearningContentStore store, LearningCatalog catalog,
            SimulationCatalog simulations, UserRepository users, ObjectMapper mapper,
            ApplicationEventPublisher events) {
        this.authorizer = authorizer;
        this.concepts = concepts;
        this.quizItems = quizItems;
        this.drafts = drafts;
        this.revisions = revisions;
        this.store = store;
        this.catalog = catalog;
        this.simulations = simulations;
        this.users = users;
        this.mapper = mapper;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public AdminLearningDtos.Concepts list(String userId) {
        authorizer.requireAdmin(userId);
        List<LearningConcept> all = concepts.findAll();
        Map<String, LearningConceptDraft> pending = drafts.findAll().stream()
                .collect(Collectors.toMap(LearningConceptDraft::getSlug, Function.identity()));
        Map<String, LearningContent.Node> saved = store.nodes(all).stream()
                .collect(Collectors.toMap(LearningContent.Node::slug, Function.identity()));
        List<AdminLearningDtos.ConceptSummary> items = treeOrder(all).stream().map(concept -> {
            LearningConceptDraft draft = pending.get(concept.getSlug());
            AdminLearningDtos.EditRequest content = draft == null
                    ? AdminLearningDtos.EditRequest.from(saved.get(concept.getSlug()))
                    : fromJson(draft.getDraftJson());
            return new AdminLearningDtos.ConceptSummary(concept.getSlug(), content.parent(), content.level(),
                    content.title(), content.quiz().size(), concept.getStatus().name(), concept.getContentVersion(),
                    draft == null ? concept.getUpdatedAt() : draft.getUpdatedAt(), draft != null);
        }).toList();
        return new AdminLearningDtos.Concepts(catalog.version(), items);
    }

    @Transactional(readOnly = true)
    public AdminLearningDtos.Concept get(String userId, String slug) {
        authorizer.requireAdmin(userId);
        LearningConcept concept = requireConcept(slug);
        return response(concept, drafts.findById(slug).orElse(null));
    }

    @Transactional
    public AdminLearningDtos.Concept edit(String userId, String slug, AdminLearningDtos.EditRequest request) {
        authorizer.requireAdmin(userId);
        LearningConcept concept = requireConcept(slug);
        validate(slug, request);
        String baseVersion = drafts.findById(slug).map(LearningConceptDraft::getBaseVersion)
                .orElse(concept.getContentVersion());
        LearningConceptDraft draft = new LearningConceptDraft(slug, baseVersion, userId, toJson(request), Instant.now());
        drafts.save(draft);
        return response(concept, draft);
    }

    /** 초안이 있으면 초안을 반영해 공개하고, 초안 없는 비공개 개념은 저장된 내용 그대로 다시 공개한다. */
    @Transactional
    public AdminLearningDtos.Concept publish(String userId, String slug) {
        authorizer.requireAdmin(userId);
        List<LearningConcept> all = concepts.lockAll();
        LearningConcept concept = find(all, slug);
        LearningConceptDraft draft = drafts.findById(slug).orElse(null);
        if (draft == null && concept.getStatus() == LearningConceptStatus.PUBLISHED) {
            throw ApiException.conflict("게시할 초안이 없습니다.");
        }
        LearningContent.Node next;
        if (draft != null) {
            if (!draft.getBaseVersion().equals(concept.getContentVersion())) {
                throw ApiException.conflict("게시된 개념이 변경되었습니다. 초안을 다시 작성하세요.");
            }
            AdminLearningDtos.EditRequest request = fromJson(draft.getDraftJson());
            validate(slug, request);
            next = request.toNode(slug);
        } else {
            next = store.node(concept);
        }
        List<LearningConcept> others = all.stream()
                .filter(item -> item.getStatus() == LearningConceptStatus.PUBLISHED && !item.getSlug().equals(slug))
                .toList();
        List<LearningContent.Node> candidate = new ArrayList<>(store.nodes(others));
        candidate.add(next);
        requireValidTree(candidate, "게시하면 개념 트리 규칙이 깨집니다: ");

        if (draft != null) {
            concept.apply(next);
            concept.setContentVersion(newVersion());
            replaceQuiz(slug, next.quiz());
            drafts.delete(draft);
        }
        concept.setStatus(LearningConceptStatus.PUBLISHED);
        concepts.saveAndFlush(concept);
        record(concept, next, "PUBLISHED", userId);
        events.publishEvent(new LearningContentChangedEvent(slug, "PUBLISHED"));
        return response(concept, null);
    }

    /**
     * 공개를 내린다. 내용은 그대로 두므로 다시 publish하면 같은 내용으로 복귀한다.
     * 공개 중인 하위 개념이 있으면 트리가 끊기므로 하위 개념부터 내리게 한다.
     */
    @Transactional
    public AdminLearningDtos.Concept unpublish(String userId, String slug) {
        authorizer.requireAdmin(userId);
        List<LearningConcept> all = concepts.lockAll();
        LearningConcept concept = find(all, slug);
        if (concept.getStatus() != LearningConceptStatus.PUBLISHED) {
            throw ApiException.conflict("이미 비공개 상태인 개념입니다.");
        }
        List<String> publishedChildren = all.stream()
                .filter(item -> item.getStatus() == LearningConceptStatus.PUBLISHED
                        && slug.equals(item.getParentSlug()))
                .map(LearningConcept::getSlug)
                .toList();
        if (!publishedChildren.isEmpty()) {
            throw ApiException.conflict("공개 중인 하위 개념이 있어 공개를 내릴 수 없습니다: "
                    + String.join(", ", publishedChildren));
        }
        List<LearningConcept> remaining = all.stream()
                .filter(item -> item.getStatus() == LearningConceptStatus.PUBLISHED && !item.getSlug().equals(slug))
                .toList();
        requireValidTree(store.nodes(remaining), "공개를 내리면 개념 트리 규칙이 깨집니다: ");

        concept.setStatus(LearningConceptStatus.DRAFT);
        concepts.saveAndFlush(concept);
        record(concept, store.node(concept), "UNPUBLISHED", userId);
        events.publishEvent(new LearningContentChangedEvent(slug, "UNPUBLISHED"));
        return response(concept, drafts.findById(slug).orElse(null));
    }

    @Transactional
    public void discard(String userId, String slug) {
        authorizer.requireAdmin(userId);
        requireConcept(slug);
        drafts.deleteById(slug);
    }

    @Transactional(readOnly = true)
    public AdminLearningDtos.Revisions revisions(String userId, String slug) {
        authorizer.requireAdmin(userId);
        requireConcept(slug);
        List<LearningConceptRevision> found = revisions.findBySlugOrderByCreatedAtDesc(slug);
        Map<String, String> emails = users.findAllById(found.stream().map(LearningConceptRevision::getEditorUserId)
                        .distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, User::getEmail));
        return new AdminLearningDtos.Revisions(slug, found.stream()
                .map(revision -> new AdminLearningDtos.Revision(revision.getId(), revision.getAction(),
                        revision.getContentVersion(),
                        emails.getOrDefault(revision.getEditorUserId(), "(삭제된 계정)"), revision.getCreatedAt()))
                .toList());
    }

    /**
     * 초안 저장 시점에 개념 하나만 보고 확인할 수 있는 규칙. 트리 전체 규칙은 게시 직전에 다시 본다.
     * 시드 검증(LearningTree)과 같은 규칙에 더해 문항 id 충돌·시나리오·출처 URL 형식을 확인한다.
     */
    private void validate(String slug, AdminLearningDtos.EditRequest request) {
        String parent = request.parent() == null || request.parent().isBlank() ? null : request.parent();
        if (parent == null && request.level() != 1) {
            throw ApiException.badRequest("상위 개념이 없으면 레벨 1이어야 합니다.");
        }
        if (parent != null) {
            if (parent.equals(slug)) {
                throw ApiException.badRequest("자기 자신을 상위 개념으로 지정할 수 없습니다.");
            }
            LearningConcept parentConcept = concepts.findById(parent)
                    .orElseThrow(() -> ApiException.badRequest("존재하지 않는 상위 개념입니다: " + parent));
            if (parentConcept.getLevel() != request.level() - 1) {
                throw ApiException.badRequest("상위 개념의 레벨이 맞지 않습니다. 상위 개념(" + parent + ")은 레벨 "
                        + parentConcept.getLevel() + "입니다.");
            }
        }
        for (LearningConcept child : concepts.findAll()) {
            if (slug.equals(child.getParentSlug()) && child.getLevel() != request.level() + 1) {
                throw ApiException.badRequest("하위 개념(" + child.getSlug() + ")의 레벨과 맞지 않습니다.");
            }
        }
        String simulationId = request.simulationId();
        if (simulationId != null && !simulationId.isBlank() && !simulations.contains(simulationId)) {
            throw ApiException.badRequest("존재하지 않는 모의투자 시나리오입니다: " + simulationId);
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
        Set<String> ids = new HashSet<>();
        for (AdminLearningDtos.QuizItemInput item : request.quiz()) {
            if (!ids.add(item.id())) {
                throw ApiException.badRequest("같은 문항 id를 두 번 쓸 수 없습니다: " + item.id());
            }
            if (!DIFFICULTIES.contains(item.difficulty())) {
                throw ApiException.badRequest("문항 난이도는 BASIC, INTERMEDIATE, ADVANCED 중 하나여야 합니다: " + item.id());
            }
            long correct = item.options().stream().filter(AdminLearningDtos.OptionInput::correct).count();
            if (correct != 1) {
                throw ApiException.badRequest("확인 문제의 정답은 정확히 1개여야 합니다: " + item.id());
            }
            quizItems.findById(item.id())
                    .filter(existing -> !existing.getConceptSlug().equals(slug))
                    .ifPresent(existing -> {
                        throw ApiException.badRequest("다른 개념(" + existing.getConceptSlug()
                                + ")에서 이미 쓰는 문항 id입니다: " + item.id());
                    });
        }
    }

    private static void requireValidTree(List<LearningContent.Node> nodes, String prefix) {
        try {
            LearningTree.of("candidate", nodes);
        } catch (IllegalStateException e) {
            throw ApiException.conflict(prefix + e.getMessage());
        }
    }

    /** 문항을 초안대로 맞춘다. 같은 id는 고쳐 쓰고, 빠진 id는 지우고, 새 id는 넣는다. */
    private void replaceQuiz(String slug, List<LearningContent.QuizItem> next) {
        Map<String, LearningQuizItem> existing = new LinkedHashMap<>();
        quizItems.findByConceptSlugOrderBySortOrderAsc(slug).forEach(item -> existing.put(item.getId(), item));
        List<LearningQuizItem> keep = new ArrayList<>();
        for (int i = 0; i < next.size(); i++) {
            LearningContent.QuizItem item = next.get(i);
            LearningQuizItem row = existing.remove(item.id());
            if (row == null) {
                row = new LearningQuizItem(item.id(), slug);
            }
            row.apply(i, item);
            keep.add(row);
        }
        quizItems.deleteAll(existing.values());
        quizItems.saveAll(keep);
    }

    private void record(LearningConcept concept, LearningContent.Node snapshot, String action, String userId) {
        revisions.save(new LearningConceptRevision(UUID.randomUUID().toString(), concept.getSlug(), action,
                concept.getContentVersion(), userId, toJson(AdminLearningDtos.EditRequest.from(snapshot)),
                Instant.now()));
    }

    private static String newVersion() {
        return "admin-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private LearningConcept requireConcept(String slug) {
        return concepts.findById(slug)
                .orElseThrow(() -> ApiException.notFound("학습 개념을 찾을 수 없습니다: " + slug));
    }

    private static LearningConcept find(List<LearningConcept> all, String slug) {
        return all.stream().filter(item -> item.getSlug().equals(slug)).findFirst()
                .orElseThrow(() -> ApiException.notFound("학습 개념을 찾을 수 없습니다: " + slug));
    }

    /** 상위 → 하위, 같은 층은 order 순. 운영 화면에서 트리 모양 그대로 훑어볼 수 있게 한다. */
    private static List<LearningConcept> treeOrder(List<LearningConcept> all) {
        Comparator<LearningConcept> byOrder = Comparator.comparingInt(LearningConcept::getSortOrder)
                .thenComparing(LearningConcept::getSlug);
        Map<String, List<LearningConcept>> children = new LinkedHashMap<>();
        Set<String> slugs = all.stream().map(LearningConcept::getSlug).collect(Collectors.toSet());
        List<LearningConcept> roots = new ArrayList<>();
        for (LearningConcept concept : all) {
            if (concept.getParentSlug() == null || !slugs.contains(concept.getParentSlug())) {
                roots.add(concept);
            } else {
                children.computeIfAbsent(concept.getParentSlug(), key -> new ArrayList<>()).add(concept);
            }
        }
        List<LearningConcept> ordered = new ArrayList<>();
        roots.sort(byOrder);
        for (LearningConcept root : roots) {
            visit(root, children, byOrder, ordered);
        }
        return ordered;
    }

    private static void visit(LearningConcept concept, Map<String, List<LearningConcept>> children,
                              Comparator<LearningConcept> byOrder, List<LearningConcept> into) {
        into.add(concept);
        List<LearningConcept> below = new ArrayList<>(children.getOrDefault(concept.getSlug(), List.of()));
        below.sort(byOrder);
        for (LearningConcept child : below) {
            visit(child, children, byOrder, into);
        }
    }

    private AdminLearningDtos.Concept response(LearningConcept concept, LearningConceptDraft draft) {
        AdminLearningDtos.EditRequest content = draft == null
                ? AdminLearningDtos.EditRequest.from(store.node(concept))
                : fromJson(draft.getDraftJson());
        return new AdminLearningDtos.Concept(concept.getSlug(), content, concept.getStatus().name(),
                concept.getContentVersion(), draft == null ? concept.getUpdatedAt() : draft.getUpdatedAt(),
                draft != null);
    }

    private String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("개념 초안을 직렬화하지 못했습니다.", e);
        }
    }

    private AdminLearningDtos.EditRequest fromJson(String json) {
        try {
            return mapper.readValue(json, AdminLearningDtos.EditRequest.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("저장된 개념 초안을 읽지 못했습니다.", e);
        }
    }
}
