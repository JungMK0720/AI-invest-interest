.PHONY: help dev test build model docker-up docker-down docker-seed

help:
	@echo "make dev         Start the H2 backend and Vite frontend (http://localhost:5173)"
	@echo "make test        Run Python and Java tests, then build the frontend"
	@echo "make build       Package the backend and build the frontend"
	@echo "make model       Retrain and evaluate the two local text models"
	@echo "make docker-up   Start PostgreSQL and the production-profile backend"
	@echo "make docker-down Stop the PostgreSQL/backend containers"
	@echo "make docker-seed Load the original demo rows into a fresh PostgreSQL database once"

# The frontend runs in the background so one Ctrl-C stops both local processes.
# H2 and the original demo SQL initialize automatically with the default Spring profile.
dev:
	@command -v mvn >/dev/null && command -v npm >/dev/null || { echo "Java 21/Maven and Node.js/npm are required." >&2; exit 1; }
	@if [ ! -d frontend/node_modules ]; then cd frontend && npm ci; fi
	@echo "Frontend: http://localhost:5173  Backend: http://localhost:8080/api"
	@(cd frontend && npm run dev -- --host 127.0.0.1) & frontend_pid=$$!; \
	  trap 'kill $$frontend_pid 2>/dev/null || true; wait $$frontend_pid 2>/dev/null || true' EXIT INT TERM; \
	  cd backend && mvn spring-boot:run

test:
	@python3 -m unittest discover -s ml -p 'test_*.py'
	@cd backend && mvn -B test
	@if [ ! -d frontend/node_modules ]; then cd frontend && npm ci; fi
	@cd frontend && npm run build

build:
	@cd backend && mvn -B -DskipTests package
	@if [ ! -d frontend/node_modules ]; then cd frontend && npm ci; fi
	@cd frontend && npm run build

model:
	@python3 ml/train.py

docker-up:
	@docker compose -f backend/docker-compose.yml up --build

docker-down:
	@docker compose -f backend/docker-compose.yml down

docker-seed:
	@docker compose -f backend/docker-compose.yml exec -T postgres psql -U finmate -d finmate < backend/src/main/resources/data.sql
