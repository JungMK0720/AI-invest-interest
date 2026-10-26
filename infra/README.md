# oh-my-finance/infra

여러 저장소를 연결하는 실행·배포 설정을 관리합니다. API 코드·DB 엔터티·모델 학습은 [back](https://github.com/oh-my-finance/back), 웹 코드는 [front](https://github.com/oh-my-finance/front), UR/FR/NFR와 ADR은 [docs](https://github.com/oh-my-finance/docs)에 둡니다. organization 링크는 분리 후 목적지이며 실제 원격 반영 완료를 뜻하지 않습니다.

## 파일과 소유권

| 파일 | 용도 |
|---|---|
| `compose.yaml` | 로컬 PostgreSQL + Spring API 통합 실행 |
| `render.yaml` | 독립 `oh-my-finance/back` 저장소를 빌드하는 운영 배포 템플릿 |
| `.env.example` | 로컬 Compose 입력 예시 |

DB migration SQL은 애플리케이션 버전과 함께 바뀌므로 back이 소유합니다. infra는 연결 환경변수·배포 대상·향후 백업/복구 절차를 소유합니다. 실제 비밀번호·API 키·인증서·DB 덤프는 이 저장소에 넣지 않습니다.

## 로컬 통합 실행

현재 작업공간과 저장소 분리 후 모두 다음 상대 경로를 유지합니다. 독립 저장소로 받은 경우 `back`과 `infra`를 같은 상위 디렉터리에 clone합니다.

```text
workspace/
├── back/    pom.xml, Dockerfile, src/, ml/
├── front/
├── infra/   compose.yaml, render.yaml
└── docs/
```

Docker Desktop 또는 Docker Engine + Compose v2가 필요합니다. `infra` 디렉터리에서 실행합니다.

```sh
cp .env.example .env
docker compose config --quiet
docker compose up --build
```

PowerShell에서는 첫 줄 대신 `Copy-Item .env.example .env`를 사용합니다. `.env`에서 로컬 포트, 허용할 프런트 주소, 선택적 관리자·LLM 변수를 설정할 수 있습니다. 서비스는 기본적으로 `127.0.0.1:8080`과 `127.0.0.1:5432`에만 공개됩니다. 프런트 개발 서버는 front 저장소에서 별도로 실행합니다.

Compose는 기존 `prod` 프로필의 Hibernate `update` 동작을 보존한 **로컬 통합 환경**입니다. 운영 Supabase 연결 설정과 다릅니다. `back` Dockerfile은 빌드 의존성 다운로드에 네트워크가 필요합니다.

첫 기동 후 문항·예시 상품은 새 로컬 DB에 한 번 적재합니다. Bash에서는:

```sh
docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U finmate -d finmate < ../back/src/main/resources/data.sql
```

PowerShell 7에서는:

```powershell
Get-Content -Raw -Encoding utf8 ../back/src/main/resources/data.sql | docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U finmate -d finmate
```

일반 INSERT이므로 같은 DB에 반복 실행하지 않습니다. 용어·학습 콘텐츠는 앱이 없는 항목만 자동 적재하며 관리자가 수정한 콘텐츠는 보존합니다. `docker compose down`은 컨테이너를 중지하고 명명된 DB 볼륨은 유지합니다.

## Supabase + Render 전환

`render.yaml`은 **독립 infra 저장소 루트**에 놓일 설정입니다. Blueprint가 있는 infra와 빌드할 back이 다르므로 `repo: https://github.com/oh-my-finance/back`을 명시했습니다. Docker context와 Dockerfile은 이 back 저장소 루트 기준입니다. 현재 통합 작업공간 전체를 back 저장소로 올리는 경우에는 맞지 않는 경로입니다. [Render 공식 Blueprint 필드 설명](https://render.com/docs/blueprint-spec)

자동 재배포는 꺼 두었습니다. Blueprint를 처음 생성하는 행위 자체는 초기 배포를 시도하므로 아래 준비 완료 후 연결합니다. `plan: free`는 템플릿 값이며 실행 전 실제 제공 조건과 가용성을 확인합니다.

1. front/back/infra/docs 분리를 반영하고 back CI가 통과했는지 확인합니다.
2. Supabase 프로젝트·지역·요금제를 정하고 개발/운영 DB를 구분합니다. 이 작업에서는 실제 프로젝트를 생성하지 않았습니다.
3. **배포 차단 항목:** back의 실제 엔터티 22개에 맞는 마이그레이션과 초기 시드, 전용 `finmate` 스키마·최소 권한 런타임 역할을 준비합니다. 현재 Flyway/운영 DDL은 없습니다. Data API는 비활성화하고 이 스키마를 노출하지 않습니다.
4. PostgreSQL에서 스키마·시드와 가입/로그인·진단·관리자 게시·재시작 지속성을 검증합니다. 현재 H2 테스트만으로 운영 DB 호환성을 확정하지 않습니다.
5. Render Secret에 `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `FRONTEND_ORIGIN`을 넣습니다. 관리자·LLM 변수는 사용할 때 설정합니다. `FRONTEND_ORIGIN`은 `/api` 없는 프런트 origin이며 프런트 `VITE_API_BASE_URL`은 백엔드 URL의 `/api`까지 포함합니다.
6. 준비된 DB에 `prod,supabase`로 배포하고 `/api/product-categories`, `/api/questions`, 로그인과 진단 저장을 확인합니다. 이 프로필은 `ddl-auto: validate`이므로 미준비 DB에 스키마를 자동 생성하지 않습니다.

Spring API는 direct 연결 또는 IPv4용 session pooler 5432를 사용하고, 인스턴스당 Hikari 최대 5개로 시작합니다. SSL 암호화를 기본으로 하며 운영에서는 CA 인증서와 `verify-full`을 권장합니다. 상세 자격증명 예시는 back의 `.env.example`에 있습니다. [Supabase 공식 PostgreSQL 연결 안내](https://supabase.com/docs/guides/database/connecting-to-postgres)

기존 자체 인증을 유지하므로 Supabase Auth, 브라우저 Data API, Storage를 동시에 도입하지 않습니다. 금융 데이터 공급자·시세 수집·예측 실험은 후속 범위이며 이번 설정에 별도 서비스나 DB를 추가하지 않았습니다.
