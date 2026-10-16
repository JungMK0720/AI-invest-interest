# Changelog (요구사항/설계 변경 이력)

날짜는 KST 기준, 최신 항목이 위로 오도록 추가한다.

## 2026-10-16 (2차 고도화: 주식 용어 사전·학습 모델·개인화·운영)

- **주식 용어 사전**: 14개 분류 247개 용어(정의·설명·공식·예시·주의·관련 용어·연결 개념·출처)를 추가하고 DB 기반 API·북마크·서버 채점 용어 퀴즈를 구현했다. 세율·한도처럼 바뀌는 수치는 기준 시점을 밝히거나 공식 자료 확인으로 안내한다(ADR-012).
- **학습 모델 재설계**: 자유 서술 성향 신호를 40문장 나이브 베이즈에서 생성 코퍼스(1,500) + 개발 문장(40) 기반 softmax 로지스틱 회귀와 관심사 태그 9종으로 바꿨다. 사람이 쓴 보류 115문장에서 정확도 0.617(v1 0.522, 키워드 규칙 0.417), ±1등급 0.904, 주제 무관 문장 보류 100%. 용어 검색은 필드별 TF-IDF 가중합으로 test Top-1 0.860, 설명형 Top-1 0.415 → 0.792(ADR-013·015).
- **Python↔Java 일치 검사**: 보류 문장·평가 질문마다 Java 추론이 Python 정답과 같은지 `MlParityTest`로 고정했다. 모델 카드를 `GET /ml/models`와 모델 실험실 화면에 공개했다.
- **진단 자유 서술**: 진단 마지막에 선택형 자유 서술을 추가했다. 등급 확정 뒤 신호만 추정하고 원문은 저장하지 않으며, 설문과 크게 다르면 `NARRATIVE_MISMATCH` 플래그를 붙인다(ADR-014).
- **결함 수정**: 진단 근거 스냅샷이 DB에 저장되지 않아 결과·이력의 근거가 항상 비어 있던 문제를 고쳤다(식별자 직접 부여 엔터티의 `save()` 반환값 미사용).
- **개인화 학습 경로** `GET /me/learning-path`: 복습 시점 오답, 서술-설문 차이, 관심사 태그, 확인 플래그, 현재 성향 순으로 다음 개념·용어를 사유와 함께 안내한다. 학습·오답 기록(`quiz_attempts`, `quiz_attempt_answers`, `quiz_review_state`)과 1·3·7·14·30일 간격 복습을 구현했다.
- **운영 콘솔**: 용어 신규 등록(비공개 생성), 초안 저장·버리기, 검증 게시, 공개 내리기, 불변 게시 이력, 운영 현황을 추가했다. 검색 인덱스는 DB 공개 용어로 만들고 커밋 후 이벤트로 재구성해 게시가 즉시 반영된다(ADR-016).
- **세션**: 메모리 토큰을 DB 해시 세션(만료·로그아웃·`/auth/me`)으로 바꾸고 관리자 역할을 환경변수로만 부여한다(ADR-017).
- **화면**: 공개 첫 화면, 학습·용어·모의투자·모델 실험실 공개 접근과 로그인 후 원래 화면 복귀(ADR-018), 모바일 접기 메뉴, 진단 선택지 접근성 이름, 용어 사전 의미 검색, 퀴즈 seed 갱신.
- **문서**: README 전면 개편, [16 UR 분석](16-user-requirements-analysis.md), [17 사용자 시나리오](17-user-scenarios.md), [18 AI·데이터 거버넌스](18-ai-models-and-data-governance.md), ADR-012~018, OAS 2.0.0(50 paths), 최종 DBML(36 tables) 갱신.

## 2026-09-10 (요구사항 분석·문제 인식·데이터 모델 통합본)

