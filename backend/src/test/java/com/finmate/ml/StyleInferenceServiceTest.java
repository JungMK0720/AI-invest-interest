package com.finmate.ml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.finmate.common.ApiException;
import org.junit.jupiter.api.Test;

class StyleInferenceServiceTest {

    private final StyleInferenceService service = MlTestFixtures.style();

    @Test
    void 자유_서술이_정반대면_불일치만_표시하고_설문_등급은_그대로_둔다() {
        MlDtos.StyleCheckResponse result = service.check(new MlDtos.StyleCheckRequest(
                "원금이 크게 손실 나도 고수익을 위해 레버리지 투자를 하겠습니다", 5));

        assertThat(result.status()).isEqualTo("ESTIMATED");
        assertThat(result.estimatedProfileType()).isEqualTo("AGGRESSIVE");
        assertThat(result.questionnaireGrade()).isEqualTo(5);
        assertThat(result.mismatch()).isTrue();
        assertThat(result.probabilities()).hasSize(5);
        assertThat(result.notice()).contains("변경되지 않습니다");
    }

    @Test
    void 관심사_태그가_학습_개념과_용어로_이어진다() {
        MlDtos.StyleCheckResponse result = service.check(new MlDtos.StyleCheckRequest(
                "예금처럼 원금이 보장되는 상품만 원해요. 몇 달 뒤 보증금으로 쓸 돈이에요", 5));

        assertThat(result.estimatedProfileType()).isEqualTo("STABLE");
        assertThat(result.mismatch()).isFalse();
        assertThat(result.tags()).extracting(MlDtos.TagSignal::code).contains("PRINCIPAL_FOCUS", "SHORT_TERM_NEED");
        assertThat(result.learningLinks()).extracting(MlDtos.LearningLink::code)
                .contains("PRINCIPAL_FOCUS", "SHORT_TERM_NEED");
        assertThat(result.learningLinks().getFirst().conceptSlugs()).isNotEmpty();
    }

    @Test
    void 정보가_부족하거나_주제와_무관한_문장은_등급을_추측하지_않는다() {
        for (String text : new String[]{"안녕하세요", "주식에 관심이 있습니다", "점심 메뉴 추천해 주세요", "고양이가 너무 귀여워요"}) {
            MlDtos.StyleCheckResponse result = service.check(new MlDtos.StyleCheckRequest(text, 3));
            assertThat(result.status()).as(text).isEqualTo("INSUFFICIENT");
            assertThat(result.estimatedGrade()).as(text).isNull();
            assertThat(result.mismatch()).as(text).isFalse();
        }
    }

    @Test
    void 등급_범위를_벗어나거나_너무_긴_요청은_거절한다() {
        assertThatThrownBy(() -> service.check(new MlDtos.StyleCheckRequest("원금 보전이 중요합니다", 0)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.check(new MlDtos.StyleCheckRequest("가".repeat(1001), 3)))
                .isInstanceOf(ApiException.class);
    }
}
