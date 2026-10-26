# 디렉터리 분리 검증 기록

검증일: 2026-10-26. 원본 HEAD: `2594646`. 검증은 로컬 구조와 기존 기능의 회귀 확인이며, GitHub push·실서비스 배포·Supabase 접속 검증은 포함하지 않는다.

| 검사 | 결과 | 환경·의미 |
|---|---|---|
| front `npm ci` | 통과 | Node 24.15.0, npm 11.12.1, 기존 lockfile 사용 |
| front `npm run lint` | 종료 코드 0 | 기존 React effect/Fast Refresh 경고가 남아 있다. 경고 0건을 뜻하지 않는다. |
| front `npm run build` | 통과 | TypeScript 검사 + Vite 프로덕션 번들 |
| Python ML 회귀 | **12개 통과** | Python 3.13.6. 기존 11개 + CRLF/LF 원천 해시 회귀 1개 |
| back `mvn -B test` | **70개 중 69개 통과, 1개 실패** | 설치된 JDK 24.0.2로 release 21 컴파일. 목표 runtime/CI는 Java 21 |
| 이동 전 원본 `MlParityTest` 재현 | 동일 실패 | 원본 HEAD의 backend만 별도 `.build/baseline`에 추출, 동일 환경에서 3개 중 1개 실패 |
| compose 구성 파싱 | 통과 | `docker compose -f infra/compose.yaml config --quiet`. Docker daemon은 사용 불가하여 실제 컨테이너 기동 미실시 |

## 발견한 문제와 범위

Windows Git `core.autocrlf=true` 체크아웃에서는 용어 JSON의 CRLF 때문에 Linux에서 만든 모델의 원천 해시가 달랐다. `back/ml/train.py`가 줄바꿈을 LF로 정규화한 뒤 해시하도록 수정하고, 내용 변경은 계속 감지하는 회귀 검증을 추가했다. 모델 파라미터나 학습 데이터는 바꾸지 않았다.

남은 Java 실패는 `MlParityTest.용어_검색_순위와_점수가_Python과_일치한다`에서 질문 “감사의견 거절이면 상장폐지될 수 있나요?”의 검색 순위가 Python 정답 파일과 다른 것이다. 기대 `capital-impairment`, 실제 `stock-index`다. 변경 전 원본에서도 동일해 디렉터리 이동으로 새로 생긴 실패는 아니다. 원인과 수정은 아직 확정하지 않았으며 전체 Java 검증 통과로 표시하지 않는다. 로그는 로컬 `.build/baseline/parity.log`와 `back/target/surefire-reports/`에 있다.

Supabase `prod,supabase` 프로필은 운영 자격증명과 준비된 스키마가 필요하므로 실제 DB로 기동하지 않았다. Flyway 초기 migration·권한 설정·시드·재시작 지속성·백업 복구·부하테스트는 후속 배포 작업이다. 실제 Java 21 환경에서 CI 결과를 확인해야 한다.

## 구조·내보내기 검사

- 이동 대상 원본 파일 307개를 새 경로와 대조해 누락 0개를 확인했다. 기존 프런트 `.env.development/.env.production`은 공개 예시 `.env.example`로 정리했다.
- Markdown 43개의 로컬 파일 링크를 검사해 깨진 링크 0개를 확인했다. 원격 organization 링크 접근은 별도다.
- 저장소 내보내기 도구 테스트 3개 중 2개 통과, 1개는 Windows의 심볼릭 링크 생성 권한 부족으로 건너뛰었다. 기존 출력·워크스페이스 외부·소스 내부(경로 우회 포함) 출력 거부, 비밀 환경파일·키·캐시 제외, 한글 파일명과 새 파일 포함을 검증했다.
- 내보내기 dry-run은 front 49 / back 209 / infra 5 / docs 69개를 확인했다. 최종 사본은 `.build/repositories-final/`에 준비했다. manifest의 332개 파일이 원본 작업트리와 바이트 단위로 일치하고 독립 저장소 루트·CI 경계를 확인했다. 사본 안에서도 ML 12개 테스트가 통과했다. 앞서 만든 `.build/repositories/`는 문서 완료 전 스냅샷이다.
- `git diff --check`는 통과했다. 저장소별 CI는 독립 저장소로 옮긴 뒤 실행되며, 알려진 Java 실패 해결 전에는 전체 back CI 통과를 기대하지 않는다.