- 우선순위 1(요구사항 분석·문제 인식)과 2(Data Model)를 제출·복사용으로 한 문서에 모은 `docs/15-requirements-problem-and-datamodel.md`를 추가했다. 기획 배경, 문제 인식(관찰/제보/가설 분리), 페르소나별 PainPoint, FR/NFR 요약, 데이터 모델 개요를 담는다.
- 기존 `deliverables/FinMATE-DB.dbml`(P0)과 `deliverables/FinMATE-DataPlatform.dbml`(배포형 초안)을 하나로 합친 `deliverables/FinMATE-DataModel-Final.dbml`을 추가했다. 총 29개 테이블(계정 1, 진단 9, 학습·AI 설명 11, 모의투자 이력 4, 실데이터 플랫폼 4)이며 `dbml2sql --postgres`로 문법 검증했다.
- 실제 구현(`Assessment`, `DecisionSnapshot`, `AssessmentPeriodPolicy`, `ReasoningValidator`, `PracticeEngine`, `LearningAiService`, `concepts.json`)과 대조해 테이블마다 `[구현됨]/[구현: JSON]/[구현: 정적 카탈로그]/[설계 제안·P1]` 상태를 표시했다. AI 설명 검증 이력(`assessment_reasoning_generations`), 개념 AI 질의응답 이력(`concept_ai_explanations`), 모의투자 실행·비교 이력(`practice_runs`, `practice_run_comparisons`)을 새로 설계해 현재 stateless로 동작하는 구간의 배포형 확장 지점을 명시했다.
- Figma에서 "AI 학습 코치" 와이어프레임(AI 학습 코치 홈, 퀴즈 결과 AI 맞춤 해설, 포트폴리오 AI 코멘터리) 3장을 제작하고 캡처해 `presentation/captures/10~12-*.png`에 저장, `presentation/FinMATE-플로우차트-와이어프레임.pptx`에 09~11번 슬라이드로 반영했다. AI의 역할을 "진단 근거 설명(대체 가능)"에서 "학습·연습 개인화 엔진(제거 시 서비스 가치가 실제로 빠짐)"으로 재배치한 기획 근거를 함께 슬라이드화했다.

## 2026-09-10 (기능·비기능 요구사항과 Use Case 확장)

- 기능 요구사항 FR-01~FR-20, 비기능 요구사항 NFR-01~NFR-14, P0/P1 범위를 `docs/13-functional-nonfunctional-usecases.md`에 정리했다.
- 학습·개념별 퀴즈·메커니즘 시뮬레이션·교육용 포트폴리오 실험과 실데이터 수집·과거 자료·콘텐츠 검수의 Use Case를 `docs/07-usecase.puml`에 반영했다.
- Sparx Enterprise Architect에서 프로젝트 패키지·Use Case·Requirements Diagram·FR/NFR 추적·이미지 내보내기까지 재현하는 발표용 SOP를 `presentation/FinMATE-Enterprise-Architect-UseCase-SOP.md`에 추가했다.
- 기존 발표자료의 16:9 스타일을 유지한 편집 가능한 플로우차트·와이어프레임 부록 `presentation/FinMATE-플로우차트-와이어프레임.pptx`와 화면/API 명세를 추가했다.

## 2026-09-08 (2차: React 프론트엔드 구현 및 실브라우저 E2E 검증)

