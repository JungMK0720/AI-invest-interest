# 04. API 명세 설계 (OAS 3.0 기준)

> 기준 문서: [02-user-flow.md](02-user-flow.md), [03-data-model.md](03-data-model.md)
> 제출용 명세: [FinMATE-API.yml](../deliverables/FinMATE-API.yml)
> 상태: 1차 Agile(P0) 계약 확정 · 2차 고도화 API는 §10 (OAS 2.0.0, 50 paths)
> 발표용 최신 명세: [FinMATE-발표용-API.yml](../presentation/FinMATE-발표용-API.yml) (25 paths / 26 operations)

## 1. API 원칙

- 사용자 식별자는 인증 컨텍스트에서 얻고, 진단 제출 본문으로 `userId`를 받지 않는다.
- 성향 점수·등급·카테고리 필터는 규칙 엔진이 결정한다. LLM은 설명 문장만 생성한다.
- 위험등급은 과제용 예시 데이터의 내부 탐색 기준이다. API가 실제 투자 권유·적합성·적정성 판단을 제공하는 것으로 표현하지 않는다.
- 선택지의 `score`와 질문의 `weight`는 클라이언트에 내려주지 않는다.
- 상품 응답에는 화면에서 항상 노출할 `disclaimer`를 포함한다.
- MVP는 동기 처리다. 정상적인 `POST /assessments`는 `201 + COMPLETED`를 반환한다.

## 2. 태그(Tag) 구성

| 태그 | 설명 |
|---|---|
| auth | 회원가입/로그인 |
| questions | 활성 진단 질문 조회 |
| assessments | 진단 생성·결과·탐색 카테고리 조회 |
| products | 카테고리·예시 상품 조회 |

## 3. 1차 API 목록

| 태그 | Method | Path | 인증 | 설명 |
|---|---|---|---|---|
| auth | POST | `/auth/signup` | 불필요 | 회원가입 |
| auth | POST | `/auth/login` | 불필요 | 로그인 및 데모용 access token 발급 |
| questions | GET | `/questions` | 불필요 | 활성 질문·선택지 목록 조회 |
| assessments | POST | `/assessments` | 필요 | 답변 제출 → 규칙 판정·설명 생성·진단 저장 |
| assessments | GET | `/assessments/{assessmentId}` | 필요 | 본인 진단 상태·결과 조회 |
| assessments | GET | `/assessments/{assessmentId}/recommendations` | 필요 | 본인 진단의 탐색 카테고리·이유 조회 |
| products | GET | `/product-categories` | 불필요 | 전체 예시 카테고리 목록 조회 |
| products | GET | `/products` | 불필요 | 예시 상품 목록 조회 (`categoryId` 선택) |
| products | GET | `/products/{productId}` | 불필요 | 예시 상품 상세 조회 |

## 4. 핵심 계약

### POST `/assessments`

요청에는 활성 질문별 `questionId`와 선택한 `choiceId`를 정확히 한 번씩 포함한다. 서버는 문항-선택지 소속을 검증하고, 가중 평균 점수와 5단계 성향을 결정한 뒤 카테고리 필터를 적용한다.

LLM 설명 생성이 실패하거나 API 키가 없으면 `reasoningSource=TEMPLATE`로 템플릿 설명을 저장하고도 `201`을 반환한다. 문항 또는 카테고리 데이터가 없어 분석을 진행할 수 없는 경우에만 `503 ANALYSIS_UNAVAILABLE`을 반환한다.

```yaml
requestBody:
  required: true
  content:
    application/json:
      schema:
        $ref: '#/components/schemas/SubmitAssessmentRequest'
responses:
  '201':
    description: 진단 생성 및 분석 완료
    content:
      application/json:
        schema:
          $ref: '#/components/schemas/AssessmentResult'
  '400':
    description: 미응답 문항, 중복 응답, 존재하지 않는 선택지, 문항-선택지 불일치
  '401':
    description: 로그인 필요 또는 토큰 무효
  '503':
    description: 진단 문항 또는 상품 카테고리가 준비되지 않음
```

### GET `/assessments/{assessmentId}`

본인 소유 여부를 확인한다. 다른 사용자의 진단이면 `403`, 존재하지 않으면 `404`를 반환한다. MVP 정상 흐름에서는 `status=COMPLETED`이고 `profile`이 채워진다.

### GET `/assessments/{assessmentId}/recommendations`

인증된 사용자의 진단에 귀속된 카테고리를 `rank` 오름차순으로 반환한다. `rank`는 화면 노출 순위이며 투자 수익률 순위가 아니다.

