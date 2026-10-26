# API·데이터 계약

기존 FinMATE 산출물의 파일명과 내용을 보존했다. organization 분리 후 API·DB와 일치하는지 별도 검증한다.

| 파일 | 용도 | 해석 |
|---|---|---|
| [FinMATE-API.yml](FinMATE-API.yml) | 전체 OpenAPI | 기존 기능 계약 원천 |
| [FinMATE-DataModel-Final.dbml](FinMATE-DataModel-Final.dbml) | 확장 모델 | 구현·후속 설계 혼합. 실행 migration이 아님 |
| [FinMATE-DB.dbml](FinMATE-DB.dbml) | MVP 모델 | 역사 기록 |
| [FinMATE-DataPlatform.dbml](FinMATE-DataPlatform.dbml) | 데이터 플랫폼 | 설계 참고 |
| [FinMATE-발표자료.pptx](FinMATE-발표자료.pptx) | 기존 제출 파일 | 내용 수정·검증 없이 보존 |

`presentation/`의 API·DB는 발표용 사본이다. 전체 계약을 먼저 갱신하고 필요할 때 발표본을 맞춘다. 운영 DB 변경은 `back`의 버전 migration이 소유하며 `infra`는 연결·배포·복원을 담당한다. DBML을 운영 DB로 그대로 옮기거나 대시보드에서만 스키마를 바꾸지 않는다.

[FR-27~35](../requirements/functional-requirements.md)의 새 학습·리플레이·선택 예측은 아직 이 계약에 구현 명세로 추가하지 않았다.
