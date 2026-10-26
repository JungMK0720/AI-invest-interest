# Organization 저장소 이관

기준일: 2026-10-26. 로컬 디렉터리 정리는 완료 대상이고 원격 생성·push·도메인 전환·클라우드 배포는 별도 작업이다. 공개 organization 페이지는 확인했으나, 현재 Git 인증으로 `front/back/infra` 조회는 `Repository not found`를 반환했다. 비공개·권한·이름 중 어느 원인인지 확인되지 않았다. 저장소가 없다고 단정하지 않는다.

## 준비된 경계

`front`, `back`, `infra`, `docs` 네 디렉터리가 각각 저장소 루트다. 원본 `origin`과 Git 이력은 유지했다. 이름은 사용자가 지정한 `front/back/infra`를 기준으로 맞췄고 `docs`를 추가 제안한다.

```powershell
# 이 작업공간 루트에서 실행. 생성 대상이 비어 있어야 한다.
python tools/export_repositories.py --dry-run
python tools/export_repositories.py
```

생성 결과는 `.build/repositories/{front,back,infra,docs}`다. Git이 추적하거나 ignore하지 않은 파일 중 각 경계 안의 파일만 복사하고 `.git`·비밀 환경파일·캐시·빌드 산출물은 제외한다. 실제 비밀값은 파일 이름 검사만으로 보장할 수 없으므로 push 전 diff를 검토한다. 스크립트는 원격 생성·commit·push를 하지 않는다.

## 원격 이관 시 결정

- 기존 front/back/infra의 정확한 이름·권한·기본 브랜치를 먼저 확인하고 기존 내용을 읽는다. 비어 있지 않은 저장소에 덮어쓰거나 force push하지 않는다.
- docs 저장소는 기존 가시성 정책에 맞춰 추가한다. UR/FR/NFR을 다른 저장소로 더 나누지 않는다.
- 새 저장소는 독립 이력으로 시작할 수 있다. 그 경우 첫 commit에 원본 URL·원본 HEAD·이관 날짜를 적고 원본 저장소를 보존한다. 내보내기는 Git 과거 이력을 복사하지 않는다.
- 과거 이력 보존이 필수이면 별도 복제본에서 `git filter-repo`로 경로를 추출해야 한다. back은 backend와 ml 두 경로를 합쳐야 하므로 단순 한 경로 subtree split으로 끝나지 않는다. 원본 작업공간의 이력은 재작성하지 않는다.
- front/back의 CI는 각각 저장소 루트에서 실행한다. infra compose는 로컬에서 front/back/infra/docs를 형제 디렉터리로 clone한 구성을 가정한다.

## 배포 순서

1. 분리한 back에서 Python·Java 검증, front에서 lint·build를 다시 실행한다.
2. [DB 준비](../architecture/database-strategy.md)를 마치고 staging DB와 back을 연결한다.
3. infra의 Blueprint는 **독립 back 저장소**를 참조한다. 현재 원본 저장소에 그대로 적용하지 않는다. 실제 원격 기본 브랜치와 Blueprint 접근권한을 확인한다.
4. Netlify는 독립 front 루트에서 빌드하고 `VITE_API_BASE_URL`을 실제 API HTTPS 주소로 설정한다. API에는 실제 프런트 origin을 CORS로 허용한다.
5. 가입 → 개념·퀴즈 → 진단·이력 → 관리자 게시, 재시작 후 지속성, 모바일 경로 새로고침, API 오류 처리를 확인한다.
6. [성능 검증 계획](../operations/load-testing.md)의 소규모 단계와 복구 검증을 통과한 뒤 공개 주소를 전환한다.

수정 전 원본 HEAD는 `2594646`(2026-10-16)이다. 이번 변경의 테스트 환경과 결과는 [검증 기록](../operations/verification.md)에 남긴다.