### GET `/products?categoryId={categoryId}`

카테고리를 지정하면 해당 카테고리의 사전 등록 예시 상품을 반환한다. 결과가 없으면 빈 배열을 반환하며 화면은 빈 상태를 표시한다.

## 5. 주요 스키마

| 스키마 | 핵심 필드 |
|---|---|
| `AssessmentResult` | `id`, `status`, `profile`, `createdAt`, `completedAt` |
| `InvestmentProfile` | `profileType`, `profileLabel`, `profileGrade`, `totalScore`, `reasoning`, `reasoningSource` |
| `CategoryRecommendation` | `categoryId`, `categoryName`, `riskGrade`, `riskGradeLabel`, `rank`, `reason` |
| `Product` | `id`, `categoryId`, `categoryName`, `name`, `riskGrade`, `riskGradeLabel`, `expectedPeriod`, `description`, `disclaimer` |
| `Question` | `id`, `content`, `orderNo`, `choices[]` |
| `Choice` | `id`, `label`, `orderNo` |
| `Error` | `code`, `message` |

비밀번호, 비밀번호 해시, 선택지 점수, 질문 배점, 프롬프트 원문은 어떠한 외부 응답에도 포함하지 않는다.

## 6. 에러 코드 및 사용자 처리

| HTTP | 코드 | 사용 상황 | 화면 처리 |
|---|---|---|---|
| 400 | `INVALID_REQUEST` | 형식 오류, 답변 누락/중복, 문항-선택지 불일치 | 해당 입력 안내 |
| 401 | `UNAUTHORIZED` | 로그인 필요 또는 인증 실패 | 로그인 화면으로 이동 |
| 403 | `FORBIDDEN` | 다른 사용자의 진단 접근 | 접근 불가 안내 |
| 404 | `NOT_FOUND` | 진단·상품 없음 | 없음 상태 안내 |
| 503 | `ANALYSIS_UNAVAILABLE` | 문항·카테고리 데이터 준비 실패 | 답변 유지 후 재시도 |
| 500 | `INTERNAL_ERROR` | 처리되지 않은 서버 오류 | 내부 상세를 숨기고 재시도 안내 |

LLM 설명 실패는 별도 오류가 아니라 템플릿 폴백으로 처리한다.

## 7. 명세 검증 순서

1. [FinMATE-API.yml](../deliverables/FinMATE-API.yml)을 YAML/OAS 파서로 검증한다.
2. `02-user-flow.md`의 S1~S6 동작과 엔드포인트를 1:1로 대조한다.
3. 성공·400·401·403·404·503·500 경로를 최소 한 번씩 실행한다.
4. 다른 사용자 `assessmentId` 접근과 선택지 점수 미노출을 확인한다.
5. 실제 서비스화 전에는 인증·상품 위험정보·문구에 대한 별도 준법 검토를 수행한다.

## 8. 2차 Agile API

- `GET /products?ids=...` 상품 비교
- `/portfolios` 포트폴리오 생성·조회·상품 담기·삭제
- `/admin/questions`, `/admin/products` CRUD
- 토큰 만료·로그아웃·재발급
- 진단 질문/상품 버전 및 결과 스냅샷

## 9. 현재 발표용 고도화 API 구성

발표에서는 기존 진단 API만 보여 주지 않고, “진단 결과 → 개념 학습 → 개념별 검증 → 가정 실험”의 흐름을 API 단위로 설명한다.

| 영역 | 엔드포인트 수 | 발표에서 설명할 핵심 |
|---|---:|---|
| 인증·진단·상품 탐색 | 10 paths | 사용자 식별, 월 1회 정책 상태, 규칙 엔진 판정, 상품 탐색 |
| 개념 학습·퀴즈 | 6 paths | 3단계 개념 트리, 개념별 문항, 서버 채점, choice ID 제출 |
| 메커니즘 시뮬레이션 | 4 paths | 서버 계산, 입력 명세, seed 재현, 가정·한계 반환 |
| 교육용 포트폴리오 | 5 paths | 가상 상품·충격·분석·전후 비교, 실제 주문과 분리 |

실데이터 수집·과거 자료·관리자 검수 API는 발표용 명세에 포함하지 않고 P1 확장으로 설명한다. 구현 시에는 출처 URL, 기준 시각, 데이터 hash, 콘텐츠 버전을 함께 저장해 현재 데이터와 과거 스냅샷을 덮어쓰지 않도록 한다.


## 10. 2차 고도화 API (2026-10)