- 배포 조합 확정: 프론트엔드 Netlify, 백엔드(Spring Boot 컨테이너) Render, DB Supabase Postgres. Netlify/Supabase 단독으로는 상시구동 JVM을 못 올리는 제약을 확인하고 이 조합으로 결정.
- 프론트엔드를 `frontend/`에 React + Vite + TypeScript로 신규 구현 — S1~S6 전 화면을 목업이 아닌 실제 API 연동으로 구현(회원가입/로그인, 진단 시작, 진단 진행 위저드, 분석 결과, 탐색 카테고리·상품 목록, 상품 상세).
- 백엔드에 CORS 설정 추가(`WebConfig`, `finmate.cors.allowed-origins`), dev는 `localhost:5173`, prod는 `FRONTEND_ORIGIN` 환경변수로 분리.
- 배포 설정 파일 추가: `backend/render.yaml`(Render 블루프린트), `frontend/netlify.toml`(SPA 라우팅 리다이렉트 포함).
- 실제 브라우저(Claude in Chrome)로 회원가입 → 로그인 → 진단 5문항 응답 → 제출 → 결과(TEMPLATE 폴백 배너까지 실제 렌더링 확인) → 탐색 카테고리 → 상품 상세까지 클릭으로 전체 시나리오를 재현했고 콘솔 에러 없음을 확인. 목업이 아니라 실동작임을 이 과정으로 검증.
- 남은 결정: 실제 Netlify/Render/Supabase 계정 생성과 배포 실행(계정 접근 권한이 필요해 사용자 조치 필요), 인증 저장소 고도화(ADR-009 후속).

## 2026-09-08 (2차: 배포 준비 시작)

- 1차 Agile 제출용 산출물(PDF용 와이어프레임, API YML, DBML, 발표자료 PPTX)을 마무리하고, 이후 작업은 "고도화 + 배포"로 전환.
- 백엔드 컨테이너화: `backend/Dockerfile`(멀티스테이지), `backend/docker-compose.yml`(app + Postgres) 추가.
- `application-prod.yml` 프로파일 추가 — Postgres 연결(env var 기반), `ddl-auto: update`, `sql.init.mode: never`(영구 DB 재시작 시 시드 재실행으로 인한 PK 충돌 방지).
- `docker compose up --build`로 실제 컨테이너 기동 → Postgres 연결 확인 → `data.sql`을 psql로 1회 시드(수정 없이 그대로 호환됨) → 회원가입/로그인/진단 제출 E2E 재현 → 컨테이너 재시작 후에도 데이터 유지됨을 확인(H2 인메모리와 달리 영구 저장 검증).
- 남은 결정 사항: 프론트엔드 프레임워크, 배포 호스팅 대상, 인증 저장소 고도화(ADR-009 후속). 결정되는 대로 이 changelog와 05-decisions.md에 ADR로 남긴다.

## 2026-09-08 (구현 실행 검증 및 유스케이스 재검증)

- `mvn test`(11개, 전부 통과)와 `mvn spring-boot:run`으로 백엔드를 실제 기동하고, 회원가입 → 로그인 → 질문 조회 → 진단 제출(`POST /assessments`) → 결과 조회 → 추천 조회 → 카테고리·상품 목록/상세까지 P0 핵심 시나리오를 curl로 end-to-end 재현했다. `ANTHROPIC_API_KEY` 미설정 상태에서 `reasoningSource=TEMPLATE`로 201 완료됨을 확인.
- 다른 사용자의 `assessmentId` 접근 시 403, 미인증 접근 시 401, 이메일 중복 가입 시 409, 잘못된 비밀번호 로그인 시 계정 존재 여부를 노출하지 않는 401을 각각 실제 응답으로 확인.
- 실행 로그에 이메일·비밀번호·토큰이 남지 않음을 확인했고, `docs`/`deliverables`/`backend/src` 전체에서 `투자 권유`·`수익 보장` 계열 표현을 전수 검색해 모두 부정문·면책 문구 용법임을 확인했다.
- `docs/07-usecase.puml`을 실제 렌더링해 기존 `usecase.png`와 비교, 07-usecase-review.md가 지적한 7개 문제(범위 혼재, 유스케이스 중복, 오명명, AI 관계 오배치, 화살표 오용, 추적성 부재, 시스템명)가 실물에 있었고 보완본에서 해소됨을 확인. 각 유스케이스에 `UC-01.`~`UC-13.` 번호를 다이어그램에 직접 표기해 추적성을 강화하고, 루트 `usecase.png`를 이 보완본으로 교체했다.
- 남은 P0-A 작업은 S1~S6 와이어프레임 캡처(디자인 도구 필요)뿐이며, 상세는 [08-supervisor-review-and-plan.md](08-supervisor-review-and-plan.md) 참고.

