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
