"""FinMATE 보조 텍스트 모델 학습 패키지.

표준 라이브러리만 사용한다. 학습 결과는 JSON으로 내보내고 Spring Boot가 기동 시 읽어
같은 규칙으로 추론한다. 사용자 요청마다 Python을 호출하지 않는다.
"""

LABELS = ("AGGRESSIVE", "ACTIVE", "NEUTRAL", "CONSERVATIVE", "STABLE")

# InvestorProfileType.fromScore와 같은 경계(초과 기준)를 쓴다.
LABEL_THRESHOLDS = (("AGGRESSIVE", 80), ("ACTIVE", 65), ("NEUTRAL", 45), ("CONSERVATIVE", 25))

GRADE = {"AGGRESSIVE": 1, "ACTIVE": 2, "NEUTRAL": 3, "CONSERVATIVE": 4, "STABLE": 5}


def label_for(score: float) -> str:
    for label, lower_exclusive in LABEL_THRESHOLDS:
        if score > lower_exclusive:
            return label
    return "STABLE"
