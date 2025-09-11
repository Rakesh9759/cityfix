# ----- Makefile (root) -----

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

# ---------- Local Orchestration (to be used in Step 2) ----------
local-up: ## Start local stack (added in Step 2)
	@echo "Stub. Implement in Step 2."

local-down: ## Stop local stack (added in Step 2)
	@echo "Stub. Implement in Step 2."

seed: ## Seed local data (added later)
	@echo "Stub. Implement after DB is ready."

# ---------- Local Stack ----------
COMPOSE := docker compose --env-file .env.dev

.PHONY: local-up local-init local-health local-logs local-down local-nuke

## Start local stack (Postgres, Redis, RabbitMQ, Localstack)
local-up:
	@$(COMPOSE) up -d
	@echo "Waiting for Localstack to be healthy..."
	@bash -c 'for i in {1..30}; do \
		status=$$(docker inspect -f "{{.State.Health.Status}}" cityfix-localstack 2>/dev/null || echo "unknown"); \
		echo "Localstack health: $$status"; \
		[ "$$status" = "healthy" ] && exit 0; \
		sleep 2; \
	done; echo "ERROR 2.D.E2: Localstack not healthy after timeout" >&2; exit 1'

## Initialize Localstack (S3 bucket, SES identity, SSM param)
local-init:
	@./scripts/localstack-init.sh || { echo "ERROR 2.D.E3: localstack-init failed"; exit 1; }

## Show container health statuses
local-health:
	@echo "Postgres:   $$(docker inspect -f '{{.State.Health.Status}}' cityfix-postgres 2>/dev/null || echo 'n/a')"
	@echo "Redis:      $$(docker inspect -f '{{.State.Health.Status}}' cityfix-redis 2>/dev/null || echo 'n/a')"
	@echo "RabbitMQ:   $$(docker inspect -f '{{.State.Health.Status}}' cityfix-rabbitmq 2>/dev/null || echo 'n/a')"
	@echo "Localstack: $$(docker inspect -f '{{.State.Health.Status}}' cityfix-localstack 2>/dev/null || echo 'n/a')"

## Tail logs for all services
local-logs:
	@$(COMPOSE) logs -f

## Stop stack (keep volumes)
local-down:
	@$(COMPOSE) down

## Nuke stack (remove volumes)
local-nuke:
	@$(COMPOSE) down -v
