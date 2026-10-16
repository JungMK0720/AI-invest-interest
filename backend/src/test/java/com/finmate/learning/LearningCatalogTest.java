package com.finmate.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 콘텐츠 무결성 테스트.
 *
 * <p>학습 콘텐츠의 시드는 JSON이라 컴파일러가 검사해 주지 않는다. 그래서 트리 구조·출처·확인 문제의
 * 규칙을 테스트로 고정한다. 콘텐츠를 추가하다 규칙을 깨면 빌드가 실패하고, 관리자 게시도 같은 규칙({@link LearningTree})을 거친다.
 */
class LearningCatalogTest {

    private static LearningSeedData seed;
    private static LearningCatalog catalog;

    @BeforeAll
    static void setUp() {
        seed = new LearningSeedData(new ObjectMapper());
        catalog = new LearningCatalog(seed::tree);
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

    @Test
    void 트리_규칙을_어긴_콘텐츠는_만들어지지_않는다() {
        LearningContent.Node riskReturn = seed.tree().node("risk-return");
        LearningContent.QuizItem item = riskReturn.quiz().get(0);

        List<LearningContent.Option> twoCorrect = item.options().stream()
                .map(option -> new LearningContent.Option(option.label(), true, option.rationale())).toList();
        assertThatThrownBy(() -> LearningTree.of("t", replace(withQuiz(riskReturn, item, twoCorrect))))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("정답은 정확히 1개");

        List<LearningContent.Option> noRationale = new ArrayList<>(item.options());
        LearningContent.Option first = noRationale.get(0);
        noRationale.set(0, new LearningContent.Option(first.label(), first.correct(), " "));
        assertThatThrownBy(() -> LearningTree.of("t", replace(withQuiz(riskReturn, item, noRationale))))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("해설");

        LearningContent.Node noSource = copy(riskReturn, riskReturn.parent(), riskReturn.level(), "");
        assertThatThrownBy(() -> LearningTree.of("t", replace(noSource)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("출처");

        LearningContent.Node wrongLevel = copy(riskReturn, riskReturn.parent(), 3, riskReturn.sourceUrl());
        assertThatThrownBy(() -> LearningTree.of("t", replace(wrongLevel)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("레벨");

        LearningContent.Node orphan = copy(riskReturn, "no-such-parent", riskReturn.level(), riskReturn.sourceUrl());
        assertThatThrownBy(() -> LearningTree.of("t", replace(orphan)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("상위 개념을 찾을 수 없습니다");
    }

    @Test
    void 카탈로그는_변경_이벤트를_받으면_다음_조회에서_다시_만든다() {
        AtomicInteger loads = new AtomicInteger();
        LearningCatalog counted = new LearningCatalog(() -> {
            loads.incrementAndGet();
            return seed.tree();
        });
        counted.version();
        counted.nodes();
        assertThat(loads).hasValue(1);

        counted.onContentChanged(new LearningContentChangedEvent("risk-return", "PUBLISHED"));
        counted.version();
        assertThat(loads).hasValue(2);
    }

    /** 시드 트리에서 같은 slug의 노드만 바꿔 끼운다. */
    private static List<LearningContent.Node> replace(LearningContent.Node changed) {
        return seed.tree().nodes().stream()
                .map(node -> node.slug().equals(changed.slug()) ? changed : node)
                .toList();
    }

    private static LearningContent.Node withQuiz(LearningContent.Node node, LearningContent.QuizItem item,
                                                 List<LearningContent.Option> options) {
        List<LearningContent.QuizItem> quiz = node.quiz().stream()
                .map(existing -> existing.id().equals(item.id())
                        ? new LearningContent.QuizItem(item.id(), item.prompt(), item.difficulty(), options, item.takeaway())
                        : existing)
                .toList();
        return new LearningContent.Node(node.slug(), node.parent(), node.level(), node.order(), node.title(),
                node.summary(), node.oneLiner(), node.explanation(), node.keyPoints(), node.checklist(),
                node.misconception(), node.diagramKey(), node.sourceName(), node.sourceUrl(), node.simulationId(), quiz);
    }

    private static LearningContent.Node copy(LearningContent.Node node, String parent, int level, String sourceUrl) {
        return new LearningContent.Node(node.slug(), parent, level, node.order(), node.title(), node.summary(),
                node.oneLiner(), node.explanation(), node.keyPoints(), node.checklist(), node.misconception(),
                node.diagramKey(), node.sourceName(), sourceUrl, node.simulationId(), node.quiz());
    }
}
