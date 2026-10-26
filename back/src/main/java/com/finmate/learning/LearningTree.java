package com.finmate.learning;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 검증을 마친 개념 트리 한 벌. 만든 뒤에는 바뀌지 않는다.
 *
 * <p>시드 JSON을 읽을 때, DB의 공개 개념으로 카탈로그를 다시 만들 때, 관리자가 게시하기 직전
 * "게시하면 이렇게 될 트리"를 미리 만들어 볼 때 모두 {@link #of}를 거친다. 그래서 부모·레벨·정답·해설·출처
 * 규칙이 시드와 운영 수정에 똑같이 적용된다. 규칙을 어기면 {@link IllegalStateException}을 던진다.
 */
public final class LearningTree {

    private final String version;
    private final List<LearningContent.Node> nodes;
    private final Map<String, LearningContent.Node> bySlug;
    private final Map<String, List<String>> childrenOf;
    private final List<String> roots;

    private LearningTree(String version, List<LearningContent.Node> nodes, Map<String, LearningContent.Node> bySlug,
                         Map<String, List<String>> childrenOf, List<String> roots) {
        this.version = version;
        this.nodes = nodes;
        this.bySlug = bySlug;
        this.childrenOf = childrenOf;
        this.roots = roots;
    }

    public static LearningTree of(String version, List<LearningContent.Node> concepts) {
        List<LearningContent.Node> sorted = new ArrayList<>(concepts);
        sorted.sort(Comparator.comparingInt(LearningContent.Node::level).thenComparingInt(LearningContent.Node::order));

        Map<String, LearningContent.Node> index = new LinkedHashMap<>();
        for (LearningContent.Node node : sorted) {
            if (index.put(node.slug(), node) != null) {
                throw new IllegalStateException("학습 개념 slug가 중복되었습니다: " + node.slug());
            }
        }

        Map<String, List<String>> children = new LinkedHashMap<>();
        List<String> rootSlugs = new ArrayList<>();
        Set<String> quizIds = new HashSet<>();
        for (LearningContent.Node node : sorted) {
            validate(node, index);
            for (LearningContent.QuizItem item : node.quiz()) {
                if (!quizIds.add(item.id())) {
                    throw new IllegalStateException("확인 문제 id가 중복되었습니다: " + item.id());
                }
            }
            if (node.parent() == null) {
                rootSlugs.add(node.slug());
            } else {
                children.computeIfAbsent(node.parent(), key -> new ArrayList<>()).add(node.slug());
            }
        }

        Map<String, List<String>> frozenChildren = new LinkedHashMap<>();
        children.forEach((parent, slugs) -> frozenChildren.put(parent, List.copyOf(slugs)));
        return new LearningTree(version, List.copyOf(sorted), Map.copyOf(index), Map.copyOf(frozenChildren),
                List.copyOf(rootSlugs));
    }

    private static void validate(LearningContent.Node node, Map<String, LearningContent.Node> index) {
        if (node.parent() == null && node.level() != 1) {
            throw new IllegalStateException("상위 개념이 없는 개념은 레벨 1이어야 합니다: " + node.slug());
        }
        if (node.parent() != null && !index.containsKey(node.parent())) {
            throw new IllegalStateException("상위 개념을 찾을 수 없습니다: " + node.slug() + " -> " + node.parent());
        }
        if (node.parent() != null && index.get(node.parent()).level() != node.level() - 1) {
            throw new IllegalStateException("상위 개념의 레벨이 맞지 않습니다: " + node.slug());
        }
        if (node.sourceUrl() == null || node.sourceUrl().isBlank()) {
            throw new IllegalStateException("출처가 없는 학습 개념은 배포할 수 없습니다: " + node.slug());
        }
        for (LearningContent.QuizItem item : node.quiz()) {
            long correct = item.options().stream().filter(LearningContent.Option::correct).count();
            if (correct != 1) {
                throw new IllegalStateException("확인 문제의 정답은 정확히 1개여야 합니다: " + item.id());
            }
            if (item.options().size() < 3) {
                throw new IllegalStateException("확인 문제의 선택지는 3개 이상이어야 합니다: " + item.id());
            }
            boolean missingRationale = item.options().stream()
                    .anyMatch(option -> option.rationale() == null || option.rationale().isBlank());
            if (missingRationale) {
                throw new IllegalStateException("모든 선택지에 해설이 필요합니다: " + item.id());
            }
        }
    }

    /** 콘텐츠 개정 버전. 확인 문제 제출 시 이 값이 다르면 409로 다시 불러오게 한다. */
    public String version() {
        return version;
    }

    public List<LearningContent.Node> nodes() {
        return nodes;
    }

    public List<String> rootSlugs() {
        return roots;
    }

    public List<String> childSlugs(String slug) {
        return childrenOf.getOrDefault(slug, List.of());
    }

    /** 자기 자신 포함 하위 전체. 진도·검색 범위 계산에 쓴다. */
    public List<LearningContent.Node> descendants(String slug) {
        List<LearningContent.Node> collected = new ArrayList<>();
        collect(slug, collected);
        return List.copyOf(collected);
    }

    private void collect(String slug, List<LearningContent.Node> into) {
        LearningContent.Node node = bySlug.get(slug);
        if (node == null) {
            return;
        }
        into.add(node);
        for (String child : childSlugs(slug)) {
            collect(child, into);
        }
    }

    /** 루트부터 대상까지의 경로. 화면의 breadcrumb에 사용한다. */
    public List<LearningContent.Node> path(String slug) {
        List<LearningContent.Node> reversed = new ArrayList<>();
        LearningContent.Node current = bySlug.get(slug);
        while (current != null) {
            reversed.add(0, current);
            current = current.parent() == null ? null : bySlug.get(current.parent());
        }
        return List.copyOf(reversed);
    }

    public LearningContent.Node node(String slug) {
        LearningContent.Node node = bySlug.get(slug);
        if (node == null) {
            throw new IllegalArgumentException("학습 개념을 찾을 수 없습니다: " + slug);
        }
        return node;
    }

    public Optional<LearningContent.Node> lookup(String slug) {
        return Optional.ofNullable(bySlug.get(slug));
    }

    public boolean contains(String slug) {
        return bySlug.containsKey(slug);
    }
}
