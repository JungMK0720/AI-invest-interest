package com.finmate.product;

public final class ProductDtos {

    /** 자본시장법상 투자 자문이 아님을 화면에 항상 노출하기 위한 고지 문구 (FR-07 인수 조건). */
    public static final String DISCLAIMER =
            "본 정보는 투자 참고용이며 투자 자문이나 투자 권유가 아닙니다. 투자 판단의 최종 책임은 투자자 본인에게 있으며, "
                    + "금융투자상품은 원금 손실이 발생할 수 있습니다.";

    private ProductDtos() {
    }

    public record CategoryResponse(String id, String name, int riskGrade, String riskGradeLabel, String description) {
    }

    public record ProductResponse(
            String id,
            String categoryId,
            String categoryName,
            String name,
            int riskGrade,
            String riskGradeLabel,
            String expectedPeriod,
            String description,
            String disclaimer) {
    }

    public static CategoryResponse from(ProductCategory category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getRiskGrade(),
                category.riskGrade().label(),
                category.getDescription());
    }

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getName(),
                product.getRiskGrade(),
                product.riskGrade().label(),
                product.getExpectedPeriod(),
                product.getDescription(),
                DISCLAIMER);
    }
}
