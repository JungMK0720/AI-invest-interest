# 요구사항 추적

기준일: 2026-10-26. `UR → FR/NFR → 화면·API → 데이터 → 검증 증거`를 연결한다. 코드 존재, 테스트 실행, 배포 검증은 구분한다.

## 기존 연결

기존 테스트명과 관찰 기록은 [기존 UR §6](../reference/16-user-requirements-analysis.md), 상세 구현 상태는 [기존 FR/NFR §10](../reference/13-functional-nonfunctional-usecases.md)에 보존한다. 이전 ✅ 표시는 이번 실행 증거가 아니다.

| UR | FR/NFR | 기존 화면·API·검증 영역 |
|---|---|---|
| UR-01·02 | FR-02~07, NFR-02 | 결과·진단 API, AssessmentStore·NarrativeLearningPathIntegrationTest |
| UR-03~05 | FR-22, NFR-09 | 선택 서술·신호·등급 불변, NarrativeLearningPathIntegrationTest |
| UR-06 | FR-10~14, ADR-018 | learn·practice·glossary·models 공개 경로 |
| UR-07·08 | FR-21·23 | glossary·term-search, GlossaryIntegrationTest·MlParityTest·Python 평가 |
| UR-09 | FR-12·13, NFR-02 | SimulationEngineTest·PracticeEngineTest |
| UR-10·11 | FR-15·24 | me 복습·학습 경로, AuthProgressAdminIntegrationTest |
| UR-12 | FR-25 | 모델 카드·StyleInferenceServiceTest |
| UR-13 | FR-01, NFR-01 | DB 해시 세션·만료·로그아웃 테스트 |
| UR-14 | NFR-11 | 모바일·키보드 과업, 재검증 필요 |
| UR-15~17 | FR-18·26 | admin glossary·learning·overview, 운영 생명주기 테스트 |
| UR-18 | FR-16·17·19 | 데이터 권한·연동·운영 검증 필요 |
| UR-19 | FR-20 | 실계좌·실주문 제외 |

## 새 방향

모두 계획이다. 아직 없는 화면·API·테이블을 구현된 계약처럼 기록하지 않는다.

| UR | FR | NFR | 후속 산출물 | 단계 |
|---|---|---|---|---|
| UR-20 | FR-27 | NFR-14·15 | 증권업 개념·출처·학습 전후 설명 과업 | 1 |
| UR-21 | FR-28 | NFR-15 | 금융기관 비교·오해 확인 문제 | 1 |
| UR-22 | FR-29·30 | NFR-02·04·15 | 선수 개념·손익 표·계산식·경계값 검증 | 1 |
| UR-23 | FR-31~33 | NFR-16·17 | 데이터 이용·공개 시점·세션·누수·재현 검증 | 2 |
| UR-24 | FR-34 | NFR-11 | 차트·표 동등성·키보드 과업 | 2 |
| UR-25 | FR-35 | NFR-18 | 기준선·시간 분할·비용·실패 구간 보고 | 선택 3 |

## 변경과 완료 기록

- 기존 ID를 다른 의미로 재사용하지 않고 변경 이유와 영향 계약·검증을 함께 기록한다.
- 계약은 [기준본 안내](../contracts/README.md)를 따른다.
- 증거에는 커밋·일시·환경·시나리오·명령·기대값·결과·미검증 항목을 기록한다.
- [NFR](nonfunctional-requirements.md)의 미측정 목표와 실제 결과를 구분한다.
