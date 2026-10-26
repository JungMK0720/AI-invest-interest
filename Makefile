.PHONY: help dev test test-ml test-backend build-frontend build model docker-up docker-down docker-seed export-repos

BACKEND_PORT ?= 8080
FRONTEND_PORT ?= 5173
PYTHON ?= python

help:
	@echo "make dev            Spring(H2) + Vite local development (POSIX shell)"
	@echo "make test           ML regression + Java tests + frontend build"
	@echo "make model          Retrain/export models inside back repository"
	@echo "make docker-up      Local Postgres + API (infra/compose.yaml)"
	@echo "make docker-seed    Initial question/product seed (empty local DB only)"
	@echo "make export-repos   Export independent repositories into .build/repositories"

# Windows PowerShell users: run the individual commands in README.md.
dev:
	@command -v mvn >/dev/null && command -v npm >/dev/null || { echo "Java 21/Maven and Node/npm required" >&2; exit 1; }
	@if [ ! -d front/node_modules ]; then cd front && npm ci; fi
	@(cd front && VITE_API_BASE_URL=http://localhost:$(BACKEND_PORT)/api npm run dev -- --host localhost --port $(FRONTEND_PORT) --strictPort) & frontend_pid=$$!; \
	  trap 'kill $$frontend_pid 2>/dev/null || true; wait $$frontend_pid 2>/dev/null || true' EXIT INT TERM; \
	  set -a; [ ! -f back/.env ] || . back/.env; set +a; \
	  cd back && SERVER_PORT=$(BACKEND_PORT) \
	    FINMATE_CORS_ALLOWED_ORIGINS=http://localhost:$(FRONTEND_PORT),http://127.0.0.1:$(FRONTEND_PORT) \
	    mvn spring-boot:run

test: test-ml test-backend build-frontend
test-ml:
	@$(PYTHON) -m unittest discover -s back/ml -p 'test_*.py'
test-backend:
	@cd back && mvn -B test
build-frontend:
	@if [ ! -d front/node_modules ]; then cd front && npm ci; fi
	@cd front && npm run build
build:
	@cd back && mvn -B -DskipTests package
	@$(MAKE) build-frontend
model:
	@$(PYTHON) back/ml/train.py
docker-up:
	@docker compose --env-file infra/.env -f infra/compose.yaml up --build
docker-down:
	@docker compose --env-file infra/.env -f infra/compose.yaml down
docker-seed:
	@docker compose --env-file infra/.env -f infra/compose.yaml exec -T postgres psql -v ON_ERROR_STOP=1 -U finmate -d finmate < back/src/main/resources/data.sql
export-repos:
	@$(PYTHON) tools/export_repositories.py