# FinMATE Mini-project 기획 문서

AI 캠퍼스 "AI 웹 서비스 설계 Mini-project" 과제용 기획 문서 세트. `docs/` 안의 문서를 기준으로 세션(대화)을 이어간다.

## 문서 구조

```
docs/
├── 00-project-brief.md   # 확정: 서비스 개요, 성공 기준
├── 01-requirements.md    # 확정: 액터, 기능 요구사항(FR), 제외 범위
├── 02-user-flow.md       # 확정: 화면 목록, 사용자 흐름, 화면-API 매핑
├── 03-data-model.md      # 확정: ERD 엔터티/관계, DBML 초안
├── 04-api-design.md      # 확정: API 목록, OAS 요청/응답 초안
├── 05-decisions.md       # ADR: 왜 이렇게 결정했는지
├── 06-mvp-review-and-handoff.md # 1차 완료 판정, 다음 세션 인계
├── 07-usecase-review.md   # 유스케이스 검토 및 추적
├── 08-supervisor-review-and-plan.md # 감시자 감사 및 고도화 기준
├── 09-product-learning-and-reassessment.md # 월 1회 진단 정책, 결과 재현성
├── 10-data-platform-and-learning-expansion.md # 실데이터 수집·학습 플랫폼 설계
├── 11-learning-tree-quiz-simulation.md # 개념 트리·확인 문제·모의투자 설계
├── 12-product-strategy-and-user-journeys.md # 문제·사용자·제품 전략과 학습 여정
├── 13-functional-nonfunctional-usecases.md # 기능/비기능 요구사항·Use Case·추적표
└── changelog.md          # 요구사항/설계 변경 이력 + backlog
```

## 세션(대화) 운영 방법

1. **기준 문서 vs 변경 문서를 구분한다.**
   - 00~04: 1차 Agile 기준 문서 (자주 안 바뀜)
   - 05, changelog: 왜 바뀌었는지 (수시로 추가)
   - 06: 현재 세션의 완료 판정과 다음 작업 순서
2. **작업을 요청할 때는 어떤 문서를 기준으로 할지 명시한다.**
   - 예: "`docs/01-requirements.md`를 기준으로 `docs/02-user-flow.md`의 관리자 흐름을 보완해줘."
   - 예: "현재 문서 기준으로 UI에는 있는데 API 명세에 없는 기능을 찾아줘."
3. **요구사항이 바뀌면 해당 문서를 직접 수정하고, `changelog.md`에 날짜와 함께 한 줄로 남긴다.** 결정의 이유가 있으면 `05-decisions.md`에 ADR로 추가한다.
4. **문서를 요약/압축할 때도 다음은 절대 생략하지 않는다.**
   - 서비스 목적, 타깃 사용자
   - 필수(P0) 기능 목록과 ID
   - 제외 범위
   - 화면 ↔ API ↔ 데이터 매핑
   - 설계 결정의 이유

## 진행 순서 (3일 가이드라인 기준)

| 일차 | 산출물 | 관련 문서 |
|---|---|---|
| 1일차 | 서비스 개요, 요구사항, UI 흐름도(와이어프레임) | 00, 01, 02 |
| 2일차 | 데이터 모델(ERD), API 명세(OAS) | 03, 04 |
| 3일차 | 기획/설계 보완, 산출물 3종 최종 정리, 발표(5분) | 전체 + 05 |

## 최종 제출 산출물 (반드시 3종)

| 산출물 | 파일명 | 도구 |
|---|---|---|
| 프로젝트 기술서 (PDF) | `{반}_{이름}_FinMATE-개요.pdf` | 00/01/02 문서 + Balsamiq/PPT 와이어프레임 캡처 |
| API 명세서 (YML) | `{반}_{이름}_FinMATE-API.yml` | Swagger Editor / OAS Editor, 04 문서 기반 |
| DB 다이어그램 (DBML) | `{반}_{이름}_FinMATE-DB.dbml` | dbdiagram.io, 03 문서 기반 |

> UI 흐름도(화면 캡처)는 반드시 PDF 안에 포함되어야 함 (평가 기준 필수 사항).

발표용 API 자료는 `presentation/FinMATE-발표용-API.yml`(상세 OAS)과 `presentation/FinMATE-API-발표요약.md`(슬라이드용 요약)으로 분리해 관리한다.
캡처·다이어그램·슬라이드 연결 절차는 `presentation/FinMATE-발표-시각화-SOP.md`를 따른다.
Enterprise Architect에서 직접 모델을 작성할 때는 `presentation/FinMATE-Enterprise-Architect-UseCase-SOP.md`를 따른다.
플로우차트와 와이어프레임 부록은 `presentation/FinMATE-플로우차트-와이어프레임.pptx`와 `presentation/FinMATE-플로우차트-와이어프레임-명세.md`에 있다.
