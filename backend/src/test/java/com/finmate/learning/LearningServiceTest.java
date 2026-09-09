package com.finmate.learning;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LearningServiceTest {

    private static LearningService service;
    private static SimulationCatalog simulations;

    @BeforeAll
    static void setUp() {
        LearningCatalog catalog = new LearningCatalog(new ObjectMapper());
        catalog.load();
        simulations = new SimulationCatalog();
        service = new LearningService(catalog, simulations);
    }

    @Test
    void 트리는_네_개의_최상위_분류와_전체_개념수를_돌려준다() {
        LearningDtos.TreeResponse tree = service.tree();
        assertThat(tree.roots()).hasSize(4);
        assertThat(tree.conceptCount()).isEqualTo(countNodes(tree.roots()));
        assertThat(tree.notice()).isEqualTo(LearningDtos.EDUCATION_NOTICE);
    }

    @Test
    void 개념이_직접_선언한_시나리오도_연결된다() {
        // structured-products는 어떤 시나리오의 conceptSlugs에도 없고 simulationId로만 연결된다.
        assertThat(simulations.forConcept("structured-products")).isEmpty();
        assertThat(service.scenariosFor("structured-products"))
                .extracting(SimulationDtos.ScenarioSpec::id).contains("option-payoff");

        // 트리의 hasSimulation 플래그도 같은 기준을 써야 한다.
        assertThat(findNode(service.tree().roots(), "structured-products").hasSimulation()).isTrue();
    }

    @Test
    void 연결이_없는_개념은_상위_개념의_시나리오를_물려받는다() {
        // disclosure는 자체 시나리오가 없고 상위(protection)에도 없으므로 비어 있어야 한다.
        assertThat(service.scenariosFor("disclosure")).isEmpty();
        // credit-risk는 상위 bonds의 시나리오를 통해서도 채권 시나리오에 도달한다.
        assertThat(service.scenariosFor("credit-risk"))
                .extracting(SimulationDtos.ScenarioSpec::id).contains("bond-rate-shock");
    }

    @Test
    void 상세에는_breadcrumb과_하위개념과_관련개념이_들어간다() {
        LearningDtos.ConceptDetail detail = service.detail("bonds");
        assertThat(detail.breadcrumb()).extracting(LearningDtos.Crumb::slug).containsExactly("instruments", "bonds");
        assertThat(detail.children()).extracting(LearningDtos.TreeNode::slug)
                .containsExactly("bond-price-yield", "duration", "credit-risk");
        assertThat(detail.relatedSlugs()).isNotEmpty().doesNotContain("bonds");
        assertThat(detail.simulationId()).isEqualTo("bond-rate-shock");
    }

    private static int countNodes(List<LearningDtos.TreeNode> nodes) {
        return nodes.stream().mapToInt(node -> 1 + countNodes(node.children())).sum();
    }

    private static LearningDtos.TreeNode findNode(List<LearningDtos.TreeNode> nodes, String slug) {
        for (LearningDtos.TreeNode node : nodes) {
            if (node.slug().equals(slug)) {
                return node;
            }
            LearningDtos.TreeNode found = findNode(node.children(), slug);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
