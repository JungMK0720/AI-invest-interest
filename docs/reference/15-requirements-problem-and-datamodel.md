# 15. 요구사항 분석·문제 인식·기획 배경·데이터 모델 (제출용 통합본)

> 보존 문서: FinMATE 당시의 설계·검증 기록입니다. 현재 제품 방향은 [제품 비전](../product/vision.md), 현행 요구사항은 [문서 안내](../README.md)를 따릅니다. 본문의 완료 표시는 이번 정리 작업에서 재검증한 결과가 아닙니다. 코드 링크의 organization 저장소와 `main` 경로는 분리 후 목적지이며 원격 반영은 별도 확인이 필요합니다.
> 작성일: 2026-09-10
> 목적: 우선순위 1(요구사항 분석·문제 인식)과 2(Data Model)를 한 문서에 모아, 그대로 복사해 제출 문서·슬라이드에 붙여넣을 수 있게 한다.
> 근거 문서: [01-requirements.md](01-requirements.md)(P0 상세), [12-product-strategy-and-user-journeys.md](12-product-strategy-and-user-journeys.md)(문제 인식 전문), [13-functional-nonfunctional-usecases.md](13-functional-nonfunctional-usecases.md)(FR/NFR/UC 전문)

---

## 1. 기획 배경

FinMATE는 원래 "투자성향을 진단하고 상품을 추천하는 서비스"로 시작했다. 그러나 실사용 후기를 반영하는 과정에서 두 가지를 다시 정의했다.

1. **진단 등급이 지식의 전부가 아니다.** 안정형으로 분류되면 공격적인 상품 정보 자체를 볼 수 없는 구조는, "이 상품이 왜 위험한지 배우고 싶다"는 요구를 막는다. 등급(거래 가능 여부)과 학습(개념 이해)을 같은 게이트로 묶은 것이 원인이다.
2. **AI의 역할이 장식적이었다.** 초기 구조에서 AI는 "근거 설명 문장"만 생성했고 실패 시 템플릿으로 완전히 대체 가능했다. AI를 빼도 서비스가 그대로 동작해 "AI 웹 서비스"로 보기 어렵다는 문제 인식에 도달했다.

이에 따라 제품 한 문장을 다음으로 재정의했다.

> FinMATE는 투자성향 등급을 받아 상품을 고르는 서비스가 아니라, 사용자가 상품의 돈의 흐름과 위험이 바뀌는 조건을 학습하고, 가정된 시나리오에 직접 입력해 본 뒤, 공식 자료를 확인할 수 있게 하는 금융 의사결정 학습 도구다.

AI는 진단 등급 판정에는 관여하지 않고(규칙 엔진이 전담), **학습·연습 경로의 개인화 엔진**(오답 패턴 분석, 개념 질문 응답, 실험 결과 해설)으로 재배치했다. 이 재배치의 상세 근거는 [14-ai-service-integration.md](14-ai-service-integration.md)에 있다.

---

## 2. 문제 인식 (관찰 · 사용자 진술 · 가설 분리)

| 구분 | 근거 | 제품 처리 | 검증 방법 |
|---|---|---|---|
| 사용자 제보 | "안정형으로 분류되어 원하는 상품을 바로 보지 못했다" | 등급(거래 가능 여부)과 학습 접근을 분리 | 제한 적용 후 행동 사용성 테스트 |
| 구현 관찰 | 기존 확인 질문이 여러 개념에서 동일 선택지를 재사용 | 개념별 문항·오답 이유·다음 행동으로 분리(59개 개념, 75개 문항) | 문항별 오답률·재시도율 |
| 제품 가설 | 구조 그림 + 숫자 실험이 글만 읽는 것보다 메커니즘 이해를 높인다 | 개념→그림→입력→결과→공식 자료 흐름 | 대조군과 사후 설명 비교 |
| 제품 가설 | 사용자는 상품명보다 결과를 바꾸는 변수를 먼저 알아야 한다 | 상품명 없는 시나리오와 민감도 실험(`/practice`) | 자유 답변에서 원인 변수 언급 여부 |
| 미확인 | 초보자·파생상품 경험자에게 필요한 적정 난이도 | 난이도(BASIC~ADVANCED)와 선택 경로 제공 | 5~8명 인터뷰·태스크 테스트 |
| 미확인 | 실시간 최신성의 필요와 비용 수용도 | 기준 시각·지연 표시(데이터 플랫폼 설계) | 실데이터 연결 전후 조사 |

**"완벽한 이해"는 완료 조건으로 쓰지 않는다.** 목표는 설명 가능한 판단 과정과 다음 확인 행동을 남기는 것이며, 실제 이해도는 반복 평가와 행동 관찰로 검증한다.

