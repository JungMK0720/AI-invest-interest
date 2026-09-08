package com.finmate.analysis;

/**
 * 과제용 투자자 성향 5단계.
 * grade 숫자가 작을수록 공격적이며, 내부 탐색 필터의 기준값으로만 사용한다.
 */
public enum InvestorProfileType {

    AGGRESSIVE(1, "공격투자형", 80),
    ACTIVE(2, "적극투자형", 65),
    NEUTRAL(3, "위험중립형", 45),
    CONSERVATIVE(4, "안정추구형", 25),
    STABLE(5, "안정형", 0);

    private final int grade;
    private final String label;
    private final int minScoreExclusive;

    InvestorProfileType(int grade, String label, int minScoreExclusive) {
        this.grade = grade;
        this.label = label;
        this.minScoreExclusive = minScoreExclusive;
    }

    public int grade() {
        return grade;
    }

    public String label() {
        return label;
    }

    /** 0~100 환산 점수를 성향 등급으로 변환한다. 상위 등급부터 검사하므로 경계값은 상위 등급에 포함되지 않는다. */
    public static InvestorProfileType fromScore(int score) {
        for (InvestorProfileType type : values()) {
            if (score > type.minScoreExclusive) {
                return type;
            }
        }
        return STABLE;
    }

    public static InvestorProfileType fromLabel(String label) {
        for (InvestorProfileType type : values()) {
            if (type.label.equals(label)) {
                return type;
            }
        }
        throw new IllegalArgumentException("알 수 없는 투자 성향: " + label);
    }
}