## 2026-09-08 (감시자 검토 및 기획 고도화)

- 문서·API·DBML·구현의 기준을 5단계 성향, 1~6 내부 위험등급, 규칙 엔진 판정, LLM 설명 폴백으로 통일.
- LLM 설명 생성 실패는 503이 아니라 `reasoningSource=TEMPLATE`로 완료하고, 문항·카테고리 데이터 준비 실패만 503으로 제한.
- 실제 투자 권유·적합성 판단으로 오인될 수 있는 표현을 내부 탐색 필터·안내 범위로 변경.
- 질문 배점 합, 답변 완전성, 카테고리·상품 위험등급 일치 등 P0 데이터 불변식과 제출 전 감시 체크포인트 추가.
- 감시자 검토·인수 기준·P0/P1 고도화 계획을 `docs/08-supervisor-review-and-plan.md`에 추가.

## 2026-09-08 (기획 검토)

- 1차 범위를 S1~S6(P0) 진단·분석·카테고리/상품 탐색으로 고정하고, 비교·포트폴리오·관리자 기능은 2차 Agile로 분리.
- ERD를 P0 엔터티 중심으로 정리하고, AI 결과를 개별 상품이 아닌 카테고리 추천으로 구조화.
- `POST /assessments`의 사용자 식별자 처리, 분석 실패 상태·응답, P0 인수 조건을 명시.

## 2026-09-08

- 프로젝트 주제를 "AI 감정 분석 음악 생성"과 "AI 투자 성향 분석 금융 서비스" 중 비교 후, 금융 도메인 포트폴리오 가치를 위해 후자로 확정.
- 서비스명을 FinMATE로 결정. 범위를 "실제 매매"가 아닌 "AI 분석 + 상품 탐색"으로 축소 (ADR-001, ADR-002).
- 00~05 기획 문서 초안 작성 완료. MVP(P0) 기능을 FR-01~FR-07로 확정.

## Backlog (2차 Agile 후보, 우선순위 미정)

- ~~자연어 자유 입력 기반 답변 분석 (ADR-004 참고)~~ → 2026-10-16 자유 서술 신호로 구현(ADR-014)
- ~~관리자 로그인/회원가입 플로우 추가 (ADR-003 참고)~~ → 2026-10-16 역할 기반 관리자(환경변수 생성)로 구현(ADR-017)
- 여러 진단 이력 간 성향 변화 추이 비교
- 실시간 금융 데이터 연동
- 상품 비교 화면(S7) 고도화 (다건 비교 UI)

## 2026-09-09 (2차) — 개념 트리·확인 문제·모의투자 고도화

