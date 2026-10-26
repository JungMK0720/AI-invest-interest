package com.finmate.analysis;

import com.finmate.product.ProductCategory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 과제용 내부 탐색 필터.
 *
 * 투자자 성향 등급(1~5)과 상품 위험등급(1~6)은 둘 다 숫자가 작을수록 공격적/고위험이므로
 * "상품 카테고리 위험등급 >= 투자자 성향 등급"인 카테고리를 안내한다.
 * 이 비교식은 실제 투자 권유·적합성·적정성 판단을 대체하지 않는다.
 */
@Component
public class SuitabilityMatcher {

    /** 화면에 노출할 상위 카테고리 수. 권유 가능 범위 전체를 나열하지 않고 성향에 가까운 순으로 자른다. */
    public static final int MAX_RECOMMENDATIONS = 3;

    public boolean isSuitable(InvestorProfileType profile, int riskGrade) {
        return riskGrade >= profile.grade();
    }

    public List<RankedCategory> rank(InvestorProfileType profile, List<ProductCategory> categories) {
        List<ProductCategory> suitable = categories.stream()
                .filter(category -> isSuitable(profile, category.getRiskGrade()))
                .sorted(Comparator.comparingInt(ProductCategory::getRiskGrade))
                .limit(MAX_RECOMMENDATIONS)
                .toList();

        List<RankedCategory> ranked = new ArrayList<>();
        for (int i = 0; i < suitable.size(); i++) {
            ProductCategory category = suitable.get(i);
            ranked.add(new RankedCategory(category, i + 1, describeReason(profile, category)));
        }
        return ranked;
    }

    private String describeReason(InvestorProfileType profile, ProductCategory category) {
        RiskGrade maxRisk = RiskGrade.of(profile.grade());
        return "%s의 내부 탐색 기준은 위험등급 %d등급(%s) 이상입니다. 이 카테고리는 %d등급(%s)으로 안내 범위에 해당합니다."
                .formatted(
                        profile.label(),
                        maxRisk.number(),
                        maxRisk.label(),
                        category.riskGrade().number(),
                        category.riskGrade().label());
    }

    public record RankedCategory(ProductCategory category, int rankNo, String reason) {
    }
}