### 재진단 제한의 실제 근거

FinMATE의 월 1회 제한은 금융권 공통 규정이 아니라 **자체 제품 정책**(`AssessmentPeriodPolicy`, `monthly-kst-v1`)이다. 실제 금융기관 정책은 기관마다 다르다.

- KDB: 비대면 평가 일 최대 3회(만 65세 이상·장애인은 1일 1회), 5영업일 이내 재평가는 등급 상향 2단계 이내로 제한
- 신한투자증권: 온라인 설명·위험등급 절차를 금융소비자보호 기준으로 운영

FinMATE는 이 차이를 화면에서 숨기지 않고 "FinMATE 자체 정책"으로 명시한다.

---

## 3. User Pain Point (페르소나별)

| 페르소나 | 상황 | Pain Point | Jobs-to-be-Done | 제공 기능 |
|---|---|---|---|---|
| **금융 초보자** | 예금·주식·채권·펀드·ETF 이름은 들었지만 금리·NAV·보수·스프레드·만기를 연결하지 못함 | 상품 홍보 문구와 전문용어가 섞인 검색 결과만 나옴 | 돈이 어떤 경로로 움직이고 언제 현금화되는지 이해하기 | 자산군→상품→메커니즘 계층, 한 문장 정의, 돈의 흐름 그림, 초급 문항, 공식 문서 체크리스트 |
| **진단 제한 사용자** | 월 1회 제한·현재 등급 때문에 공격적 상품을 바로 거래하지 못함 | 등급이 곧 "이 상품을 알 자격이 없다"로 오해됨 | 현재 등급을 바꾸지 않고도 구조와 위험을 공부하기 | 상태·제한 근거·다음 평가 시점을 분리 표시, 등급과 무관한 학습 경로, 위험 카드, 가정 실험 |
| **이론 학습자** | "금리가 오르면 채권 가격이 내린다"는 읽었지만 크기(민감도)를 모름 | 개념 설명이 정성적 텍스트에 머무름 | 가상 입력을 바꾸고 어느 변수가 결과를 바꿨는지 확인하기 | `/practice` 가정·공식·결과·민감도 비교, seed 재현 |
| **경험자·멘토(후순위)** | 심화 개념과 원문을 빠르게 확인하거나 같은 시나리오로 교육하고 싶음 | 초급 콘텐츠만 있으면 이탈 | 같은 seed·시나리오를 재현해 비교·교육하기 | ADVANCED 난이도 문항, seed 고정 재현, 방법론·가정 공개(`/practice/methodology`) |

---

## 4. 요구사항 분석 요약

전체 상세는 [13-functional-nonfunctional-usecases.md](13-functional-nonfunctional-usecases.md) 참고. 아래는 제출·발표용 요약이다.

### 기능 요구사항 (FR-01~FR-20, P0/P1/P2)

- **P0(발표·구현 완료)**: 회원가입·로그인, 진단 가능 여부 확인(월 1회 정책), 진단 질문 조회·답변 제출, 규칙 기반 성향 판정, AI 근거 설명(+검증·템플릿 폴백), 결과·고지 조회, 카테고리·상품 탐색, 개념 트리·학습 카드(59개), 개념별 확인 문제(75개)·채점, 메커니즘 시뮬레이션, 교육용 포트폴리오 실험·비교(`/practice`), 방법론·가정·출처 조회
- **P1(배포형 고도화 설계)**: 진단 이력·변화 비교, 학습 진도·오답노트 저장, 실데이터 수집·정규화·스냅샷, 과거 자료 탐색, 콘텐츠·상품·출처 검수·게시, 데이터 신선도·오류 안내
- **P2(명시적 제외)**: 실제 계좌·주문 연동 — 별도 보안·규제·계약 검토 없이는 착수하지 않는다

### 비기능 요구사항 (NFR-01~NFR-14, 4개 그룹)

| 그룹 | 대표 항목 |
|---|---|
| 보안·안전 | 권한 분리(NFR-01), 안전한 표현·오해 방지(NFR-03), 개인정보 최소화(NFR-09) |
| 데이터 품질·추적성 | 결과 재현성(NFR-02), 데이터 정확성(NFR-04), 출처·기준일 추적(NFR-05), 저작권·출처(NFR-14) |
| 성능·가용성 | p95 응답 목표(NFR-06), 장애 시 폴백(NFR-07), 관측 가능성(NFR-10) |
| 유지보수·검증 | 멱등성(NFR-08), 접근성(NFR-11), 유지보수성(NFR-12), 테스트 가능성(NFR-13) |

