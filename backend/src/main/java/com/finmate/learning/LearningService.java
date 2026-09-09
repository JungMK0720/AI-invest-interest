package com.finmate.learning;

import com.finmate.common.ApiException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 학습 도메인의 조회 서비스. 카탈로그의 원본 노드를 화면용 DTO로 옮긴다.
 *
 * <p>컨트롤러가 원본 노드를 직접 노출하면 정답이 포함된 quiz까지 함께 나가므로,
 * 변환은 반드시 이 계층을 거친다.
 */
@Service
public class LearningService {

    private final LearningCatalog catalog;
    private final SimulationCatalog simulations;

    public LearningService(LearningCatalog catalog, SimulationCatalog simulations) {
        this.catalog = catalog;
        this.simulations = simulations;
    }

    public LearningDtos.TreeResponse tree() {
        List<LearningDtos.TreeNode> roots = catalog.rootSlugs().stream().map(this::treeNode).toList();
        return new LearningDtos.TreeResponse(
                catalog.version(), catalog.nodes().size(), roots, LearningDtos.EDUCATION_NOTICE);
    }

    private LearningDtos.TreeNode treeNode(String slug) {
        LearningContent.Node node = catalog.node(slug);
        List<LearningDtos.TreeNode> children = catalog.childSlugs(slug).stream().map(this::treeNode).toList();
        return new LearningDtos.TreeNode(
                node.slug(),
                node.title(),
                node.summary(),
                node.level(),
                !directScenarios(node).isEmpty(),
                node.quiz().size(),
                catalog.descendants(slug).size() - 1,
                children);
    }

    /**
     * 이 개념에 직접 연결된 시나리오.
     *
     * <p>연결은 두 방향에서 온다. 시나리오가 conceptSlugs로 개념을 가리키는 경우와,
     * 개념이 simulationId로 시나리오를 가리키는 경우다. 콘텐츠 작성자가 어느 쪽에 적든
     * 화면에서는 같게 보여야 하므로 여기서 합집합으로 해석한다.
     */
    private List<SimulationDtos.ScenarioSpec> directScenarios(LearningContent.Node node) {
        List<SimulationDtos.ScenarioSpec> found = new ArrayList<>(simulations.forConcept(node.slug()));
        String declared = node.simulationId();
        if (declared != null && simulations.contains(declared)
                && found.stream().noneMatch(spec -> spec.id().equals(declared))) {
            found.add(0, simulations.find(declared));
        }
        return List.copyOf(found);
    }

    public LearningDtos.ConceptDetail detail(String slug) {
        LearningContent.Node node = require(slug);
        List<LearningDtos.Crumb> breadcrumb = catalog.path(slug).stream()
                .map(item -> new LearningDtos.Crumb(item.slug(), item.title(), item.level()))
                .toList();
        List<LearningDtos.TreeNode> children = catalog.childSlugs(slug).stream().map(this::treeNode).toList();
        return new LearningDtos.ConceptDetail(
                node.slug(),
                node.title(),
                node.summary(),
                node.oneLiner(),
                node.level(),
                node.explanation(),
                node.keyPoints(),
                node.checklist(),
                node.misconception(),
                node.diagramKey(),
                node.sourceName(),
                node.sourceUrl(),
                node.simulationId(),
                node.quiz().size(),
                breadcrumb,
                children,
                related(node),
                catalog.version(),
                LearningDtos.EDUCATION_NOTICE);
    }

    /**
     * 관련 개념. 같은 모의투자 시나리오를 공유하는 개념을 우선 연결한다.
     * 시나리오는 "같은 메커니즘을 쓰는 개념"의 묶음이므로 별도 관계 테이블 없이도 의미 있는 연결이 된다.
     */
    private List<String> related(LearningContent.Node node) {
        List<String> slugs = new ArrayList<>();
        for (SimulationDtos.ScenarioSpec spec : simulations.forConcept(node.slug())) {
            for (String candidate : spec.conceptSlugs()) {
                if (!candidate.equals(node.slug()) && catalog.contains(candidate) && !slugs.contains(candidate)) {
                    slugs.add(candidate);
                }
            }
        }
        for (String sibling : siblings(node)) {
            if (!slugs.contains(sibling)) {
                slugs.add(sibling);
            }
        }
        return slugs.size() > 6 ? List.copyOf(slugs.subList(0, 6)) : List.copyOf(slugs);
    }

    private List<String> siblings(LearningContent.Node node) {
        if (node.parent() == null) {
            return List.of();
        }
        return catalog.childSlugs(node.parent()).stream().filter(slug -> !slug.equals(node.slug())).toList();
    }

    /** 개념 화면의 모의투자 탭. 직접 연결된 시나리오가 없으면 상위 개념의 시나리오를 물려준다. */
    public List<SimulationDtos.ScenarioSpec> scenariosFor(String slug) {
        LearningContent.Node node = require(slug);
        List<SimulationDtos.ScenarioSpec> direct = directScenarios(node);
        if (!direct.isEmpty()) {
            return direct;
        }
        List<LearningContent.Node> path = catalog.path(slug);
        for (int i = path.size() - 2; i >= 0; i--) {
            List<SimulationDtos.ScenarioSpec> inherited = directScenarios(path.get(i));
            if (!inherited.isEmpty()) {
                return inherited;
            }
        }
        return List.of();
    }

    public LearningContent.Node require(String slug) {
        if (!catalog.contains(slug)) {
            throw ApiException.notFound("학습 개념을 찾을 수 없습니다: " + slug);
        }
        return catalog.node(slug);
    }
}
