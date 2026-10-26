# oh-my-finance / front

증권업을 이해하기 위한 FinMATE 학습 서비스의 React 19 + TypeScript + Vite 프런트엔드입니다. 이 디렉터리 전체가 독립 `front` 저장소의 루트가 됩니다.

## 개발

Node.js는 `.nvmrc` 기준입니다. API는 별도로 `back` 저장소에서 실행합니다.

```powershell
npm ci
Copy-Item .env.example .env.local
npm run dev
```

기본 API는 `http://localhost:8080/api`, 화면은 `http://localhost:5173`입니다. 다른 API를 사용할 때 `.env.local`의 `VITE_API_BASE_URL`을 변경합니다. 인증은 Spring API의 DB 해시 세션을 사용하며, 진단 이력·학습 기록도 API로 조회합니다. 브라우저 토큰 저장은 현재 `localStorage` 방식입니다.

```powershell
npm run lint
npm run build
```

## Netlify 배포

독립 `front` 저장소를 연결하면 base directory는 루트(비움), build는 `npm run build`, publish는 `dist`입니다. 현재 통합 작업공간을 직접 연결할 경우에만 base directory를 `front`로 설정합니다. SPA redirect는 `netlify.toml`에 있습니다.

호스팅 환경변수에 `VITE_API_BASE_URL=https://<실제-api-host>/api`를 넣은 다음 빌드합니다. Vite 환경변수는 빌드 시 브라우저 번들에 포함됩니다. DB URL·DB 비밀번호·Supabase secret/service role 키·LLM 키는 여기에 넣지 않습니다. API의 `FRONTEND_ORIGIN`에는 프런트의 정확한 HTTPS origin을 설정합니다.

## 경계와 다음 단계

- 이 저장소는 화면·상호작용·API 호출·프런트 빌드 설정을 소유합니다.
- DB 접근과 업무 규칙은 `back`, 배포 운영은 `infra`, UR/FR/NFR과 학습 로드맵은 `docs`가 소유합니다.
- 증권업 입문 → 차트 복기 → 경량 모델 실험은 새 계획입니다. 현재 구현은 성향 진단·용어·개념·퀴즈·시뮬레이션·복습·콘텐츠 운영입니다.

조직 저장소 대상: [back](https://github.com/oh-my-finance/back), [infra](https://github.com/oh-my-finance/infra), [docs](https://github.com/oh-my-finance/docs). 원격 생성·권한·기본 브랜치는 이 작업에서 확인되지 않았습니다.