제출용 명세 `deliverables/FinMATE-API.yml`을 2.0.0으로 올려 아래 경로를 추가했다. 인증 표기: 공개 = 토큰 불필요, 선택 = 토큰이 있으면 기록을 저장(잘못된 토큰은 401), 필요 = 로그인, 관리자 = ADMIN 역할.

| 태그 | Method | Path | 인증 | 설명 |
|---|---|---|---|---|
| auth | GET | `/auth/me` | 필요 | 세션 확인·역할 |
| auth | POST | `/auth/logout` | 필요 | 세션 폐기 |
| learning | POST | `/learning/concepts/{slug}/explain` | 공개 | 승인 콘텐츠 기반 AI 설명(템플릿 폴백) |
| learning | POST | `/learning/concepts/{slug}/quiz/attempts` | 선택 | choice ID 제출·채점, 로그인 시 이력·복습 저장 |
| glossary | GET | `/glossary/categories` | 공개 | 14개 분류와 공개 용어 수 |
| glossary | GET | `/glossary/terms?q=&category=&level=` | 공개 | 공개 용어 목록·글자 검색 |
| glossary | GET | `/glossary/terms/{slug}` | 공개 | 용어 상세 |
| glossary | GET | `/glossary/quiz?category=&count=&seed=` | 공개 | 용어 4지선다(정답 미포함) |
| glossary | POST | `/glossary/quiz/grade` | 선택 | 채점, 로그인 시 용어 복습 저장 |
| glossary | GET/PUT/DELETE | `/glossary/bookmarks[/{slug}]` | 필요 | 저장한 용어 |
| ml | GET | `/ml/models` | 공개 | 모델 카드(데이터·지표·기준선·한계) |
| ml | POST | `/ml/style-check` | 공개 | 자유 서술 성향 신호·관심사 태그·학습 링크 |
| ml | POST | `/ml/term-search` | 공개 | 질문형 용어 검색(DB 공개 용어 인덱스) |
| me | GET | `/me/learning-path` | 필요 | 개인화 학습 경로(사유 포함) |
| me | GET | `/me/quiz-attempts[/{id}]` | 필요 | 시도 이력·상세 |
| me | GET | `/me/wrong-answers` | 필요 | 오답노트 |
| me | GET | `/me/review-schedule` | 필요 | 복습 일정 |
| admin | GET | `/admin/overview` | 관리자 | 운영 현황 |
| admin | GET/POST | `/admin/glossary/terms` | 관리자 | 전체 용어 / 새 용어(비공개) |
| admin | GET/PUT | `/admin/glossary/terms/{slug}` | 관리자 | 편집용 조회 / 초안 저장 |
| admin | POST | `/admin/glossary/terms/{slug}/publish` | 관리자 | 검증 게시(이력·인덱스 재구성) |
| admin | POST | `/admin/glossary/terms/{slug}/unpublish` | 관리자 | 공개 내리기 |
| admin | DELETE | `/admin/glossary/terms/{slug}/draft` | 관리자 | 초안 버리기 |
| admin | GET | `/admin/glossary/terms/{slug}/revisions` | 관리자 | 게시 이력 |
| admin | GET | `/admin/learning/concepts` | 관리자 | 전체 개념(트리 순서, 공개·비공개·초안) |
| admin | GET/PUT | `/admin/learning/concepts/{slug}` | 관리자 | 편집용 조회 / 개념·확인 문제 초안 저장 |
| admin | POST | `/admin/learning/concepts/{slug}/publish` | 관리자 | 트리 재검증 후 게시(학습 콘텐츠 버전 변경) |
| admin | POST | `/admin/learning/concepts/{slug}/unpublish` | 관리자 | 공개 내리기(공개 하위 개념이 있으면 409) |
| admin | DELETE | `/admin/learning/concepts/{slug}/draft` | 관리자 | 초안 버리기 |
| admin | GET | `/admin/learning/concepts/{slug}/revisions` | 관리자 | 게시 이력 |

변경된 기존 계약

- `POST /assessments` 요청에 선택 필드 `narrative`(1,000자 이하) 추가. 응답 `evidence.narrative`에 추정 상태·성향·확신도·불일치·태그·모델 버전(원문 없음), `evidence.reviewFlags`에 `NARRATIVE_MISMATCH` 추가.
- `LoginResponse`에 `expiresAt`, `UserResponse`에 `role` 추가.
- 오류 코드는 기존 `Error` 스키마를 그대로 쓴다. 관리자 API의 권한 부족은 403, 콘텐츠 버전 충돌은 409다.
