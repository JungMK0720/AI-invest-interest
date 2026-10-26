# Oh My Finance 문서

제품 목표는 **증권업을 이해하고 상품 구조를 배우며 과거 사례에서 판단 근거를 연습하는 것**이다. FinMATE의 코드를 이어받아 `front`·`back`·`infra`와 함께 관리한다. 공유 문서 저장소는 `docs` 하나를 추가하며 UR·FR·NFR은 그 안에서 분류한다.

기준일: 2026-10-26. 이 디렉터리는 향후 `oh-my-finance/docs` 루트로 내보낼 구조다. 원격 생성·push·배포는 로컬 정리와 별도다.

## 현재 기준

| 문서 | 내용 |
|---|---|
| [제품 비전](product/vision.md) | 증권업 학습 목적·범위·공식 출처 |
| [학습 로드맵](product/learning-roadmap.md) | 입문→파생상품→과거 차트·표→선택 예측 |
| [사용자 요구 UR](requirements/user-requirements.md) | 기존 UR-01~19, 계획 UR-20~25 |
| [기능 요구 FR](requirements/functional-requirements.md) | 기존 FR-01~26, 계획 FR-27~35 |
| [품질 요구 NFR](requirements/nonfunctional-requirements.md) | 기존 NFR-01~14, 계획 NFR-15~18, 미측정 부하 목표 |
| [추적](requirements/traceability.md) | 요구→동작·계약→검증 |
| [저장소 경계](architecture/repository-boundaries.md) | 나눈 이유와 소유권 |
| [DB 전략](architecture/database-strategy.md) | Supabase PostgreSQL·API 경계·migration |
| [분리 절차](migration/repository-split.md) | organization 이동·검증 |
| [부하 테스트](operations/load-testing.md) | 측정 시나리오·단계별 목표 |
| [검증 기록](operations/verification.md) | 실제 실행 결과와 남은 실패·미검증 범위 |
| [계약 안내](contracts/README.md) | OpenAPI·DBML 기준본과 보존본 |

## 디렉터리

```text
docs/
├── product/                  현재 비전·학습 순서
├── requirements/             UR·FR·NFR·추적
├── architecture/             저장소·DB 경계와 결정 이유
├── operations/               배포·운영·검증 절차
├── migration/                organization 분리 절차
├── contracts/                OpenAPI·DBML·기존 제출 파일
├── design/wireframes/        canvas.json과 HTML 함께 보존
├── presentation/             발표용 사본·시각 자료·SOP
└── reference/                기존 00~18·ADR·changelog
```

공유 제품·요구·계약·구조·운영 원칙은 여기에 둔다. 프런트 실행법은 `front/README.md`, API·migration·모델 학습은 `back/README.md`, 배포 설정과 환경변수 예시는 `infra/README.md`가 소유한다.

## 기존 FinMATE 기록

| 기록 | 용도 |
|---|---|
| [00 개요](reference/00-project-brief.md)·[01 MVP](reference/01-requirements.md)·[02 흐름](reference/02-user-flow.md) | 초기 범위 |
| [03 데이터](reference/03-data-model.md)·[04 API](reference/04-api-design.md) | 초기·확장 설계 |
| [05 ADR](reference/05-decisions.md) | ADR-001~019. 009 메모리 세션은 017로 대체 |
| [06 인계](reference/06-mvp-review-and-handoff.md)·[07 검토](reference/07-usecase-review.md)·[08 감사](reference/08-supervisor-review-and-plan.md) | 과거 검토 기록 |
| [09 정책](reference/09-product-learning-and-reassessment.md)·[10 데이터](reference/10-data-platform-and-learning-expansion.md)·[11 학습](reference/11-learning-tree-quiz-simulation.md) | 기존 확장 방향 |
| [12 전략](reference/12-product-strategy-and-user-journeys.md)·[13 FR/NFR](reference/13-functional-nonfunctional-usecases.md)·[15 제출본](reference/15-requirements-problem-and-datamodel.md) | 기존 요구 원천 |
| [14 AI 연결](reference/14-ai-service-integration.md)·[18 거버넌스](reference/18-ai-models-and-data-governance.md) | AI 역할·평가·한계 |
| [16 UR](reference/16-user-requirements-analysis.md)·[17 시나리오](reference/17-user-scenarios.md)·[이력](reference/changelog.md) | 기존 추적·관찰 |

현재 기준은 `product/`와 `requirements/`다. `reference/`의 완료 표시는 과거 기록이며 이번 정리에서 새로 검증한 결과가 아니다. 기존 문서의 코드 링크는 organization 분리 목적지 `oh-my-finance/front`·`back`의 `main` 경로로 바꿨으며 원격 반영은 push 후 확인한다. 역사 기록의 인라인 경로·명령은 현재 실행법으로 그대로 사용하지 않는다.