- 학습 개념을 평면 18개에서 **3단계 트리 59개**(대분류 4 / 핵심 23 / 세부 32)로 재구성했다. 콘텐츠는 `backend/src/main/resources/learning/concepts.json`으로 분리해 개정을 코드 변경과 떼어 냈다.
- 좌측 트리에 노드별 펼침/접힘과 "세부 개념까지 보기" 토글을 추가했다. 검색 시에는 접힘 상태를 무시하고 일치 노드의 조상까지 펼친다.
- 확인 문제를 전 개념 공용 1문항에서 **75문항 225선택지**로 확장했다. 개념마다 다른 문항이 나오고, 정답·해설은 서버에만 두며 `POST /learning/concepts/{slug}/quiz/grade`로만 채점한다. 기존의 클라이언트 문자열 비교 판정(`startsWith`)을 제거했다.
- 모든 선택지에 오답 해설을 필수로 두고, 카탈로그 로드 시점에 검증한다. 정답이 1개가 아니거나 해설·출처가 없으면 애플리케이션이 기동하지 않는다.
- **모의투자 11개 시나리오**를 추가했다. 채권 금리 충격(현금흐름 할인·듀레이션·볼록성), 보수 누적, ETF 괴리·추적오차, 분산 효과, 유동성 비용, 레버리지 변동성 손실, 옵션 손익구조, 선물 증거금·일일정산, 예금 실질 구매력, 가격 경로, 적립식 대 거치식.
- 계산은 전부 서버에서 하고 입력 명세도 서버가 내려준다. seed가 같으면 결과가 항상 같아 발표·검증에서 재현 가능하며, "다른 시장 상황 보기"로 seed만 바꿀 수 있다.
- 시뮬레이션이 실제 금융 구조를 재현하는지 테스트로 고정했다. 액면가 채권의 정확한 가격, 만기별 금리 민감도 차이, 상관계수 1에서 분산 효과 소멸, 레버리지의 변동성 손실, 옵션 손익분기점 등 8개 불변식을 검증한다. 백엔드 테스트 11개 → 35개.
- 잘못된 입력은 400이 아니라 명세 범위로 보정하고 실제 적용값을 응답에 돌려준다. 숫자 하나 때문에 실험이 끊기지 않게 했다.
- 모의투자·학습 응답에 교육용 고지와 계산 가정을 항상 포함한다. 특정 상품명·종목명을 쓰지 않고, 자산은 가정한 변동성·기대수익률로만 정의한다.
- `application.yml`에서 `finmate.integrations.opendart.api-key`가 `logging` 블록 아래에 잘못 들어가 있어 `OPENDART_API_KEY` 주입이 동작하지 않던 문제를 고쳤다.
- 설계 근거와 남은 작업은 [11-learning-tree-quiz-simulation.md](11-learning-tree-quiz-simulation.md) 참고.

## 2026-09-09 — 월 1회 진단·근거 스냅샷·상품 학습 고도화

- 사용자 피드백을 반영해 FinMATE 자체 정책으로 한국 시간 기준 월 1회 진단 제출을 구현했다. `ASSESSMENT_MONTHLY_LIMIT`(HTTP 409)와 다음 진단일, 기존 결과·학습 경로를 제공한다.
- 사용자·제출월 유일성 제약과 동시 제출 방어를 추가했다.
- 질문지 해시, 규칙 버전, 답변 근거, 확인 플래그를 진단 시점 스냅샷으로 저장하고 결과 화면에서 조회한다.
- 진단 이력 페이지와 동일 버전 간 답변 변경 비교를 추가했다.
- AI 설명 출력 검증기를 추가해 다른 등급·수익 예측·특정 상품 권유·과도한 길이를 템플릿 폴백한다.
- 상품 위험·채권·ETF·유동성 기초 학습 페이지를 추가했다. 카드 내용은 Investor.gov 공개 안내를 참고한 교육 요약이다.

## 2026-09-10 — 개념별 퀴즈·포트폴리오 실험기·사용자 전략 고도화

- 반복되던 공통 확인 질문을 개념별 75문항과 선택지별 오답 해설로 교체했다. 정답을 인덱스가 아닌 choice ID로 제출하고 콘텐츠 버전을 검증한다.
- 학습 개념 59개 트리에서 상품·메커니즘을 선택하고, 서버가 채점·해설을 반환하는 흐름을 추가했다.
- DB 없이 실행하는 `/practice` 포트폴리오 연습기를 추가했다. 주식·채권·펀드·ETF·금·부동산·리츠·마진·선물·옵션을 조합하고 가격·금리·스프레드·환율·비용 충격을 바꿔 자산별 원인과 의미를 비교한다.
- `GET /practice/products`, `GET /practice/scenarios`, `GET /practice/methodology`, `POST /practice/analyze`, `POST /practice/compare` API와 OAS 경로를 추가했다.
- 사용자 페인포인트·가설·Jobs-to-be-Done·우선순위·검증 계획은 [docs/12-product-strategy-and-user-journeys.md](12-product-strategy-and-user-journeys.md)에 정리했다.
