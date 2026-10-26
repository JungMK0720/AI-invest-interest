# 저장소 경계와 디렉터리 정리 이유

기준일: 2026-10-26. 현재는 기존 Git 이력을 유지하는 통합 작업공간이며, 각 디렉터리를 독립 저장소로 내보낼 수 있게 정리했다. 원격 분리·push·실서비스 배포가 완료됐다는 뜻은 아니다.

## 권장 organization

| 저장소 | 소유하는 것 | 분리 이유 |
|---|---|---|
| `front` | React 화면, API 클라이언트, 프런트 CI, Netlify 빌드 | 화면 변경과 정적 배포를 API 릴리스와 독립시킨다. |
| `back` | Spring API, 인증·권한·업무 규칙, JPA, 향후 DB migration, 콘텐츠 시드, `ml/`, Dockerfile, 백엔드 CI | 데이터 변경을 API 버전과 함께 검증한다. 모델·용어·Java 추론 정답 파일도 한 변경으로 관리한다. |
| `infra` | 로컬 통합 compose, Render Blueprint, 환경변수 계약, 운영 절차 | 환경 구성과 애플리케이션 구현의 책임을 구분한다. DB 비밀번호 등 실제 비밀값은 호스팅 Secret 설정에 둔다. |
| `docs` **추가 권장** | 제품 목적, 학습 로드맵, UR/FR/NFR, ADR, OpenAPI·DBML, 운영·검증 기록, 디자인·발표 기록 | 사용자 요구와 화면·API·테스트를 한 곳에서 추적한다. |

`UR`, `FR`, `NFR`을 각각 저장소로 만들지는 않는다. 같은 변경에서 함께 검토하는 문서이므로 `docs/requirements` 안에서 나눈다. `.github` 조직 소개 저장소는 향후 협업 안내가 필요할 때 추가한다. 별도 `database`, `ml`, `data`, `contracts` 저장소는 지금 만들지 않는다.

## 이동 내역

| 이전 | 현재 / 향후 저장소 내부 |
|---|---|
| `frontend/` | `front/` → front 저장소 루트 |
| `backend/` | `back/` → back 저장소 루트 |
| `ml/` | `back/ml/` → back 저장소 `ml/` |
| `backend/docker-compose.yml` | `infra/compose.yaml` |
| `backend/render.yaml` | `infra/render.yaml` (독립 back 저장소를 참조) |
| `docs/00~18`, `docs/changelog.md` | `docs/reference/` (기존 FinMATE 시점 기록) |
| `deliverables/` | `docs/contracts/` |
| `wireframes/`, `usecase.png` | `docs/design/wireframes/`, `docs/design/usecase.png` |
| `presentation/` | `docs/presentation/` |

ML 학습은 백엔드 용어 JSON을 읽고 모델·Java parity 정답 파일을 쓴다. 독립 ML 서비스는 현재 필요하지 않다. 학습 작업 주기·담당자·모델 artifact 계약이 독립할 때 분리한다. DB migration도 별도 DB 저장소보다 `back/src/main/resources/db/migration/`가 적합하다. 이 경로는 향후 Flyway 도입 위치이며 아직 migration은 없다.

## 독립 저장소에서 지킬 계약

- front는 HTTP API만 호출한다. back의 코드나 DB에 직접 의존하지 않는다.
- OpenAPI는 docs의 계약이다. API 변경 PR에서 계약 변경 커밋을 연결하고, 배포 기록에 front/back/docs SHA를 함께 남긴다. 계약 자동 검증은 후속 작업이다.
- DB 변경은 back 릴리스에 포함하고, infra는 연결·권한·백업·실행 순서를 관리한다.
- 각 저장소의 `.github/workflows`는 독립 저장소 루트에서 실행한다. 현재 중첩 폴더 안의 워크플로는 통합 작업공간에서는 자동 실행되지 않는다.
- docs의 조직 코드 링크는 이관 대상 경로다. 원격 저장소·기본 브랜치·접근권한 확인 전에는 링크의 배포 상태를 보장하지 않는다.

원격 분리 절차는 [저장소 이관](../migration/repository-split.md), DB 판단은 [DB 전략](database-strategy.md)을 따른다.
