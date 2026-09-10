# FinMATE 플로우차트·와이어프레임 명세

> 시각 산출물: [FinMATE-플로우차트-와이어프레임.pptx](FinMATE-플로우차트-와이어프레임.pptx)

## 사용자 흐름

```text
로그인
  → 진단 가능 여부 확인
  → 질문 응답
  → 규칙 기반 성향 판정
  → AI 근거 설명 또는 템플릿 폴백
  → 카테고리·상품 탐색

개념 트리
  → 상품 학습 카드
  → 개념별 퀴즈
  → 메커니즘 시뮬레이션
  → 가상 포트폴리오 분석·비교
```

## 화면 명세

| 화면 | 화면 목적 | 핵심 구성 요소 | 연결 API |
|---|---|---|---|
| S1 로그인 | 사용자를 식별하고 보호 API 접근 | 이메일, 비밀번호, 로그인, 회원가입 | `POST /auth/login`, `POST /auth/signup` |
| S2 진단 시작 | 진단 가능 여부와 학습 진입 분리 | 다음 진단 가능일, 진단 시작, 상품 학습 | `GET /assessments/availability` |
| S3 진단 진행 | 질문에 답하고 제출 | 진행률, 선택지, 다음, 미응답 상태 | `GET /questions`, `POST /assessments` |
| S4 분석 결과 | 판정과 근거를 확인 | 성향, 점수, 근거, TEMPLATE 표시, 탐색 버튼 | `GET /assessments/{id}`, `GET /assessments/{id}/recommendations` |
| S5 상품 탐색 | 카테고리별 상품을 비교 | 카테고리 필터, 상품 목록, 위험등급, 기간 | `GET /product-categories`, `GET /products` |
| S6 상품 상세 | 상품 구조와 확인 문서 학습 | 위험 변수, 설명서 체크리스트, 면책 | `GET /products/{id}` |
| S7 개념 학습 | 자산군·메커니즘을 계층으로 학습 | 개념 트리, 그림, 오해, 출처 | `GET /learning/tree`, `GET /learning/concepts/{slug}` |
| S8 개념별 퀴즈 | 개념 이해를 검증 | 문제, 선택지, 오답 해설, 다음 학습 | `GET /learning/concepts/{slug}/quiz`, `POST /learning/concepts/{slug}/quiz/grade` |
| S9 포트폴리오 연습 | 가상 상품과 충격을 실험 | 예산, 자산 바구니, 충격, 손익, 의미, 비교 | `POST /practice/analyze`, `POST /practice/compare` |

## 상태별 와이어프레임 규칙

| 상태 | 화면 처리 |
|---|---|
| 정상 | 입력값·처리 결과·다음 행동을 한 화면에서 보여 준다. |
| 미응답 | 제출 버튼을 비활성화하고 빠진 문항을 알려 준다. |
| AI 실패 | 진단을 실패시키지 않고 `TEMPLATE` 근거로 완료한다. |
| 데이터 준비 실패 | 답변을 보존한 상태로 재시도한다. |
| 빈 상품 목록 | 이유와 다른 학습 경로를 보여 준다. |
| 시뮬레이션 결과 | 가정·seed·모델 버전·한계를 접지 않고 보여 준다. |

## 발표 연결

- 1~3번 슬라이드: 전체 흐름과 진단 예외
- 4~6번 슬라이드: S1~S6 와이어프레임
- 7번 슬라이드: S7~S8 학습·퀴즈
- 8번 슬라이드: S9 포트폴리오 연습

