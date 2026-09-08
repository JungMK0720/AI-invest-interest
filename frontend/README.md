# FinMATE Frontend

React + Vite + TypeScript. `backend/`(Spring Boot) API를 그대로 호출하는 실제 동작 프론트엔드.
S1~S6 전 화면(회원가입/로그인, 진단 시작, 진단 진행, 분석 결과, 탐색 카테고리·상품 목록, 상품 상세)이
목업이 아니라 실제 API와 연동되어 동작한다.

## 로컬 실행

```bash
# 1) 백엔드 먼저 기동 (다른 터미널)
cd ../backend && mvn spring-boot:run

# 2) 프론트엔드
npm install
npm run dev
# http://localhost:5173
```

`.env.development`의 `VITE_API_BASE_URL`이 로컬 백엔드(`http://localhost:8080/api`)를 가리킨다.

## 배포 (Netlify)

1. 이 저장소를 Netlify에 연결하고 base directory를 `frontend`로 지정.
2. Netlify 사이트 설정 > Environment variables에 `VITE_API_BASE_URL` = 배포된 Render 백엔드 URL(`/api` 포함) 등록.
3. `netlify.toml`이 빌드 명령(`npm run build`)과 SPA 라우팅 리다이렉트를 이미 포함하고 있어 추가 설정 불필요.
4. 백엔드(`render.yaml`)의 `FRONTEND_ORIGIN` 환경변수에 이 Netlify 도메인을 넣어야 CORS가 허용된다.

## 알려진 제한

- 인증 토큰은 백엔드가 메모리에만 보관한다(ADR-009). 백엔드가 재시작되면 로그인이 풀린다.
- "지난 진단 결과 다시 보기"는 브라우저 `localStorage`에 저장된 마지막 assessmentId 하나만 기억한다.
  진단 이력 목록 API가 아직 없기 때문이며, 2차 Agile 확장 후보다.
