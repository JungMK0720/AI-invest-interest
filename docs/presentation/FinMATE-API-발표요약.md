# FinMATE API 발표 요약

> 상세 계약: [FinMATE-발표용-API.yml](FinMATE-발표용-API.yml)  
> 현재 구현 기준: 25 paths / 26 operations

## API 흐름

```text
로그인
  ↓
진단 질문 조회 → 답변 제출
  ↓
규칙 엔진이 성향 판정 → AI가 근거 설명
  ↓
결과·상품 탐색

개념 트리 → 개념별 퀴즈 → 메커니즘 시뮬레이션
                                      ↓
                         교육용 포트폴리오 분석·비교
```

## 발표용 엔드포인트 목록

| 영역 | Method | Path | 인증 | 설명 |
|---|---|---|---|---|
| 인증 | POST | `/auth/signup` | - | 회원가입 |
| 인증 | POST | `/auth/login` | - | access token 발급 |
| 진단 | GET | `/questions` | - | 활성 질문·선택지 조회 |
| 진단 | GET | `/assessments/availability` | Bearer | 월 1회 정책 상태 조회 |
| 진단 | POST | `/assessments` | Bearer | 답변 제출·성향 분석·결과 생성 |
| 진단 | GET | `/assessments/{id}` | Bearer | 본인 결과 조회 |
| 진단 | GET | `/assessments/{id}/recommendations` | Bearer | 탐색 카테고리 조회 |
| 진단 | GET | `/assessments` | Bearer | 진단 이력 조회 |
| 상품 | GET | `/product-categories` | - | 자산군·위험등급 조회 |
| 상품 | GET | `/products?categoryId=...` | - | 예시 상품 목록 |
| 상품 | GET | `/products/{id}` | - | 상품 상세·면책 문구 |
| 학습 | GET | `/learning/tree` | - | 3단계 개념 트리 |
| 학습 | GET | `/learning/concepts/{slug}` | - | 정의·그림·오해·출처 |
| 학습 | GET | `/learning/concepts/{slug}/quiz` | - | 개념 전용 문제 조회 |
| 학습 | POST | `/learning/concepts/{slug}/quiz/grade` | - | 서버 채점·오답 해설 |
| 학습 | POST | `/learning/concepts/{slug}/quiz/attempts` | - | 콘텐츠 버전·choice ID 제출 |
| 실험 | GET | `/learning/simulations` | - | 금리·수수료·옵션·선물 시나리오 |
| 실험 | POST | `/learning/simulations/{id}/run` | - | seed 기반 메커니즘 계산 |
| 포트폴리오 | GET | `/practice/products` | - | 가상 학습 상품 목록 |
| 포트폴리오 | GET | `/practice/scenarios` | - | 가격·금리·환율 충격 목록 |
| 포트폴리오 | GET | `/practice/methodology` | - | 공식·가정·제외 항목 |
| 포트폴리오 | POST | `/practice/analyze` | - | 가상 포트폴리오 분석 |
| 포트폴리오 | POST | `/practice/compare` | - | 변경 전후 결과 비교 |

## 대표 요청·응답

### 진단 제출

```json
POST /assessments
Authorization: Bearer {accessToken}

{
  "answers": [
    { "questionId": "q-risk-01", "choiceId": "q-risk-01-c3" }
  ]
}
```

```json
201 Created
{
  "id": "assessment-001",
  "status": "COMPLETED",
  "profile": {
    "profileLabel": "위험중립형",
    "profileGrade": 3,
    "totalScore": 51,
    "reasoningSource": "TEMPLATE"
  }
}
```

### 교육용 포트폴리오 분석

```json
POST /practice/analyze

{
  "portfolio": {
    "budget": 10000000,
    "holdings": [
      {
        "rowId": "row-1",
        "productId": "long-bond",
        "label": "장기 채권 예시",
        "amount": 4000000,
        "annualFeePercent": 0.5,
        "duration": 8,
        "leverage": 1,
        "strike": 100,
        "premium": 3,
        "borrowingRatePercent": 4
      }
    ]
  },
  "shocks": {
    "equityPercent": -10,
    "ratePoints": 1,
    "creditSpreadPoints": 0.5,
    "goldPercent": 5,
    "realEstatePercent": -3,
    "fxPercent": 3,
    "days": 30
  }
}
```

응답에는 `profitLoss`, `returnPercent`, 자산별 `positions`, `lessons`, `limitations`가 포함된다. 결과는 실데이터 예측이나 매매 주문이 아니라 입력한 충격을 계산한 교육용 결과다.

## API 설계 포인트

- 점수·등급은 규칙 엔진이 결정하고 AI는 설명만 생성한다.
- AI 설명 생성 실패 시 `reasoningSource=TEMPLATE`으로 진단을 완료한다.
- 퀴즈 조회 응답에는 정답을 포함하지 않고 서버에서 채점한다.
- 시뮬레이션은 입력·seed·모델 버전을 반환해 같은 결과를 재현한다.
- 포트폴리오 연습기는 가상의 상품과 가정값만 사용하며 실제 주문·추천 API가 아니다.
- 실데이터 수집·과거 자료·관리자 검수는 P1 확장으로 별도 API를 설계한다.

