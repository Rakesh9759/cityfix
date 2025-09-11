# ----- Makefile (root) -----
SHELL := /bin/bash
.DEFAULT_GOAL := help

## Print available targets
help:
	@echo "CityFix Make Targets:"
	@grep -E '^[a-zA-Z0-9_-]+:.*?## ' Makefile | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-22s\033[0m %s\n", $$1, $$2}'

# ---------- Meta ----------
fmt: ## Format repo files (placeholder; real formatters added later)
	@echo "Nothing to format yet."

lint: ## Lint repo (placeholder)
	@echo "Nothing to lint yet."

test: ## Run tests (placeholder)
	@echo "No tests yet."

seed: ## Seed local data (added later)
	@echo "Stub. Implement after DB is ready."

# ---------- Local Stack ----------
COMPOSE := docker compose --env-file .env.dev

.PHONY: local-up local-init local-health local-logs local-down local-nuke

local-up: ## Start local stack (Postgres, Redis, RabbitMQ, Localstack)
	@$(COMPOSE) up -d
	@echo "Waiting for Localstack (aws s3 ls)..."
	@i=1; \
	while [ $$i -le 90 ]; do \
		aws --endpoint-url http://localhost:4566 s3 ls >/dev/null 2>&1 && echo "Localstack ready" && exit 0; \
		sleep 2; \
		i=$$((i+1)); \
	done; \
	echo "ERROR 2.E.E8: Localstack AWS CLI readiness check failed" >&2; exit 1

local-init: ## Initialize Localstack (S3 bucket, SES identity, SSM param)
	@./scripts/localstack-init.sh || { echo "ERROR 2.D.E3: localstack-init failed"; exit 1; }

local-health: ## Show container health statuses
	@printf "Postgres:   "; docker exec cityfix-postgres pg_isready -U postgres -d cityfix -h localhost >/dev/null 2>&1 && echo "ready" || echo "not-ready"
	@printf "Redis:      "; docker exec cityfix-redis redis-cli ping >/dev/null 2>&1 && echo "ready" || echo "not-ready"
	@printf "RabbitMQ:   "; docker exec cityfix-rabbitmq rabbitmq-diagnostics -q ping >/dev/null 2>&1 && echo "ready" || echo "not-ready"
	@printf "Localstack: "; aws --endpoint-url http://localhost:4566 s3 ls >/dev/null 2>&1 && echo "ready" || echo "not-ready"

local-logs: ## Tail logs for all services
	@$(COMPOSE) logs -f

local-down: ## Stop stack (keep volumes)
	@$(COMPOSE) down

local-nuke: ## Nuke stack (remove volumes)
	@$(COMPOSE) down -v

