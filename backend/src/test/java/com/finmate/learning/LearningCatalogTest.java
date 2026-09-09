package com.finmate.learning;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 콘텐츠 무결성 테스트.
 *
 * <p>학습 콘텐츠는 JSON이라 컴파일러가 검사해 주지 않는다. 그래서 트리 구조·출처·확인 문제의
 * 규칙을 테스트로 고정한다. 콘텐츠를 추가하다 규칙을 깨면 빌드가 실패한다.
 */
class LearningCatalogTest {

    private static LearningCatalog catalog;

    @BeforeAll
    static void setUp() {
        catalog = new LearningCatalog(new ObjectMapper());
        catalog.load();
    }

    @Test
    void 카탈로그가_로드되고_최상위_분류가_존재한다() {
        assertThat(catalog.version()).isNotBlank();
        assertThat(catalog.rootSlugs()).contains("foundations", "instruments", "advanced", "protection");
        assertThat(catalog.nodes()).hasSizeGreaterThan(40);
    }

    @Test
    void 모든_개념은_출처와_한줄정의를_가진다() {
        for (LearningContent.Node node : catalog.nodes()) {
            assertThat(node.sourceUrl()).as(node.slug() + " 출처").isNotBlank();
            assertThat(node.sourceName()).as(node.slug() + " 출처명").isNotBlank();
            assertThat(node.oneLiner()).as(node.slug() + " 한 문장 정의").isNotBlank();
            assertThat(node.explanation()).as(node.slug() + " 설명 문단").isNotEmpty();
        }
    }

    @Test
    void 모든_확인문제는_정답이_하나이고_모든_선택지에_해설이_있다() {
        int total = 0;
        for (LearningContent.Node node : catalog.nodes()) {
            for (LearningContent.QuizItem item : node.quiz()) {
                total++;
                assertThat(item.options()).as(item.id()).hasSizeGreaterThanOrEqualTo(3);
                assertThat(item.options().stream().filter(LearningContent.Option::correct).count())
                        .as(item.id() + " 정답 수").isEqualTo(1);
                assertThat(item.takeaway()).as(item.id() + " 결론").isNotBlank();
                item.options().forEach(option ->
                        assertThat(option.rationale()).as(item.id() + " 선택지 해설").isNotBlank());
            }
        }
        assertThat(total).isGreaterThan(50);
    }

    @Test
    void 하위개념_경로와_자손_수집이_일관된다() {
        List<LearningContent.Node> path = catalog.path("duration");
        assertThat(path).extracting(LearningContent.Node::slug).containsExactly("instruments", "bonds", "duration");

        List<LearningContent.Node> descendants = catalog.descendants("bonds");
        assertThat(descendants).extracting(LearningContent.Node::slug)
                .contains("bonds", "bond-price-yield", "duration", "credit-risk");
    }

    @Test
    void 하위호환_평면목록이_기존_필드를_유지한다() {
        LearningCatalog.Concept concept = catalog.find("risk-return");
        assertThat(concept.title()).isEqualTo("위험과 수익");
        assertThat(concept.explanation()).isNotBlank();
        assertThat(concept.diagramKey()).isEqualTo("RISK");
        assertThat(catalog.list()).hasSameSizeAs(catalog.nodes());
    }
}
