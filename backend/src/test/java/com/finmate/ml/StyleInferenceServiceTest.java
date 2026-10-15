package com.finmate.ml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.finmate.common.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class StyleInferenceServiceTest {

    private static StyleInferenceService service;

    @BeforeAll
    static void setUp() {
        MlModelCatalog catalog = new MlModelCatalog(new ObjectMapper());
        catalog.load();
        service = new StyleInferenceService(catalog);
    }

    @Test
    void 자유_서술이_정반대면_불일치만_표시하고_설문_등급은_그대로_둔다() {
        MlDtos.StyleCheckResponse result = service.check(new MlDtos.StyleCheckRequest(
                "원금이 크게 손실 나도 고수익을 위해 레버리지 투자를 하겠습니다", 5));

        assertThat(result.status()).isEqualTo("ESTIMATED");
        assertThat(result.estimatedProfileType()).isEqualTo("AGGRESSIVE");
        assertThat(result.questionnaireGrade()).isEqualTo(5);
        assertThat(result.mismatch()).isTrue();
        assertThat(result.notice()).contains("변경되지 않습니다");
    }

    @Test
    void 정보가_부족하거나_모호한_문장은_등급을_추측하지_않는다() {
        MlDtos.StyleCheckResponse shortText = service.check(new MlDtos.StyleCheckRequest("안녕하세요", 3));
        MlDtos.StyleCheckResponse generic = service.check(new MlDtos.StyleCheckRequest("주식에 관심이 있습니다", 3));

        assertThat(shortText.status()).isEqualTo("INSUFFICIENT");
        assertThat(shortText.estimatedGrade()).isNull();
        assertThat(shortText.mismatch()).isFalse();
        assertThat(generic.status()).isEqualTo("INSUFFICIENT");
        assertThat(generic.mismatch()).isFalse();
    }

    @Test
    void 등급_범위를_벗어난_요청은_거절한다() {
        assertThatThrownBy(() -> service.check(new MlDtos.StyleCheckRequest("원금 보전이 중요합니다", 0)))
                .isInstanceOf(ApiException.class);
    }
}