### 완료 기준

1. `docs/07-usecase.puml`의 각 P0 유스케이스가 FR·화면·API 중 하나 이상과 연결된다.
2. P0 화면에서 입력·성공·빈 상태·오류·폴백·면책 문구를 확인할 수 있다.
3. 학습·시뮬레이션 결과에 가정·seed·계산식 버전·출처 표시가 있다.
4. `mvn test`, `npm run build`, `git diff --check`가 통과한다.

---

## 5. Data Model — 개요

전체 스키마는 6절의 DBML 코드 블록 전체를 복사해 [dbdiagram.io](https://dbdiagram.io/home)에 붙여넣으면 ERD로 볼 수 있다. 원본 파일은 [FinMATE-DataModel-Final.dbml](../contracts/FinMATE-DataModel-Final.dbml)이다.

**총 29개 테이블**을 5개 도메인으로 나눈다. 각 테이블에는 구현 상태를 표시했다.

| 구현 상태 | 의미 | 개수 |
|---|---|---|
| `[구현됨]` | 실제 JPA 엔터티로 존재 (H2/PostgreSQL) | 9 |
| `[구현: JSON]` | `concepts.json` 등 classpath 정적 콘텐츠가 대신 수행 | 6 |
| `[구현: 정적 카탈로그]` | `PracticeCatalog` 등 코드 내 정적 데이터가 대신 수행 | 2 |
| `[설계 제안·P1]` | 배포형 고도화를 위해 이번에 새로 설계 | 12 |

| 도메인 | 테이블 | 핵심 설계 포인트 |
|---|---|---|
| 계정·인증 | `users` | 이메일/BCrypt |
| 진단 (규칙 엔진 판정) | `questions`, `question_choices`, `assessments`, `answers`, `investment_profiles`, `assessment_reasoning_generations`, `product_categories`, `products`, `category_recommendations` | `assessments`는 `(user_id, submission_period)` unique로 월 1회 정책을 DB 레벨에서 강제. `decision_snapshot`은 불변 JSON. `assessment_reasoning_generations`는 AI 설명 검증(`ReasoningValidator`) 이력을 새로 남겨 재현성·관측가능성을 증명 |
| 학습 (개념·퀴즈·AI 설명) | `concepts`, `content_revisions`, `quiz_items`, `quiz_options`, `quiz_attempts`, `quiz_attempt_answers`, `simulation_scenarios`, `concept_simulations`, `simulation_runs`, `learning_progress`, `concept_ai_explanations` | `concepts`~`quiz_options`는 현재 `concepts.json`(59개 개념·75개 문항)을 그대로 옮긴 구조. `concept_ai_explanations`는 "AI에게 다시 묻기" 호출 이력을 새로 설계해 AI Service의 감사 로그를 남긴다 |
| 모의투자 연습기 | `practice_products`, `practice_scenarios`, `practice_runs`, `practice_run_comparisons` | 현재 `PracticeEngine`은 stateless 순수 계산. `practice_runs`/`practice_run_comparisons`을 새로 설계해 "내 실험 다시 보기"·전후 비교 이력을 남긴다 |
| 실데이터 플랫폼 | `source_registry`, `source_documents`, `instrument_master`, `instrument_snapshots` | 공식 API 우선 → 원문 보존(`content_hash`) → 정규화 → 시점별 스냅샷(`instrument_snapshots`, 최신값을 덮어쓰지 않음) 순서로 "과거 자료" 탭을 지원 |

### 핵심 관계

```
users 1─N assessments 1─1 investment_profiles
assessments 1─N answers, 1─N category_recommendations, 1─N assessment_reasoning_generations
concepts 1─N content_revisions 1─N quiz_items 1─N quiz_options
concepts N─N simulation_scenarios (concept_simulations)
users 1─N quiz_attempts / simulation_runs / practice_runs / concept_ai_explanations / learning_progress
source_registry 1─N source_documents 1─N instrument_snapshots
practice_runs 1─N practice_run_comparisons (before/after 두 건씩 참조)
```

---

## 6. Data Model — dbdiagram.io 붙여넣기용 DBML (전체)

아래 코드 블록 전체를 복사해 dbdiagram.io 편집 영역에 붙여넣으면 즉시 ERD가 렌더링된다. (검증 완료: `dbml2sql --postgres`로 29개 테이블 모두 SQL 변환 확인)

```dbml
// =============================================================================
// FinMATE — 최종 배포 목표 데이터 모델 (통합본)
//
// dbdiagram.io (https://dbdiagram.io/home) 에 이 파일 전체를 붙여넣으면 ERD로 볼 수 있다.
//
// 각 테이블 Note의 접두어로 구현 상태를 구분한다.
//   [구현됨]      실제 JPA 엔터티/영속 데이터로 존재한다 (deliverables/FinMATE-DB.dbml과 1:1 일치)
//   [구현: JSON]  현재는 classpath JSON(concepts.json)·정적 카탈로그로 존재하고, 배포 시 이 테이블로 대체한다
//   [설계 제안]   아직 코드/DB에 없다. 최종 배포 고도화(P1~P2)를 위해 이번에 새로 설계한 테이블이다
//
// 근거 코드(현재 저장소 기준):
//   backend/src/main/java/com/finmate/assessment/{Assessment,DecisionSnapshot,AssessmentPeriodPolicy}.java
//   backend/src/main/java/com/finmate/analysis/{ReasoningValidator,ProfileClassifier,SuitabilityMatcher}.java
//   backend/src/main/java/com/finmate/learning/{LearningCatalog,LearningAiService,ClaudeLearningTutor,QuizGrader}.java
//   backend/src/main/java/com/finmate/practice/{PracticeCatalog,PracticeEngine,PracticeDtos}.java
//   backend/src/main/java/com/finmate/integration/{MarketDataProvider,OpenDartProvider}.java
//   backend/src/main/resources/learning/concepts.json (version: learning-content-v3-2026-09-10, 59개 개념)
// =============================================================================

// -----------------------------------------------------------------------------
// SECTION 1. 계정·인증
// -----------------------------------------------------------------------------

Table users {
  id            varchar   [pk, note: 'UUID']
  email         varchar   [not null, unique]
  password_hash varchar   [not null, note: 'BCrypt 해시. 평문 비밀번호는 저장하지 않는다']
  name          varchar
  created_at    timestamp [not null]

  Note: '[구현됨] 일반 사용자 계정'
}

// -----------------------------------------------------------------------------
// SECTION 2. 투자성향 진단 (규칙 엔진이 판정, AI는 근거 설명만 생성)
// -----------------------------------------------------------------------------

Table questions {
  id        varchar [pk]
  content   varchar [not null]
  order_no  integer [not null, unique, note: '화면 노출 순서']
  weight    integer [not null, note: '문항 배점 비중. 활성 문항의 합이 100']
  is_active boolean [not null, default: true]

  Note: '[구현됨] 투자자 성향 진단 문항. 과제용 예시 질문이며 실제 투자자 분류 절차를 대체하지 않는다'
}

Table question_choices {
  id           varchar [pk]
  question_id  varchar [ref: > questions.id, not null]
  label        varchar [not null, note: '화면에 노출되는 문구']
  analysis_key varchar [not null, note: '문구가 바뀌어도 유지되는 의미 키. AI 프롬프트 입력에 사용']
  score        integer [not null, note: '0~100 원점수. 클라이언트에는 내려주지 않는다']
  order_no     integer [not null]

  indexes {
    (question_id, order_no) [unique]
  }

  Note: '[구현됨] 선택지. score가 노출되면 사용자가 원하는 성향에 맞춰 답을 고를 수 있어 API 응답에서 제외한다'
}

Table assessments {
  id                 varchar   [pk, note: 'UUID']
  user_id            varchar   [ref: > users.id, not null]
  status             varchar   [not null, note: 'PROCESSING | COMPLETED | FAILED']
  failure_reason     varchar   [note: '사용자에게 직접 노출하지 않는 내부 실패 사유']
  submission_period  varchar   [note: 'AssessmentPeriodPolicy(monthly-kst-v1) 기준 한국 달력 월(YYYY-MM). (user_id, submission_period) unique']
  decision_snapshot  text      [note: 'DecisionSnapshot record를 JSON으로 저장(SnapshotCodec). 완료 후 불변']
  created_at         timestamp [not null]
  completed_at       timestamp

  indexes {
    (user_id, submission_period) [unique, note: 'AssessmentPeriodPolicy: 월 1회 정책. FinMATE 자체 정책이며 금융권 공통 규정이 아니다']
  }

  Note: '[구현됨] 사용자의 1회 진단 세션. 월 1회 제출 제한(자체 정책)'
}

Table answers {
  id            varchar [pk, note: 'UUID']
  assessment_id varchar [ref: > assessments.id, not null]
  question_id   varchar [ref: > questions.id, not null]
  choice_id     varchar [ref: > question_choices.id, not null]

  indexes {
    (assessment_id, question_id) [unique, note: '한 진단에서 같은 문항에 두 번 답할 수 없다']
  }

  Note: '[구현됨] 진단 내 문항별 답변'
}

Table investment_profiles {
  id               varchar   [pk, note: 'UUID']
  assessment_id    varchar   [ref: - assessments.id, not null, unique]
  profile_type     varchar   [not null, note: 'AGGRESSIVE | ACTIVE | NEUTRAL | CONSERVATIVE | STABLE']
  total_score      integer   [not null, note: '0~100 가중 평균 점수']
  reasoning        varchar   [not null, note: '판단 근거 설명 문장']
  reasoning_source varchar   [not null, note: 'LLM | TEMPLATE. ReasoningValidator를 통과하지 못하면 TEMPLATE로 대체']
  created_at       timestamp [not null]

  Note: '[구현됨] 성향 판정 결과. profile_type·total_score는 규칙 엔진이, reasoning 문장만 AI가 생성한다'
}

Table assessment_reasoning_generations {
  id               varchar   [pk, note: 'UUID']
  assessment_id    varchar   [ref: > assessments.id, not null]
  attempt_no       integer   [not null]
  provider         varchar   [not null, note: 'CLAUDE | TEMPLATE']
  raw_text         text      [note: 'ReasoningValidator에 넣기 전 LLM 원문. 채택되지 않아도 감사용으로 남긴다']
  validation_passed boolean  [not null]
  rejected_reason  varchar   [note: '길이초과 | 금칙어 | 등급불일치 | 점수불일치 | 타임아웃 | API키없음']
  model            varchar   [note: '예: claude-opus-5']
  latency_ms       integer
  created_at       timestamp [not null]

  Note: '[설계 제안·P1] ReasoningValidator(현재는 boolean만 반환) 판정 이력을 저장해 NFR-02 재현성·NFR-10 관측가능성을 증명한다'
}

Table product_categories {
  id          varchar [pk]
  name        varchar [not null, unique]
  risk_grade  integer [not null, note: '금융투자상품 위험등급 1~6. 1등급이 가장 위험이 크다']
  description varchar [not null]

  Note: '[구현됨] 탐색 대상 상품 카테고리. risk_grade는 과제용 내부 필터 기준이다'
}

Table products {
  id              varchar   [pk]
  category_id     varchar   [ref: > product_categories.id, not null]
  name            varchar   [not null]
  risk_grade      integer   [not null, note: '금융투자상품 위험등급 1~6']
  expected_period varchar   [not null]
  description     varchar   [not null]
  created_at      timestamp [not null]
  updated_at      timestamp [not null]

  Note: '[구현됨] 카테고리에 속한 사전 등록 예시 상품. 실제 판매 상품이 아니다'
}

Table category_recommendations {
  id            varchar   [pk, note: 'UUID']
  assessment_id varchar   [ref: > assessments.id, not null]
  category_id   varchar   [ref: > product_categories.id, not null]
  rank_no       integer   [not null]
  reason        varchar   [not null]
  created_at    timestamp [not null]

  indexes {
    (assessment_id, category_id) [unique]
    (assessment_id, rank_no)     [unique]
  }

  Note: '[구현됨] 내부 탐색 필터(상품 위험등급 >= 투자자 성향 등급)를 통과한 카테고리만 저장'
}

// -----------------------------------------------------------------------------
// SECTION 3. 학습 개념 트리·확인 문제·AI 개념 설명
//   현재는 concepts.json(version learning-content-v3-2026-09-10, 59개 개념, 75개 문항)이
//   아래 concepts~quiz_options 구조를 그대로 대신하고 있다. 배포 단계에서 이 테이블이 JSON을 대체한다.
// -----------------------------------------------------------------------------

Table concepts {
  id             varchar [pk]
  slug           varchar [unique, not null]
  parent_id      varchar [ref: > concepts.id, note: 'null이면 최상위 대분류(level 1)']
  level          integer [not null, note: '1=대분류, 2=핵심 개념, 3=세부 개념']
  sort_order     integer [not null]
  title          varchar [not null]
  summary        varchar [not null]
  one_liner      varchar [not null, note: '한 문장 정의']
  diagram_key    varchar [not null, note: '구조 그림 종류 (예: FOUNDATION, BOND_DURATION)']
  simulation_id  varchar [ref: > simulation_scenarios.id, note: '이 개념의 기본 실험 시나리오']
  status         varchar [not null, note: 'DRAFT | REVIEW | PUBLISHED | RETIRED']

  indexes { (parent_id, sort_order) }

  Note: '[구현: JSON] 3단계 개념 트리. concepts.json의 slug/parent/level/order/title/summary/oneLiner/diagramKey/simulationId와 1:1'
}

Table content_revisions {
  id                  varchar   [pk]
  concept_id          varchar   [ref: > concepts.id, not null]
  revision_no         integer   [not null]
  content_version     varchar   [not null, note: '응답과 학습 진도에 함께 기록. 현재 값: learning-content-v3-2026-09-10']
  explanation         text      [not null, note: 'concepts.json의 explanation[] 단락 배열을 합친 본문']
  key_points          text      [note: 'JSON 배열']
  checklist           text      [note: 'JSON 배열: 상품에서 확인할 문서·항목']
  misconception       text
  diagram_spec        text
  source_document_id  varchar   [ref: > source_documents.id]
  source_name         varchar   [not null, note: '출처 없는 개념은 공개하지 않는다']
  source_url          varchar   [not null]
  reviewer            varchar
  reviewed_at         timestamp
  effective_from      timestamp [not null]
  effective_to        timestamp

  indexes { (concept_id, revision_no) [unique] }

  Note: '[구현: JSON] concepts.json 필드가 대신함. 개정 이력을 버전으로 분리해 "과거 자료" 탭을 만들 때 이 테이블이 필요하다'
}

Table quiz_items {
  id                   varchar [pk]
  concept_id           varchar [ref: > concepts.id, not null]
  content_revision_id  varchar [ref: > content_revisions.id]
  prompt               text    [not null]
  difficulty           varchar [not null, note: 'BASIC | INTERMEDIATE | ADVANCED']
  takeaway             text    [not null, note: '정오답과 무관하게 남기는 한 줄 결론']
  sort_order           integer [not null]

  Note: '[구현: JSON] 확인 문제. concepts.json의 quiz[]. 정답·해설은 서버에만 두고 조회 응답에서 제외(QuizGrader)'
}

Table quiz_options {
  id           varchar [pk]
  quiz_item_id varchar [ref: > quiz_items.id, not null]
  option_index integer [not null]
  label        text    [not null]
  is_correct   boolean [not null]
  rationale    text    [not null, note: '오답이어도 필수. 왜 틀렸는지 없으면 학습 도구가 아니다']

  indexes { (quiz_item_id, option_index) [unique] }

  Note: '[구현: JSON] concepts.json quiz[].options[]'
}

Table quiz_attempts {
  id               varchar   [pk]
  user_id          varchar   [ref: > users.id, not null]
  concept_id       varchar   [ref: > concepts.id, not null]
  content_version  varchar   [not null, note: '채점 시점 콘텐츠 버전. 이후 개정돼도 기록 유지']
  total            integer   [not null]
  correct_count    integer   [not null]
  score_percent    integer   [not null]
  attempted_at     timestamp [not null]

  Note: '[설계 제안·P1] 로그인 사용자별 진도 저장. 현재는 POST /learning/concepts/{slug}/quiz/grade가 즉시 채점만 하고 저장하지 않는다'
}

Table quiz_attempt_answers {
  id              varchar [pk]
  attempt_id      varchar [ref: > quiz_attempts.id, not null]
  quiz_item_id    varchar [ref: > quiz_items.id, not null]
  selected_index  integer [not null, note: '-1은 미응답']
  correct         boolean [not null]

  indexes { (attempt_id, quiz_item_id) [unique] }

  Note: '[설계 제안·P1] 문항별 오답노트의 근거 데이터'
}

Table simulation_scenarios {
  id          varchar [pk]
  title       varchar [not null]
  question    varchar [not null]
  kind        varchar [not null, note: 'PRICE_PATH | BOND_RATE_SHOCK | FEE_DRAG | ETF_TRACKING | DIVERSIFICATION | LIQUIDITY_EXIT | LEVERAGE_DECAY | OPTION_PAYOFF | FUTURES_MARGIN | DEPOSIT_INFLATION | DCA_VS_LUMPSUM']
  mechanism   text    [not null, note: '계산식 요약. 결과를 검증 가능하게 만든다']
  input_spec  text    [not null, note: 'JSON: 입력 필드 명세(min/max/기본값)']
  randomised  boolean [not null]
  active      boolean [not null]

  Note: '[구현: JSON] SimulationCatalog가 대신함. 개념별 메커니즘 시뮬레이션(금리·수수료·옵션 등) 정의'
}

Table concept_simulations {
  concept_id  varchar [ref: > concepts.id, not null]
  scenario_id varchar [ref: > simulation_scenarios.id, not null]
  sort_order  integer [not null]

  indexes { (concept_id, scenario_id) [pk] }

  Note: '[구현: JSON] 개념 ↔ 시나리오 다대다'
}

Table simulation_runs {
  id            varchar   [pk]
  user_id       varchar   [ref: > users.id]
  scenario_id   varchar   [ref: > simulation_scenarios.id, not null]
  concept_id    varchar   [ref: > concepts.id]
  inputs_json   text      [not null, note: '명세 범위로 보정된 실제 적용값']
  seed          bigint    [not null]
  headline      varchar   [not null]
  executed_at   timestamp [not null]

  indexes { (user_id, executed_at) }

  Note: '[설계 제안·P1] seed+입력을 남기면 같은 결과를 재현할 수 있다(NFR-02). 현재는 요청마다 즉시 계산만 하고 저장하지 않는다'
}

Table learning_progress {
  id                    varchar [pk]
  user_id               varchar [ref: > users.id, not null]
  content_revision_id   varchar [ref: > content_revisions.id, not null]
  viewed_at             timestamp
  completed_at          timestamp
  quiz_score            integer
  last_attempt_id       varchar [ref: > quiz_attempts.id]
  simulation_run_count  integer [not null, default: 0]

  indexes { (user_id, content_revision_id) [unique] }

  Note: '[설계 제안·P1] 로그인 사용자의 개념별 학습 진도·오답노트(FR-15)'
}

Table concept_ai_explanations {
  id                varchar   [pk]
  user_id           varchar   [ref: > users.id, note: '비로그인 질문도 허용하면 null']
  concept_id        varchar   [ref: > concepts.id, not null]
  content_version   varchar   [not null]
  question          text      [note: '비어 있으면 "이 개념을 초보자 눈높이로 다시 설명"']
  answer            text      [not null]
  source            varchar   [not null, note: 'CLAUDE | TEMPLATE (LearningAiService 판정 결과)']
  source_name       varchar   [not null]
  source_url        varchar   [not null]
  latency_ms        integer
  created_at        timestamp [not null]

  Note: '[설계 제안·P1] POST /learning/concepts/{slug}/explain 호출 이력. 현재는 매 요청을 저장 없이 바로 응답한다(docs/14 1단계: 요청 로그 계획)'
}

// -----------------------------------------------------------------------------
// SECTION 4. 교육용 모의투자 연습기 (계산은 결정적 엔진, DB 없이 stateless로 동작 중)
// -----------------------------------------------------------------------------

Table practice_products {
  id                    varchar [pk]
  name                  varchar [not null]
  asset_class           varchar [not null, note: '주식 | 채권 | 현금성 | 금 | 부동산 | 파생 등']
  mechanism             varchar [not null]
  driver                varchar [not null, note: '결과를 바꾸는 핵심 변수 (금리, 신용스프레드, 환율 등)']
  duration              decimal [note: '채권 듀레이션 등 민감도 계수']
  annual_fee_percent    decimal
  leverage              decimal [note: '기본 노출 배수']
  foreign_currency      boolean [not null]
  concept_id            varchar [ref: > concepts.id, note: '이 상품이 설명하는 학습 개념과 연결']
  explanation           text

  Note: '[구현: 정적 카탈로그] PracticeCatalog.ProductSpec. 현재 16종(현금·주식·ETF·펀드·채권·금·리츠·부동산·신용주식·선물·옵션 등)'
}

Table practice_scenarios {
  id                       varchar [pk]
  name                     varchar [not null]
  description              varchar [not null]
  equity_percent           decimal [not null]
  rate_points              decimal [not null]
  credit_spread_points     decimal [not null]
  gold_percent             decimal [not null]
  real_estate_percent      decimal [not null]
  fx_percent               decimal [not null]
  days                     integer [not null]

  Note: '[구현: 정적 카탈로그] PracticeDtos.Shocks 사전 구성 시나리오. 사용자는 커스텀 값도 직접 입력 가능'
}

Table practice_runs {
  id                       varchar   [pk]
  user_id                  varchar   [ref: > users.id, note: '비로그인 실험도 허용하면 null']
  model_version            varchar   [not null, note: 'PracticeEngine 계산식 버전(Analysis.modelVersion)']
  budget                   decimal   [not null]
  holdings_json            text      [not null, note: '요청 시점의 Portfolio.holdings 원본']
  shocks_json              text      [not null]
  result_json              text      [not null, note: 'Analysis 응답 전체(자산배분·손익·기여도·한계)']
  ending_value             decimal   [not null]
  profit_loss              decimal   [not null]
  created_at               timestamp [not null]

  indexes { (user_id, created_at) }

  Note: '[설계 제안·P1] POST /practice/analyze 실행 이력. 현재는 순수 계산 API로 아무 것도 저장하지 않는다. 저장 시 "내 실험 다시 보기"가 가능해진다'
}

Table practice_run_comparisons {
  id                     varchar   [pk]
  before_run_id          varchar   [ref: > practice_runs.id, not null]
  after_run_id           varchar   [ref: > practice_runs.id, not null]
  profit_loss_difference decimal   [not null]
  interpretation         text      [not null]
  created_at             timestamp [not null]

  Note: '[설계 제안·P1] POST /practice/compare 이력. before/after 두 practice_runs를 연결'
}

// -----------------------------------------------------------------------------
// SECTION 5. 실데이터 플랫폼 (공식 API 우선 → 원문 보존 → 정규화 → 스냅샷)
//   MarketDataProvider 인터페이스(OpenDartProvider 등 구현체)가 이 경계를 통해 데이터를 공급한다.
// -----------------------------------------------------------------------------

Table source_registry {
  id                varchar   [pk]
  provider_key      varchar   [unique, not null, note: '예: opendart, krx']
  source_name       varchar   [not null]
  source_url        varchar   [not null]
  access_mode       varchar   [not null, note: 'API | DOCUMENT | MANUAL']
  terms_url         varchar
  rate_limit_note   varchar
  last_success_at   timestamp

  Note: '[설계 제안·P1] 허용된 데이터 공급자 등록. MarketDataProvider.providerKey()와 대응'
}

Table source_documents {
  id               varchar   [pk]
  source_id        varchar   [ref: > source_registry.id, not null]
  external_id      varchar   [not null, note: 'MarketDataProvider.RawDataRecord.externalId']
  document_type    varchar   [not null]
  published_at     timestamp
  observed_at      timestamp [not null]
  content_hash     varchar   [not null, note: '중복 수집 방지']
  storage_uri      varchar   [not null, note: '원문 보존 위치']
  source_url       varchar   [not null]
  status           varchar   [not null, note: 'ACTIVE | REPLACED | REMOVED | FAILED']

  indexes { (source_id, external_id, content_hash) [unique] }

  Note: '[설계 제안·P1] 수집 원문. 공시·설명서·투자자 안내 등을 시점별로 보존'
}

Table instrument_master {
  id               varchar [pk]
  external_code    varchar [not null, note: '종목코드·ISIN 등']
  name             varchar [not null]
  instrument_type  varchar [not null]
  issuer_name      varchar
  active           boolean [not null]

  Note: '[설계 제안·P1] 실제 상품/종목 기준정보'
}

Table instrument_snapshots {
  id                  varchar   [pk]
  instrument_id       varchar   [ref: > instrument_master.id, not null]
  as_of               timestamp [not null]
  price               decimal
  nav                 decimal
  risk_grade          integer
  expense_ratio       decimal
  liquidity_score     decimal
  source_document_id  varchar   [ref: > source_documents.id]

  indexes { (instrument_id, as_of) [unique] }

  Note: '[설계 제안·P1] 시점별 스냅샷. 최신값을 덮어쓰지 않아 "현재/과거 자료" 탭을 만들 수 있다'
}

// =============================================================================
// 관계 요약
//   users 1─N assessments 1─1 investment_profiles
//   assessments 1─N answers, 1─N category_recommendations, 1─N assessment_reasoning_generations
//   concepts 1─N content_revisions 1─N quiz_items 1─N quiz_options
//   concepts N─N simulation_scenarios (concept_simulations)
//   users 1─N quiz_attempts / simulation_runs / practice_runs / concept_ai_explanations / learning_progress
//   source_registry 1─N source_documents 1─N instrument_snapshots
//   practice_runs 1─N practice_run_comparisons (before/after 두 건씩)
// =============================================================================
```

---

## 7. Self-Reflection

- 문제 인식은 사용자 제보 1건과 구현 관찰에서 출발한 가설이다. 모집단 연구가 아니므로 출시 전 5~8명 태스크 테스트가 필요하다.
- `[설계 제안·P1]` 12개 테이블은 아직 DB에 없다. 지금 이 테이블들을 만들기보다, 먼저 스키마로 합의하고 사용자 인증·개인정보 보관 정책을 정한 뒤 붙이는 순서를 권장한다(현재 `PracticeEngine`·`LearningAiService`가 stateless인 이유이기도 하다).
- `assessment_reasoning_generations`·`concept_ai_explanations`는 이번에 새로 제안한 감사 로그 테이블이다. 실제로는 저장 비용·개인정보(질문 원문)를 고려해 보존 기간 정책이 함께 필요하다.
