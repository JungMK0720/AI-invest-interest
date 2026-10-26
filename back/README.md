# FinMATE API · oh-my-finance/back

금융·증권 학습 서비스의 Spring Boot API입니다. 기존 Java 패키지 `com.finmate`와 API 계약은 유지하고 organization 저장소의 경계부터 분리합니다.

## 책임과 구조

```text
back/
├── src/main/java/com/finmate/   인증, 진단, 학습, 용어, 운영, 모델 추론
├── src/main/resources/         실행 프로필, 콘텐츠 시드, 추론 모델
├── src/test/                   API·도메인·Python/Java 일치 검증
├── ml/                         Python 학습·평가·모델 내보내기
├── Dockerfile                  이 저장소 루트를 context로 빌드
├── pom.xml
└── .env.example                환경변수 예시
```

엔터티·런타임 콘텐츠·추론 모델은 API 버전과 함께 관리합니다. ML은 API의 용어 원천과 Java 정답 파일에 의존하므로 지금은 이 저장소 안에 둡니다. 별도 ML 서버는 없습니다. DB 마이그레이션도 향후 이 저장소가 소유합니다.

## 로컬 실행과 검증

Java 21, Maven 3.9+, Python 3.10+가 필요합니다. 이 디렉터리에서 실행합니다.

```sh
mvn spring-boot:run
mvn -B test
python -m unittest discover -s ml -p 'test_*.py'
```

API는 `http://localhost:8080/api`입니다. 기본 프로필은 메모리 H2이며 재시작하면 사용자 기록이 초기화됩니다. 문항·예시 상품 SQL과 용어·학습 콘텐츠가 로컬 기동 시 적재됩니다.

`.env`는 Spring Boot가 자동으로 읽지 않습니다. 루트 `make dev`는 `.env`를 읽지만, 단독 실행할 때는 필요한 값을 프로세스 환경변수나 호스팅 서비스의 환경변수로 설정해야 합니다. 관리자 계정은 `FINMATE_ADMIN_EMAIL`과 12자 이상인 `FINMATE_ADMIN_PASSWORD`가 모두 있을 때 생성됩니다. `ANTHROPIC_API_KEY`가 없으면 설명은 템플릿으로 동작합니다.

모델 재학습은 `python ml/train.py`로 실행합니다. 로컬 PostgreSQL 통합 실행은 [infra 실행 문서](https://github.com/oh-my-finance/infra#readme)를 따릅니다.

## 프로필과 DB 경계

| 프로필 | 용도 | 스키마 변경 |
|---|---|---|
| 기본 | 로컬·기존 H2 테스트 | `create-drop` |
| `prod` | 기존 PostgreSQL 로컬 통합 경로 | `update` — 운영 전환 전의 임시 동작 |
| `prod,supabase` | Supabase 운영용 opt-in 설정 | `validate` — 이미 준비된 스키마만 검증 |

**Supabase 운영 배포는 아직 준비 단계입니다.** 현재 마이그레이션 도구와 검증된 운영 DDL이 없습니다. `prod,supabase`를 새 DB에 바로 적용하면 테이블 검증에서 기동이 실패합니다. 실제 엔터티 22개 기준의 마이그레이션·시드, `finmate` 스키마와 전용 DB 역할을 준비한 뒤 PostgreSQL 검증을 통과시켜야 합니다. 설계 DBML의 전체 40개 테이블을 일괄 생성하는 방식은 사용하지 않습니다.

브라우저는 Spring API만 호출하고, Spring이 JDBC로 Supabase PostgreSQL에 접근합니다. 현재 자체 `users`와 해시 `auth_sessions`를 계속 사용합니다. Supabase Auth 및 브라우저용 Supabase SDK는 도입하지 않습니다. Supabase Data API는 사용하지 않으므로 비활성화하고, `finmate` 스키마를 노출 스키마에 추가하지 않습니다.

Supabase 설정은 `.env.example`의 주석 템플릿을 참고해 호스트에 등록합니다. 프로필 순서는 `prod,supabase`로 설정합니다. 필수 값은 JDBC URL·DB 사용자·비밀번호와 정확한 `FRONTEND_ORIGIN`입니다. 기본 DB 스키마는 `finmate`, Hikari 최대 연결 수는 인스턴스당 5입니다. 인스턴스를 늘릴 때 총 연결 예산을 함께 계산합니다.

지속 실행 서버는 IPv6가 가능하면 direct 연결을, IPv4 환경이면 Dashboard **Connect → Session pooler의 5432** 연결을 사용합니다. 호스트와 사용자명은 화면에서 그대로 복사하며 지역명으로 추측하지 않습니다. transaction pooler 6543은 이 구성의 기본값이 아닙니다. JDBC URL에는 `sslmode=require`를 넣고 프로필에도 SSL 기본값을 설정했습니다. 실제 운영에서는 호스트에 CA 인증서를 배치하고 URL의 `sslmode=verify-full&sslrootcert=...`로 인증서와 호스트를 검증합니다. URL에 SSL 비활성화 옵션을 넣으면 안 됩니다. [Supabase 공식 연결 안내](https://supabase.com/docs/guides/database/connecting-to-postgres)

## 컨테이너와 배포

이 저장소 루트에서 `docker build -t finmate-back .`로 빌드할 수 있습니다. Dockerfile은 `pom.xml`과 `src/`만 이미지에 복사하며, 모델 JSON은 이미 `src/main/resources/ml/`에 포함됩니다. 컨테이너의 기본 프로필은 기존 `prod`이므로 운영에서는 반드시 `SPRING_PROFILES_ACTIVE=prod,supabase`를 명시합니다.

Render 설정의 원본은 [infra/render.yaml](https://github.com/oh-my-finance/infra/blob/main/render.yaml)입니다. DB 마이그레이션과 접근 권한 검증 전에는 배포하지 않습니다. 운영 DB 비밀번호·관리자 비밀번호·LLM API 키는 Secret으로 관리합니다. 프런트 빌드 변수에 DB 자격증명을 넣지 않습니다.

상세 제품 요구사항과 DB 전환 계획은 [docs 저장소](https://github.com/oh-my-finance/docs#readme)에서 관리합니다. 위 organization 링크는 저장소 분리 후의 목적지이며 현재 원격 반영 여부와는 구분합니다.
