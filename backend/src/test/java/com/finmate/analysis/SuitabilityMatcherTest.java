package com.finmate.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.finmate.product.ProductCategory;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SuitabilityMatcherTest {

    private final SuitabilityMatcher matcher = new SuitabilityMatcher();

    private final List<ProductCategory> categories = List.of(
            new ProductCategory("CAT1", "파생결합증권·레버리지 상품", 1, ""),
            new ProductCategory("CAT2", "국내외 주식형 펀드·ETF", 2, ""),
            new ProductCategory("CAT3", "혼합형 펀드", 3, ""),
            new ProductCategory("CAT4", "채권형 펀드", 4, ""),
            new ProductCategory("CAT5", "국공채·우량 회사채", 5, ""),
            new ProductCategory("CAT6", "예금·적금·MMF", 6, ""));

    @Test
    @DisplayName("적합성 원칙에 따라 성향 등급보다 위험한 상품은 제외된다")
    void higherRiskThanProfileIsExcluded() {
        assertThat(matcher.isSuitable(InvestorProfileType.NEUTRAL, 2)).isFalse();
        assertThat(matcher.isSuitable(InvestorProfileType.NEUTRAL, 3)).isTrue();
        assertThat(matcher.isSuitable(InvestorProfileType.NEUTRAL, 6)).isTrue();
    }

    @Test
    @DisplayName("공격투자형은 1등급부터 상위 3개 카테고리를 안내받는다")
    void aggressiveGetsHighestRiskCategories() {
        List<SuitabilityMatcher.RankedCategory> ranked =
                matcher.rank(InvestorProfileType.AGGRESSIVE, categories);

        assertThat(ranked).hasSize(3);
        assertThat(ranked).extracting(r -> r.category().getId())
                .containsExactly("CAT1", "CAT2", "CAT3");
        assertThat(ranked).extracting(SuitabilityMatcher.RankedCategory::rankNo)
                .containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("위험중립형에게는 1~2등급 카테고리가 안내되지 않는다")
    void neutralExcludesHighRiskCategories() {
        List<SuitabilityMatcher.RankedCategory> ranked =
                matcher.rank(InvestorProfileType.NEUTRAL, categories);

        assertThat(ranked).extracting(r -> r.category().getId())
                .containsExactly("CAT3", "CAT4", "CAT5");
    }

    @Test
    @DisplayName("안정형은 권유 가능 범위가 좁아 2개만 안내된다")
    void stableGetsOnlyLowestRiskCategories() {
        List<SuitabilityMatcher.RankedCategory> ranked =
                matcher.rank(InvestorProfileType.STABLE, categories);

        assertThat(ranked).hasSize(2);
        assertThat(ranked).extracting(r -> r.category().getId())
                .containsExactly("CAT5", "CAT6");
    }

    @Test
    @DisplayName("추천 사유에는 권유 가능 위험등급 근거가 포함된다")
    void reasonExplainsSuitabilityRule() {
        SuitabilityMatcher.RankedCategory first =
                matcher.rank(InvestorProfileType.CONSERVATIVE, categories).get(0);

        assertThat(first.reason()).contains("안정추구형");
        assertThat(first.reason()).contains("4등급");
    }
}
