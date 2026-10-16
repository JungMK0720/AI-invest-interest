.PHONY: help dev test test-ml test-backend build-frontend build model docker-up docker-down docker-seed

BACKEND_PORT ?= 8080
FRONTEND_PORT ?= 5173

help:
	@echo "make dev           H2 백엔드 + Vite 프런트 실행 (http://localhost:$(FRONTEND_PORT))"
	@echo "                   포트 변경: make dev BACKEND_PORT=8180 FRONTEND_PORT=5180"
	@echo "make test          Python 모델 테스트 → Java 테스트 → 프런트 빌드"
	@echo "make model         두 보조 텍스트 모델 재학습·평가·내보내기 (약 20초)"
	@echo "make build         백엔드 패키징 + 프런트 빌드"
	@echo "make docker-up     PostgreSQL + prod 프로필 백엔드 컨테이너 실행"
	@echo "make docker-down   컨테이너 중지"
	@echo "make docker-seed   새 PostgreSQL에 진단 문항·상품 시드를 한 번 적재"

# backend/.env가 있으면 관리자 계정·API 키를 읽는다(예시: backend/.env.example).
# 프런트는 백그라운드로 띄워 Ctrl-C 한 번으로 두 프로세스를 함께 멈춘다.
dev:
	@command -v mvn >/dev/null && command -v npm >/dev/null || { echo "Java 21/Maven과 Node.js/npm이 필요합니다." >&2; exit 1; }
	@if [ ! -d frontend/node_modules ]; then cd frontend && npm ci; fi
	@echo "Frontend: http://localhost:$(FRONTEND_PORT)  Backend: http://localhost:$(BACKEND_PORT)/api"
	@(cd frontend && VITE_API_BASE_URL=http://localhost:$(BACKEND_PORT)/api npm run dev -- --host localhost --port $(FRONTEND_PORT) --strictPort) & frontend_pid=$$!; \
	  trap 'kill $$frontend_pid 2>/dev/null || true; wait $$frontend_pid 2>/dev/null || true' EXIT INT TERM; \
	  set -a; [ -f backend/.env ] && . backend/.env; set +a; \
	  cd backend && SERVER_PORT=$(BACKEND_PORT) \
	    FINMATE_CORS_ALLOWED_ORIGINS=http://localhost:$(FRONTEND_PORT),http://127.0.0.1:$(FRONTEND_PORT) \
	    mvn spring-boot:run

test: test-ml test-backend build-frontend

test-ml:
	@python3 -m unittest discover -s ml -p 'test_*.py'

test-backend:
	@cd backend && mvn -B test

build-frontend:
	@if [ ! -d frontend/node_modules ]; then cd frontend && npm ci; fi
	@cd frontend && npm run build

build:
	@cd backend && mvn -B -DskipTests package
	@$(MAKE) build-frontend

model:
	@python3 ml/train.py

docker-up:
	@docker compose -f backend/docker-compose.yml up --build

docker-down:
	@docker compose -f backend/docker-compose.yml down

docker-seed:
	@docker compose -f backend/docker-compose.yml exec -T postgres psql -U finmate -d finmate < backend/src/main/resources/data.sql
