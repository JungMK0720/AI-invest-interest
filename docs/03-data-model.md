# 03. 데이터 모델 설계 (ERD)

> 기준 문서: [01-requirements.md](01-requirements.md), [02-user-flow.md](02-user-flow.md)
> 상태: 1차 Agile(P0) 확정 — 제출용 [FinMATE-DB.dbml](../deliverables/FinMATE-DB.dbml)과 동일한 모델을 기준으로 한다.
> 원칙: UI에서 쓰는 필드를 모두 포함하고, PK/FK·관계·필수값을 명시한다. 2차 엔터티는 MVP ERD에 섞지 않는다.

## 1. 1차 엔터티 목록

| 엔터티 | 설명 |
|---|---|
| users | 일반 사용자 계정 |
| questions | 진단 질문 |
| question_choices | 질문별 선택지와 AI 분석용 의미 키 |
| assessments | 사용자의 1회 진단 및 처리 상태 |
| answers | 진단 내 문항별 답변 |
| investment_profiles | AI가 생성한 투자 성향과 판단 근거 |
| product_categories | 추천 대상 금융상품 카테고리 |
| products | 카테고리에 속한 사전 등록 예시 상품 |
| category_recommendations | 진단별 탐색 카테고리·순위·이유 |

## 2. 관계 요약

- users 1:N assessments
- assessments 1:N answers, assessments 1:1 investment_profiles
- questions 1:N question_choices, questions 1:N answers
- assessments N:M product_categories (`category_recommendations`로 해소)
- product_categories 1:N products

> 위험등급과 성향등급의 숫자 비교는 과제용 내부 탐색 필터다. 실제 상품의 공식 위험정보나 금융기관의 적합성·적정성 판단을 대체하지 않는다.

## 3. ERD (제출용 DBML 원천)

```dbml
Table users {
  id varchar [pk]
  email varchar [not null, unique]
  password_hash varchar [not null]
  name varchar
  created_at timestamp [not null]
}

Table questions {
  id varchar [pk]
  content varchar [not null]
  order_no integer [not null, unique]
  weight integer [not null, note: '활성 문항의 합이 100']
  is_active boolean [not null, default: true]
}

Table question_choices {
  id varchar [pk]
  question_id varchar [ref: > questions.id, not null]
  label varchar [not null]
  analysis_key varchar [not null, note: 'AI 입력에 사용할 안정적 의미 키']
  score integer [not null, note: '0~100 원점수. 외부 응답에는 포함하지 않음']
  order_no integer [not null]

  indexes {
    (question_id, order_no) [unique]
  }
}

Table assessments {
  id varchar [pk]
  user_id varchar [ref: > users.id, not null]
  status varchar [not null, note: 'PROCESSING | COMPLETED | FAILED. MVP 정상 흐름은 COMPLETED']
  failure_reason varchar [note: '사용자에게 직접 노출하지 않는 내부 실패 사유']
  created_at timestamp [not null]
  completed_at timestamp
}

Table answers {
  id varchar [pk]
  assessment_id varchar [ref: > assessments.id, not null]
  question_id varchar [ref: > questions.id, not null]
  choice_id varchar [ref: > question_choices.id, not null]

  indexes {
    (assessment_id, question_id) [unique]
  }
}

Table investment_profiles {
  id varchar [pk]
  assessment_id varchar [ref: - assessments.id, not null, unique]
  profile_type varchar [not null, note: 'AGGRESSIVE | ACTIVE | NEUTRAL | CONSERVATIVE | STABLE']
  total_score integer [not null, note: '0~100 가중 평균 점수']
  reasoning varchar [not null, note: '선택 답변에 근거한 AI 판단 설명']
  reasoning_source varchar [not null, note: 'LLM | TEMPLATE']
  created_at timestamp [not null]
}

Table product_categories {
  id varchar [pk]
  name varchar [not null, unique]
  risk_grade integer [not null, note: '내부 탐색용 1~6 등급. 숫자가 작을수록 고위험']
  description varchar [not null]
}

Table products {
  id varchar [pk]
  category_id varchar [ref: > product_categories.id, not null]
  name varchar [not null]
  risk_grade integer [not null, note: '내부 탐색용 1~6 등급']
  expected_period varchar [not null, note: '예상 투자 기간']
  description varchar [not null]
  created_at timestamp [not null]
  updated_at timestamp [not null]
}

Table category_recommendations {
  id varchar [pk]
  assessment_id varchar [ref: > assessments.id, not null]
  category_id varchar [ref: > product_categories.id, not null]
  rank_no integer [not null]
  reason varchar [not null, note: '해당 카테고리를 안내한 이유']
  created_at timestamp [not null]

  indexes {
    (assessment_id, category_id) [unique]
    (assessment_id, rank_no) [unique]
  }
}
```

## 4. UI 필드 ↔ 엔터티 매핑

