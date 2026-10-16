package com.finmate.learning;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * 학습 카탈로그. classpath의 learning/concepts.json을 읽어 계층 구조와 인덱스를 만든다.
 *
 * <p>기동 시 한 번만 로드하고 이후에는 불변 컬렉션만 노출한다. 콘텐츠 무결성(부모 존재, slug 중복,
 * 정답 개수, 출처 유무)은 로드 시점에 검증해 잘못된 콘텐츠가 배포되면 기동 자체가 실패하도록 한다.
 * 화면에서 조용히 깨지는 것보다 배포 시점에 드러나는 편이 안전하다.
 */
@Service
public class LearningCatalog {

    private static final String RESOURCE_PATH = "learning/concepts.json";

    private final ObjectMapper objectMapper;

    private String version;
    private List<LearningContent.Node> nodes;
    private Map<String, LearningContent.Node> bySlug;
    private Map<String, List<String>> childrenOf;
    private List<String> roots;

    public LearningCatalog(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void load() {
        LearningContent.Catalog catalog = read();
        List<LearningContent.Node> sorted = new ArrayList<>(catalog.concepts());
        sorted.sort(Comparator.comparingInt(LearningContent.Node::level).thenComparingInt(LearningContent.Node::order));

        Map<String, LearningContent.Node> index = new LinkedHashMap<>();
        for (LearningContent.Node node : sorted) {
            if (index.put(node.slug(), node) != null) {
                throw new IllegalStateException("학습 개념 slug가 중복되었습니다: " + node.slug());
            }
        }

        Map<String, List<String>> children = new LinkedHashMap<>();
        List<String> rootSlugs = new ArrayList<>();
        for (LearningContent.Node node : sorted) {
            validate(node, index);
            if (node.parent() == null) {
                rootSlugs.add(node.slug());
            } else {
                children.computeIfAbsent(node.parent(), key -> new ArrayList<>()).add(node.slug());
            }
        }

        this.version = catalog.version();
        this.nodes = List.copyOf(sorted);
        this.bySlug = Map.copyOf(index);
        this.childrenOf = children.entrySet().stream()
                .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), List.copyOf(entry.getValue())), Map::putAll);
        this.roots = List.copyOf(rootSlugs);
    }

    private LearningContent.Catalog read() {
        try (InputStream in = new ClassPathResource(RESOURCE_PATH).getInputStream()) {
            LearningContent.Catalog catalog = objectMapper.readValue(in, LearningContent.Catalog.class);
            if (catalog.concepts() == null || catalog.concepts().isEmpty()) {
                throw new IllegalStateException("학습 콘텐츠가 비어 있습니다.");
            }
            return catalog;
        } catch (IOException e) {
            throw new IllegalStateException("학습 콘텐츠를 읽지 못했습니다: " + RESOURCE_PATH, e);
        }
    }

    private void validate(LearningContent.Node node, Map<String, LearningContent.Node> index) {
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

    public boolean contains(String slug) {
        return bySlug.containsKey(slug);
    }

    /** 기존 /learning/concepts 응답 형태를 유지하기 위한 평면 목록 (하위 호환). */
    public List<Concept> list() {
        return nodes.stream()
                .map(node -> new Concept(node.slug(), node.title(), node.summary(), flatten(node), node.diagramKey(), node.sourceUrl()))
                .toList();
    }

    public Concept find(String slug) {
        LearningContent.Node node = node(slug);
        return new Concept(node.slug(), node.title(), node.summary(), flatten(node), node.diagramKey(), node.sourceUrl());
    }

    private static String flatten(LearningContent.Node node) {
        return String.join(" ", node.explanation());
    }

    /** v1 응답 호환용 축약 표현. 신규 화면은 LearningDtos.ConceptDetail을 사용한다. */
    public record Concept(String slug, String title, String summary, String explanation, String diagramKey, String sourceUrl) {
    }
}
