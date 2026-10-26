# oh-my-finance · FinMATE

**증권업을 이해하기 위한 학습 프로젝트.** 증권사의 역할과 금융상품의 구조를 배우고, 과거 차트로 판단을 복기한 다음, 작은 모델 실험과 서비스 성능 검증으로 확장합니다. 제품 이름과 코드의 `FinMATE/com.finmate`는 우선 유지합니다.

## 학습 방향

| 단계 | 제공할 경험 | 이번 상태 |
|---|---|---|
| 1. 증권업·상품 개념 | 증권업이란/왜 증권업인가, 1·2금융권 개요, 주식·채권에서 선물·옵션·증거금·파생상품까지의 개념 로드맵 | 기존 개념·용어·퀴즈·시뮬레이션을 활용할 수 있음. 증권업 입문 과정과 순서 재구성은 계획 |
| 2. 차트 판단·복기 | 과거 시점의 정보만 보고 판단 → 다음 구간 공개 → 결과와 위험을 복기. 차트와 표를 함께 제공 | 계획, 데이터 이용 조건과 시점 일관성부터 확인 |
| 3. 경량 모델 실험 | 단순 기준선과 작은 모델을 시간 순서로 평가하고 비용·누수·한계를 기록 | 선택 확장. 첫 배포의 필수 조건 아님 |
| 공통. 운영·성능 | PostgreSQL 지속성, 배포·복구, 단계별 부하테스트와 개선 전후 측정 | 설정·검증 계획 정리. 운영 연결과 부하 측정은 미실시 |

[제품 목적](docs/product/vision.md) · [단계별 로드맵](docs/product/learning-roadmap.md) · [UR](docs/requirements/user-requirements.md) · [FR](docs/requirements/functional-requirements.md) · [NFR](docs/requirements/nonfunctional-requirements.md)

현재 구현은 투자 성향 진단·근거 스냅샷, 용어 사전·질문형 검색, 개념·퀴즈·모의실험, 학습 기록·복습·콘텐츠 운영입니다. 기존 텍스트 모델 2개는 성향 보조 신호와 용어 검색용이며 **주가 예측 모델이 아닙니다**. 새 로드맵의 기능 구현 완료를 뜻하지 않습니다.

## Organization과 디렉터리

```text
AI-invest-interest/                  기존 Git 이력을 유지한 이관 작업공간
├── front/                           → oh-my-finance/front
│   ├── src/, public/                React + Vite
│   ├── netlify.toml                 독립 front 루트에서 빌드
│   └── .github/workflows/           독립 front CI
├── back/                            → oh-my-finance/back
│   ├── src/                         Spring Boot + Java 21
│   ├── ml/                          학습·평가 → 런타임 모델·Java 정답
│   ├── Dockerfile, pom.xml
│   └── .github/workflows/           독립 back CI
├── infra/                           → oh-my-finance/infra
│   ├── compose.yaml                 로컬 PostgreSQL + API 통합
│   └── render.yaml                  독립 back 저장소를 배포하는 Blueprint
├── docs/                            → oh-my-finance/docs (추가 권장)
│   ├── product/, requirements/      목적·로드맵·UR/FR/NFR·추적성
│   ├── architecture/, operations/   저장소/DB 전략·부하·검증
│   ├── contracts/                   OpenAPI·DBML 및 기존 제출 산출물
│   ├── design/, presentation/       와이어프레임·발표 기록
│   ├── reference/                   기존 00~18 문서와 변경 이력
│   └── migration/                   organization 이관 절차
└── tools/                           로컬 저장소 내보내기 도구
```

front는 화면, back은 API·인증·데이터 변경, infra는 환경 운영, docs는 요구·설계 계약을 소유합니다. UR/FR/NFR은 docs 안에서 나누고, DB migration과 ML은 back 버전과 함께 관리합니다. [왜 이렇게 나눴는가](docs/architecture/repository-boundaries.md)

로컬 정리는 원격 분리와 별개입니다. 기존 `origin`은 유지했고, 원격 저장소 생성·push·클라우드 배포는 수행하지 않았습니다. 현재 인증으로 organization 저장소의 이름·권한·기본 브랜치를 확인하지 못했습니다.

## DB 추천

**브라우저 → Spring API → Supabase PostgreSQL**을 권장합니다. 기존 DB 해시 세션과 권한 검사는 Spring에 유지하고, Supabase Auth 도입은 별도 변경으로 검토합니다. 앱 전용 스키마와 runtime DB 역할을 준비하고, 브라우저가 DB에 직접 접근하지 않게 구성합니다.

현재 운영 프로필은 `ddl-auto:update`이며 migration은 없습니다. 추가한 `prod,supabase` 프로필은 준비된 스키마를 `validate`하므로, 초기 migration·시드·PostgreSQL 검증을 먼저 완료해야 합니다. [DB 전략과 배포 전 조건](docs/architecture/database-strategy.md)

## 개발과 검증

준비: Java 21, Maven 3.9+, Node.js(`front/.nvmrc`), Python 3.10+. H2 개발 모드는 재시작 시 개인 기록이 초기화됩니다.

PowerShell에서는 서로 다른 터미널에서 실행합니다.

```powershell
# API: 작업공간 루트에서
Set-Location back
mvn spring-boot:run
```

```powershell
# 화면: 작업공간 루트에서
Set-Location front
npm ci
Copy-Item .env.example .env.local
npm run dev
```

화면 `http://localhost:5173` · API `http://localhost:8080/api`. 관리자 초기 계정과 선택형 LLM 키는 [back 실행 문서](back/README.md)의 환경변수를 따릅니다. Spring은 `.env` 파일을 자동 로드하지 않습니다.

```powershell
# 작업공간 루트
python -m unittest discover -s back/ml -p 'test_*.py'
python -m unittest discover -s tools -p 'test_*.py'
# back에서: mvn -B test
# front에서: npm run lint / npm run build
```

POSIX shell에서는 `make dev`, `make test`, `make model`을 사용할 수 있습니다. 로컬 PostgreSQL 컨테이너 구성은 [infra/README.md](infra/README.md)를 따릅니다. 테스트 결과와 환경 제한은 [검증 기록](docs/operations/verification.md)에 있습니다.

## 저장소 분리와 배포

```powershell
python tools/export_repositories.py --dry-run
python tools/export_repositories.py
```

`.build/repositories/`에 네 저장소의 파일을 준비합니다. 과거 Git 이력은 복사하지 않으며 원본 이력은 유지합니다. 같은 출력에 덮어쓰지 않습니다. [이관·배포 순서](docs/migration/repository-split.md)

권장 초기 배포는 Netlify(front) + Render(Spring 컨테이너) + Supabase(PostgreSQL)입니다. 기존 배포 설정을 활용한 제안이며 요금·지역·성능 조건은 프로젝트 선택 시 확인해야 합니다. 실제 배포는 DB migration·staging 검증 후 진행합니다.

[전체 문서 지도](docs/README.md) · [기존 API 계약](docs/contracts/FinMATE-API.yml) · [데이터 모델 주의점](docs/contracts/README.md) · [부하테스트 계획](docs/operations/load-testing.md)

교육용 프로젝트이며 매매 주문·투자 권유 서비스는 범위에 포함하지 않습니다.