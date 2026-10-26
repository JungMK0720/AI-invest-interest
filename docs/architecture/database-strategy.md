# DB 전략: Spring API + Supabase PostgreSQL

기준일: 2026-10-26. 채택 제안이며 실제 Supabase 프로젝트 선택·생성·접속·운영 데이터 이관은 수행하지 않았다.

## 추천과 이유

**front → Spring API → Supabase PostgreSQL**로 시작한다. Supabase는 PostgreSQL 호스팅으로 사용하고, 기존 로그인·해시 세션·권한 검사·진단 규칙은 Spring이 소유한다. Supabase Auth·Storage·Realtime는 필요한 요구가 생길 때 각각 검토한다.

| 대안 | 판단 |
|---|---|
| Supabase PostgreSQL + 기존 Spring | 기존 JPA·트랜잭션·테스트를 활용하고 DB 운영 부담을 줄일 수 있어 현재 단계에 적합하다. |
| 브라우저에서 Supabase 직접 조회 + Supabase Auth | 가능한 설계지만 자체 사용자·세션을 이관하고 RLS 권한 모델을 다시 만들어야 한다. 이번 분리와 함께 도입하지 않는다. |
| 직접 운영 PostgreSQL | 제어권은 높지만 패치·백업·복구·접속 관리의 책임이 늘어난다. DB 운영 자체가 학습 목표가 될 때 선택한다. |

DB 연결과 인증 전환을 별도 변경으로 나누는 것은 현재 코드의 결합 관계에 따른 설계 판단이다. Supabase가 Spring 인증을 자동으로 대체하는 것은 아니다.

## 확인한 현재 상태

- JPA 엔터티는 22개다. 최종 DBML의 40개 테이블에는 후속 데이터 플랫폼 설계도 포함되어 있다. DBML 전체를 운영 DDL로 실행하지 않는다.
- `application-prod.yml`은 PostgreSQL과 `ddl-auto:update`를 사용한다. Flyway/Liquibase는 아직 없다.
- `data.sql`의 문항·예시 상품 INSERT는 최초 1회용이다. 재실행 시 충돌할 수 있다. 용어·학습 콘텐츠 seeder는 이미 있는 항목을 보존한다.
- 자체 `users`·`auth_sessions`가 사용자·역할과 해시 세션을 저장한다. Supabase `auth.users`와 별개의 체계다.
- 기존 통합 테스트는 H2 기반이다. H2 성공은 PostgreSQL 호환성과 운영 연결의 증거가 아니다.

## 연결·권한 설계

1. 운영 앱은 장시간 실행하는 Spring 컨테이너다. 직접 연결을 우선 검토하고, 배포망이 IPv4만 지원하면 **session pooler 5432**를 쓴다. 호스트·사용자는 Supabase Connect 화면에서 복사한다. transaction pooler 6543은 이번 기본값으로 사용하지 않는다. [공식 연결 문서](https://supabase.com/docs/guides/database/connecting-to-postgres)
2. Hikari pool은 인스턴스당 최대 5개를 초기 제안값으로 두고 부하테스트에서 조정한다. 전체 연결 수는 API 인스턴스 수와 관리·migration 연결까지 합산한다.
3. 외부 DB 연결은 TLS를 요구한다. 최초 예시는 `sslmode=require`, 운영 구성에서는 서버 CA·호스트 검증을 포함하는 `verify-full`을 적용하고 연결을 확인한다. [공식 SSL 문서](https://supabase.com/docs/guides/platform/ssl-enforcement)
4. 앱 전용 `finmate` 스키마를 사용하고 Data API의 노출 스키마에 추가하지 않는다. Data API가 필요 없다면 비활성화한다. `public`에 앱 테이블을 만든다면 노출·GRANT·RLS를 별도로 점검한다. [Data API 보안](https://supabase.com/docs/guides/api/securing-your-api)
5. migration 소유자와 runtime 역할을 나눈다. runtime에는 필요한 스키마 사용·테이블 DML·시퀀스 권한만 준다. `postgres` 관리자 접속은 초기 준비에 한정한다. 현재 코드는 startup seeder가 있어 runtime 쓰기 권한이 필요하다.
6. 브라우저에는 DB 비밀번호·secret/service_role 키를 전달하지 않는다. 자체 Bearer 토큰은 Supabase JWT가 아니므로 `auth.uid()` 정책을 붙이는 것만으로 현재 사용자 권한이 연결되지 않는다. Spring의 소유권 검사가 경계다.

전용 스키마는 노출을 줄이는 설계이며 DB 역할·노출 설정을 실제로 확인해야 한다. 별도 Supabase CLI migration 이력을 동시에 운영하지 않고, 앱 스키마 변경은 back의 Flyway를 단일 기준으로 삼는 것을 권장한다.

## 실제 배포 전 순서

1. 테스트 PostgreSQL에서 실제 엔터티 기준 초기 DDL을 만들고 검토한다. Flyway baseline을 back에 추가한다. 아직 구현하지 않았다.
2. 신규 DB migration → 1회용 문항·상품 시드 → 용어·학습 seeder → `ddl-auto:validate` 기동을 검증한다. 운영에서 자동 `update`를 실행하지 않는다.
3. `prod,supabase` 프로필은 준비된 스키마를 검증하는 opt-in 구성이다. 빈 DB를 자동 생성해 주는 기능이 아니며 migration 준비 전에는 기동 실패가 예상된다.
4. staging에서 가입·로그인·진단·콘텐츠 게시·권한·재시작 후 이력 유지·백업 복구를 검증한다. 그다음 운영 front를 연결한다.
5. 소규모 부하테스트로 API 지연·SQL·연결 대기와 오류율을 확인하고, 릴리스에 API/DB migration 버전을 기록한다.

이미 영구 DB에 데이터가 있으면 새 스키마에 덮어쓰지 않고 백업·행 수·PK/FK·시퀀스·권한 비교 후 이관한다. Supabase 요금·프로젝트 지역·백업 보존 조건은 실제 프로젝트 선택 시 확인한다.
