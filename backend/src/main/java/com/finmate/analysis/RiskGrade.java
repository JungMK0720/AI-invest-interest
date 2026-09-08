package com.finmate.analysis;

/**
 * 금융투자상품 위험등급 6단계. 금융투자협회 위험등급 분류 기준을 따른다.
 * number 숫자가 작을수록 위험이 크다 (1등급 = 매우 높은 위험).
 */
public enum RiskGrade {

    GRADE_1(1, "매우 높은 위험"),
    GRADE_2(2, "높은 위험"),
    GRADE_3(3, "다소 높은 위험"),
    GRADE_4(4, "보통 위험"),
    GRADE_5(5, "낮은 위험"),
    GRADE_6(6, "매우 낮은 위험");

    private final int number;
    private final String label;

    RiskGrade(int number, String label) {
        this.number = number;
        this.label = label;
    }

    public int number() {
        return number;
    }

    public String label() {
        return label;
    }

    public static RiskGrade of(int number) {
        for (RiskGrade grade : values()) {
            if (grade.number == number) {
                return grade;
            }
        }
        throw new IllegalArgumentException("위험등급은 1~6 사이여야 합니다: " + number);
    }
}