| 화면 표시 항목 | 엔터티.필드 |
|---|---|
| 진단 질문 텍스트/선택지 | questions.content, question_choices.label |
| 진단 처리 상태 | assessments.status |
| 성향 분류명/분석 근거 | investment_profiles.profile_type, investment_profiles.reasoning |
| 탐색 카테고리/이유 | category_recommendations.category_id, category_recommendations.reason |
| 상품명/위험도/투자기간/설명/면책 | products.name, products.risk_grade, products.expected_period, products.description, API 상수 면책 문구 |

## 5. 분석 데이터 경계

- 입력: 질문 내용, 사용자가 고른 선택지의 `label`과 `analysis_key`
- 규칙 엔진 출력: 0~100 점수, 5단계 성향, 내부 위험 필터를 통과한 카테고리 ID·순위·이유
- LLM 출력: 실제 답변을 근거로 한 설명 문장만 생성
- 검증: 규칙 엔진 결과는 범위·카탈로그 존재 여부를 확인하고, LLM 설명은 길이·금지 표현 검증을 통과하지 못하면 템플릿으로 대체한다.
- 저장: AI 원문 응답 전체가 아니라 검증을 통과한 설명과 구조화 결과만 저장한다.

## 6. P0 데이터 불변식

- 활성 질문의 `weight` 합은 100이다.
- 하나의 진단에는 활성 질문별 답변이 정확히 한 건씩 있다.
- `question_choices.question_id`와 제출된 문항 ID가 일치하지 않는 선택지는 저장하지 않는다.
- P0 시드 데이터에서는 상품과 소속 카테고리의 `risk_grade`를 일치시킨다.
- 과거 진단 결과는 결과 저장 시점의 점수·성향·설명·카테고리 순위를 보존한다.

## 7. 2차 Agile 확장 지점 (MVP DBML 제외)

- 상품 비교: 별도 저장 없이 다건 조회로 시작
- 포트폴리오: `portfolios`, `portfolio_items`
- 관리자: `admins` 또는 역할 기반 사용자 권한과 질문·상품 변경 이력
- 진단 추이: 기존 `assessments` 다건을 활용한 비교 조회
- 실시간 금융 데이터: 상품 마스터와 시세 스냅샷 분리


## 8. 2차 고도화에서 구현된 테이블 (2026-10)

전체 스키마는 [FinMATE-DataModel-Final.dbml](../deliverables/FinMATE-DataModel-Final.dbml)(40 tables)에 있다. 1차 제출용 [FinMATE-DB.dbml](../deliverables/FinMATE-DB.dbml)은 MVP 기록으로 유지한다.

| 테이블 | 역할 | 핵심 제약 |
|---|---|---|
| `users.role` | USER/ADMIN | 공개 가입은 USER, ADMIN은 환경변수로만 생성 |
| `auth_sessions` | 로그인 세션 | 토큰 SHA-256만 저장, 만료·폐기 시각 |
| `glossary_categories`, `glossary_terms` | 용어 사전 | 출처 필수, 관련 용어·학습 개념 존재 검증, `status`(PUBLISHED/DRAFT), `content_version` |
| `glossary_term_drafts` | 게시 전 초안 | 용어당 1건, `base_version`으로 게시 충돌 검사 |
| `glossary_term_revisions` | 게시 이력 | 생성·게시·공개 내림을 당시 내용과 함께 추가만 함 |
| `glossary_bookmarks` | 저장한 용어 | (user_id, term_slug) PK |
| `learning_concepts`, `learning_quiz_items` | 학습 개념 트리·확인 문제 | `concepts.json`에서 없는 개념만 시드, 상위 개념 레벨 = 레벨 − 1, 문항당 정답 1개·선택지 3개 이상·모든 선택지 해설(선택지는 순서대로 JSON 배열), `status`, `content_version` |
| `learning_concept_drafts` | 개념·문항 게시 전 초안 | 개념당 1건, `base_version`으로 게시 충돌 검사 |
| `learning_concept_revisions` | 개념 게시 이력 | 게시·공개 내림을 당시 본문·문항 전체와 함께 추가만 함 |
| `quiz_attempts`, `quiz_attempt_answers` | 확인 문제 시도 | 당시 문항·해설 스냅샷, 불변 |
| `quiz_review_state` | 문항별 최신 상태·복습일 | (user_id, question_id) unique, 1·3·7·14·30일 간격 |
| `assessments.decision_snapshot` | 진단 스냅샷 | 자유 서술 추정(`narrative`) 추가, **원문 미저장** |

데이터 불변식 추가

- 공개 API와 검색은 `status = PUBLISHED`인 용어만 다룬다.
- 시드 적재는 없는 slug만 넣고 관리자 수정본을 덮어쓰지 않는다.
- 자유 서술 원문은 어떤 테이블에도 저장하지 않는다(통합 테스트로 확인).
