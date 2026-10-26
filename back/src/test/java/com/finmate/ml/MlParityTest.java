package com.finmate.ml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Python(ml/train.py)이 저장한 정답 예측과 Java 추론을 한 건씩 비교한다.
 *
 * <p>정규화·n그램·가중치 계산 중 한 곳만 어긋나도 이 테스트가 실패한다.
 * 정확도 같은 통계 기준이 아니라 "같은 입력이면 같은 출력"을 검사하므로 회귀를 바로 잡는다.
 */
class MlParityTest {

    private static JsonNode golden;

    @BeforeAll
    static void setUp() throws IOException {
        try (InputStream stream = MlParityTest.class.getResourceAsStream("/ml/golden-predictions.json")) {
            golden = MlTestFixtures.MAPPER.readTree(stream);
        }
    }

    @Test
    void 모델_버전이_정답_파일과_같다() {
        assertThat(MlTestFixtures.MODELS.style().version()).isEqualTo(golden.get("styleVersion").asText());
        assertThat(MlTestFixtures.MODELS.terms().version()).isEqualTo(golden.get("termVersion").asText());
    }

    @Test
    void 자유_서술_추정이_Python과_문장마다_일치한다() {
        StyleInferenceService service = MlTestFixtures.style();
        assertThat(golden.get("style").size()).isGreaterThanOrEqualTo(100);
        for (JsonNode item : golden.get("style")) {
            StyleInferenceService.Estimate estimate = service.estimate(item.get("text").asText());
            assertThat(estimate.status()).as(item.get("text").asText()).isEqualTo(item.get("status").asText());
            assertThat(estimate.profileType()).as(item.get("text").asText())
                    .isEqualTo(item.get("label").isNull() ? null : item.get("label").asText());
            assertThat(estimate.confidence()).isCloseTo(item.get("confidence").asDouble(), within(1e-7));
            List<String> expectedTags = new ArrayList<>();
            item.get("tags").forEach(tag -> expectedTags.add(tag.asText()));
            assertThat(estimate.tags()).extracting(StyleInferenceService.TagScore::code)
                    .as(item.get("text").asText()).containsExactlyElementsOf(expectedTags);
        }
    }

    @Test
    void 용어_검색_순위와_점수가_Python과_일치한다() {
        TermRetrievalService service = MlTestFixtures.seedTermSearch();
        for (JsonNode item : golden.get("terms")) {
            List<TermRetrievalService.Ranked> ranked = service.rank(item.get("question").asText(), 5);
            JsonNode expected = item.get("top");
            assertThat(ranked).as(item.get("question").asText()).hasSize(expected.size());
            for (int i = 0; i < expected.size(); i++) {
                assertThat(ranked.get(i).term().getSlug()).as(item.get("question").asText())
                        .isEqualTo(expected.get(i).get("slug").asText());
                assertThat(ranked.get(i).score()).isCloseTo(expected.get(i).get("score").asDouble(), within(1e-8));
            }
        }
    }
}
