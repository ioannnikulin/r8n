LOCAL_ENV_FILE := deployment/config/local.env
DOCKER_ENV_FILE := deployment/config/docker.env
DOCKER_SECRETS_ENV_FILE := deployment/secrets/docker.secrets.env
DOCKER_COMPOSE_ENV_ARGS := --env-file $(DOCKER_ENV_FILE) --env-file $(DOCKER_SECRETS_ENV_FILE)
LOAD_LOCAL_ENV = set -a; [ -f "$(LOCAL_ENV_FILE)" ] && . "$(LOCAL_ENV_FILE)"; set +a;
LOAD_DOCKER_ENV = set -a; . "$(DOCKER_ENV_FILE)"; . "$(DOCKER_SECRETS_ENV_FILE)"; set +a;
SERVICES_FILE := deployment/config/services.list
ifeq (,$(wildcard $(SERVICES_FILE)))
	$(error Missing $(SERVICES_FILE))
endif
SERVICES := $(shell awk 'NF && substr($$1, 1, 1) != sprintf("%c", 35) { printf "%s ", $$1 }' "$(SERVICES_FILE)")
FRONTEND_DIR := frontend
FRONTEND_GATEWAY_CERT := $(CURDIR)/deployment/certs/internal/gateway.crt
BOOT_JAR_TASKS := $(addprefix :,$(addsuffix -sv:bootJar,$(SERVICES)))
FRONTEND_CERT_DIR := deployment/certs/edge
FRONTEND_CERT_KEY := $(FRONTEND_CERT_DIR)/localhost.key
FRONTEND_CERT_CRT := $(FRONTEND_CERT_DIR)/localhost.crt
FRONTEND_NODE_VERSION := 22.14.0
FRONTEND_NVM_BOOTSTRAP = if [ -n "$$NVM_DIR" ] && [ -s "$$NVM_DIR/nvm.sh" ]; then . "$$NVM_DIR/nvm.sh"; elif [ -s "$$HOME/.nvm/nvm.sh" ]; then . "$$HOME/.nvm/nvm.sh"; fi; if command -v nvm >/dev/null 2>&1; then nvm use $(FRONTEND_NODE_VERSION) >/dev/null 2>&1 || nvm install $(FRONTEND_NODE_VERSION); fi;
FRONTEND_SHELL = set -e; $(FRONTEND_NVM_BOOTSTRAP) cd $(FRONTEND_DIR); \
frontend_npm() { if command -v nvm >/dev/null 2>&1; then nvm exec $(FRONTEND_NODE_VERSION) npm "$$@"; else npm "$$@"; fi; }; \
frontend_npx() { if command -v nvm >/dev/null 2>&1; then nvm exec $(FRONTEND_NODE_VERSION) npx "$$@"; else npx "$$@"; fi; };

.PHONY: help all local-run-all local-stop-all \
    $(addprefix local-run-,$(SERVICES)) \
    $(addprefix local-stop-,$(SERVICES)) \
    $(addprefix docker-logs-,$(SERVICES)) \
    prebuild-jars prepare-artifacts verify-artifacts docker-build docker-up \
    docker-certs docker-certs-force internal-certs internal-certs-force internal-certs-clean docker-certs-clean docker-secrets-clean docker-secrets-init edge-certs edge-certs-force \
    docker-down docker-logs clean-artifacts ensure-log-dirs clean-logs \
    routed-request-opinion routed-request-mock direct-request-opinion direct-request-mock \
    https-routed-request-opinion https-routed-request-mock \
    docker-database-drop-volume-personal docker-database-drop-volume-campus docker-run-database docker-database-connect \
    build-opinions who-ate-all-the-space clean-the-fuck-out-of-this-campus-machine \
    frontend-install frontend-install-all frontend-check-node frontend-dev frontend-build \
    frontend-test frontend-test-unit frontend-test-e2e frontend-clean frontend-clean-all frontend-cert frontend-cert-clean \
    clean fclean re move-caches-to-goinfre gradle-%-bootJar check-makefile

##@ Docker
docker-up: docker-build ensure-log-dirs docker-certs ## Start local Docker stack (builds images, ensures logs, generates certs)
	docker compose $(DOCKER_COMPOSE_ENV_ARGS) -f docker-compose.yml up -d

docker-down: ## Stop Docker stack
	docker compose $(DOCKER_COMPOSE_ENV_ARGS) -f docker-compose.yml down

$(addprefix local-run-,$(SERVICES)): local-run-%: ## Run one backend service locally
	@echo "Starting $*-sv..."
	@$(LOAD_LOCAL_ENV) \
	cd backend && (./gradlew :$*-sv:bootRun --args='--spring.profiles.active=local' > $*.log 2>&1 & \
	echo $$! > /tmp/$*.pid)

docker-build: docker-secrets-init verify-artifacts docker-database-create-data-folder frontend-build ## Build Docker images
	docker compose $(DOCKER_COMPOSE_ENV_ARGS) -f docker-compose.yml build

docker-down: ## Stop Docker stack
	docker compose $(DOCKER_COMPOSE_ENV_ARGS) -f docker-compose.yml down

docker-logs: ## Tail logs for all services
	docker compose $(DOCKER_COMPOSE_ENV_ARGS) -f docker-compose.yml logs -f $(SERVICES)

$(addprefix docker-logs-,$(SERVICES)): docker-logs-%: ## Tail logs for one service
	docker compose $(DOCKER_COMPOSE_ENV_ARGS) -f docker-compose.yml logs -f $*

prepare-artifacts: prebuild-jars ## Copy service JARs into deployment/ folders
	@set -e; \
	for svc in $(SERVICES); do \
		mkdir -p "deployment/$$svc"; \
		build_dir="backend/$$svc-sv/build/libs"; \
		if [ -d "backend/$$svc-sv/service/build/libs" ]; then \
			build_dir="backend/$$svc-sv/service/build/libs"; \
		fi; \
		jar_path="$$(find "$$build_dir" -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | head -n1)"; \
		if [ -z "$$jar_path" ]; then \
			echo "No bootJar found in $$build_dir for $$svc-sv. Run: cd backend && ./gradlew :$$svc-sv:bootJar" >&2; \
			exit 1; \
		fi; \
		cp "$$jar_path" "deployment/$$svc/app.jar"; \
	done

verify-artifacts: prepare-artifacts ## Verify deployment app.jar manifests (re-copy if invalid)
	@set -e; \
	invalid=false; \
	for svc in $(SERVICES); do \
		jar_path="deployment/$$svc/app.jar"; \
		if [ ! -f "$$jar_path" ]; then \
			echo "Missing $$jar_path"; \
			invalid=true; \
			continue; \
		fi; \
		if ! unzip -p "$$jar_path" META-INF/MANIFEST.MF 2>/dev/null | grep -Eq '^(Main-Class|Start-Class):'; then \
			echo "Invalid bootJar manifest in $$jar_path"; \
			invalid=true; \
		fi; \
	done; \
	if [ "$$invalid" = "true" ]; then \
		echo "Rebuilding artifacts..."; \
		$(MAKE) --no-print-directory prepare-artifacts; \
		for svc in $(SERVICES); do \
			jar_path="deployment/$$svc/app.jar"; \
			if ! unzip -p "$$jar_path" META-INF/MANIFEST.MF 2>/dev/null | grep -Eq '^(Main-Class|Start-Class):'; then \
				echo "Invalid bootJar manifest after rebuild in $$jar_path"; \
				exit 1; \
			fi; \
		done; \
	fi

prebuild-jars: ## Build backend bootJar artifacts
	cd backend && ./gradlew $(BOOT_JAR_TASKS)

gradle-%-bootJar: ## Build bootJar for one backend service
	cd backend && ./gradlew :$*-sv:bootJar

clean-artifacts: ## Remove deployment service JARs
	@for svc in $(SERVICES); do \
		rm -f "deployment/$$svc/app.jar"; \
	done

##@ Docker logs
ensure-log-dirs: ## Create log directories under deployment/
	@for svc in $(SERVICES); do \
		mkdir -p "deployment/$$svc/logs"; \
	done

docker-logs: ## Tail logs for all services
	docker compose $(DOCKER_COMPOSE_ENV_ARGS) -f docker-compose.yml logs -f $(SERVICES)

clean-logs: ## Remove deployment log files
	find deployment -type f -name '*.log' -delete

$(addprefix docker-logs-,$(SERVICES)): docker-logs-%: ## Tail logs for one service
	docker compose $(DOCKER_COMPOSE_ENV_ARGS) -f docker-compose.yml logs -f $*

##@ Certificates
docker-certs: internal-certs edge-certs ## Generate internal + edge TLS certs (skips if valid)

docker-certs-force: internal-certs-force edge-certs-force ## Force regenerate internal + edge TLS certs

internal-certs: ## Generate internal TLS certs (skips if valid)
	@$(LOAD_DOCKER_ENV) \
	chmod +x ./deployment/scripts/generate-internal-certs.sh && \
	./deployment/scripts/generate-internal-certs.sh

internal-certs-force: ## Force regenerate internal TLS certs
	@$(LOAD_DOCKER_ENV) \
	chmod +x ./deployment/scripts/generate-internal-certs.sh && \
	FORCE_REGEN_CERTS=true ./deployment/scripts/generate-internal-certs.sh

internal-certs-clean: ## Remove generated internal TLS certs
	rm -rf deployment/certs/internal

docker-certs-clean: ## Remove all generated TLS certs (internal + edge)
	rm -rf deployment/certs

##@ Docker secrets
docker-secrets-clean: ## Remove local Docker secrets file (TLS passwords)
	rm -f $(DOCKER_SECRETS_ENV_FILE)

docker-secrets-init: ## Ensure local Docker secrets file exists (prompts if missing)
	@bash -lc '\
	set -e; \
	file="$(DOCKER_SECRETS_ENV_FILE)"; \
	ks="$$TLS_KEYSTORE_PASSWORD"; \
	ts="$$TLS_TRUSTSTORE_PASSWORD"; \
	if [ -f "$$file" ]; then \
		[ -z "$$ks" ] && ks="$$(awk -F= "/^TLS_KEYSTORE_PASSWORD=/{print substr(\$$0,index(\$$0,\"=\")+1)}" "$$file")"; \
		[ -z "$$ts" ] && ts="$$(awk -F= "/^TLS_TRUSTSTORE_PASSWORD=/{print substr(\$$0,index(\$$0,\"=\")+1)}" "$$file")"; \
	fi; \
	if [ -z "$$ks" ]; then \
		read -s -p "TLS_KEYSTORE_PASSWORD (min 6 chars): " ks; echo; \
	fi; \
	if [ -z "$$ts" ]; then \
		read -s -p "TLS_TRUSTSTORE_PASSWORD (min 6 chars): " ts; echo; \
	fi; \
	if [ "$${#ks}" -lt 6 ] || [ "$${#ts}" -lt 6 ]; then \
		echo "Passwords must be at least 6 characters."; \
		exit 1; \
	fi; \
	mkdir -p "$$(dirname "$$file")"; \
	printf "TLS_KEYSTORE_PASSWORD=%s\nTLS_TRUSTSTORE_PASSWORD=%s\n" "$$ks" "$$ts" > "$$file"; \
	echo "Wrote $$file"; \
	'

generate-jwt-keys-%: ## Generate RSA JWT keypair and update deployment/config/<env>.env (env examples: local, docker)
	chmod +x ./deployment/scripts/generate-jwt-keypair.sh
	./deployment/scripts/generate-jwt-keypair.sh $*

##@ Maintenance
clean-logs: ## Remove deployment log files
	find deployment -type f -name '*.log' -delete

clean-artifacts: ## Remove deployment service JARs
	@for svc in $(SERVICES); do \
		rm -f "deployment/$$svc/app.jar"; \
	done

clean: clean-artifacts clean-logs frontend-clean ## Remove backend artifacts/logs and frontend cache
	rm -f .access_token

fclean: clean frontend-clean-all ## Remove clean plus frontend node_modules and certs

re: fclean docker-build ## Full rebuild (clean + docker-build)

##@ Local Backend
local-run-all: $(addprefix local-run-,$(SERVICES)) ## Run all backend services locally

##@ Docker database
docker-database-run: docker-database-create-data-folder ## Start only the database container
	chmod a+x deployment/database/init/01_create_schemas.sh
	docker compose $(DOCKER_COMPOSE_ENV_ARGS) -f docker-compose.yml up -d database

docker-database-create-data-folder:
	@mkdir -p ./deployment/database/data

docker-database-drop-volume-personal: ## Delete local Docker DB volume (personal)
	@sudo rm -rf ./deployment/database/data

docker-database-drop-volume-campus: ## Delete local Docker DB volume (campus)
	@# yes this is the only way, since some files are owned by root, and you don't have sudo rights in campus
	@docker run --rm -v ./deployment/database:/pg alpine rm -rf /pg/data

docker-database-connect: ## Connect to local database via psql
	$(LOAD_LOCAL_ENV) \
	docker exec -it database psql -U $$DATABASE_USERNAME -d $$DATABASE_NAME

##@ Local Backend
local-run-all: $(addprefix local-run-,$(SERVICES)) ## Run all backend services locally

local-stop-all: $(addprefix local-stop-,$(SERVICES)) ## Stop all local backend services

$(addprefix local-stop-,$(SERVICES)): local-stop-%: ## Stop one local backend service
	-@set -e; \
	if [ -f /tmp/$*.pid ]; then \
		kill $$(cat /tmp/$*.pid) 2>/dev/null || true; \
		rm -f /tmp/$*.pid; \
	fi; \
	$(LOAD_LOCAL_ENV) \
	port=""; \
	case "$*" in \
		gateway) port="$$GATEWAY_PORT" ;; \
		opinions) port="$$SERVICES_OPINIONS_PORT" ;; \
		mock) port="$$SERVICES_MOCK_PORT" ;; \
	esac; \
	if [ -n "$$port" ] && command -v lsof >/dev/null 2>&1; then \
		pids="$$(lsof -ti tcp:$$port 2>/dev/null || true)"; \
		[ -z "$$pids" ] || kill $$pids 2>/dev/null || true; \
	fi

##@ Frontend
frontend-dev: frontend-install ## Start Vite dev server
	@bash -lc '$(FRONTEND_SHELL) NODE_EXTRA_CA_CERTS="$(FRONTEND_GATEWAY_CERT)" frontend_npm run dev'

LOGIN_USER := test@test.test
LOGIN_PASS := 1234

get-token: ## Obtain a JWT token using login credentials
	@$(LOAD_LOCAL_ENV) \
	token=$$(curl -s -X POST "http://localhost:8080/auth/login" \
		-H "Content-Type: application/json" \
		-d '{"login": "$(LOGIN_USER)", "password": "$(LOGIN_PASS)"}' | \
		grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4); \
	if [ -z "$$token" ]; then \
		echo "Failed to obtain token. Is the users-sv running?"; \
		exit 1; \
	fi; \
	echo "$$token" > .access_token

routed-request-opinion: ## HTTP gateway request to opinions (local)
	@if [ ! -f .access_token ]; then $(MAKE) get-token; fi
	curl "http://localhost:8080/opinions/30000000-0000-0000-0000-000000000001" -i -H "Authorization: Bearer $$(cat .access_token)"

routed-request-mock: ## HTTP gateway request to mock (local)
	@if [ ! -f .access_token ]; then $(MAKE) get-token; fi
	curl "http://localhost:8080/opinion-lists/00000000-0000-0000-0000-000000000000/summary" -i -H "Authorization: Bearer $$(cat .access_token)"

direct-request-opinion: ## HTTP direct request to opinions (local)
	@if [ ! -f .access_token ]; then $(MAKE) get-token; fi
	curl "http://localhost:8081/opinions/30000000-0000-0000-0000-000000000001" -i -H "Authorization: Bearer $$(cat .access_token)"

direct-request-mock: ## HTTP direct request to mock (local)
	@if [ ! -f .access_token ]; then $(MAKE) get-token; fi
	curl "http://localhost:8090/opinion-lists/00000000-0000-0000-0000-000000000000/summary" -i -H "Authorization: Bearer $$(cat .access_token)"

routed-request-gdpr:
	@if [ ! -f .access_token ]; then $(MAKE) get-token; fi
	curl "http://localhost:8080/users/export" -i -H "Authorization: Bearer $$(cat .access_token)"

# frontend
frontend-check-node: ## Check Node.js version (attempts nvm if too old)
	@FRONTEND_NODE_VERSION="$(FRONTEND_NODE_VERSION)" ./scripts/frontend-check-node.sh

frontend-build: frontend-install ## Build frontend dist (installs deps if missing)
	@bash -lc '$(FRONTEND_SHELL) [ -d node_modules ] || frontend_npm ci; frontend_npm run build'

frontend-test-unit: frontend-check-node ## Run frontend unit tests
	@bash -lc '$(FRONTEND_SHELL) frontend_npm run test:unit'

frontend-test: frontend-test-unit frontend-test-e2e ## Run all frontend tests

frontend-test-e2e: frontend-check-node ## Run frontend E2E tests
	@bash -lc '$(FRONTEND_SHELL) frontend_npm run test:e2e'

frontend-clean: ## Remove frontend build output, cache, and test artifacts
	rm -rf $(FRONTEND_DIR)/dist $(FRONTEND_DIR)/node_modules/.vite $(FRONTEND_DIR)/playwright-report $(FRONTEND_DIR)/test-results $(FRONTEND_DIR)/coverage

frontend-clean-all: frontend-clean frontend-cert-clean ## Remove frontend build output, cache, node_modules, and certs
	rm -rf $(FRONTEND_DIR)/node_modules

edge-certs: ## Generate edge (frontend) HTTPS certs
	@./deployment/scripts/generate-edge-certs.sh

edge-certs-force: ## Force regenerate edge (frontend) HTTPS certs
	@FORCE_REGEN_CERTS=true ./deployment/scripts/generate-edge-certs.sh

frontend-cert: edge-certs ## Generate frontend HTTPS certs

frontend-cert-clean: ## Remove generated frontend certs
	rm -rf deployment/certs/edge

##@ Smoke Tests
https-routed-request-opinion: ## HTTPS gateway request to opinions
	@if [ ! -f .access_token ]; then $(MAKE) get-token; fi
	curl --cacert deployment/certs/internal/gateway.crt "https://localhost:8080/opinions/30000000-0000-0000-0000-000000000001" -i -H "Authorization: Bearer $$(cat .access_token)"

https-routed-request-mock: ## HTTPS gateway request to mock
	@if [ ! -f .access_token ]; then $(MAKE) get-token; fi
	curl --cacert deployment/certs/internal/gateway.crt "https://localhost:8080/opinion-lists/00000000-0000-0000-0000-000000000000/summary" -i -H "Authorization: Bearer $$(cat .access_token)"

https-routed-request-gdpr: ## HTTPS gateway request to users service
	curl --cacert deployment/certs/internal/gateway.crt "https://localhost:8080/users/export" -i -H "Authorization: Bearer stub-access-token-123"

##@ 42 campus utilities
clean-the-fuck-out-of-this-campus-machine: ## Remove large local caches (campus machine only)
	docker run --rm -v  ~/.local/share:/disk alpine rm -rf /disk/docker || true
	rm -rf ~/.local/share/docker ~/.var/app/com.slack.Slack ~/.config/Code ~/.config/Slack ~/.config/google-chrome || true
	mkdir -p ~/.local/share/docker/tmp && chmod 700 ~/.local/share/docker/tmp
	docker system prune -f
	docker volume rm $(docker volume ls -qf dangling=true) || true
	pkill -f GradleDaemon

who-ate-all-the-space: ## Show top-level disk usage in home
	du --all --human-readable --one-file-system --max-depth=1 ~

move-caches-to-goinfre: ## Move Docker and Gradle caches to /goinfre (campus machine)
	@chmod +x scripts/move-docker-to-goinfre.sh
	./scripts/move-docker-to-goinfre.sh

##@ Linters
check-makefile: ## Lint Makefile formatting and conflicts
	chmod +x utils/lint-makefile.sh
	./utils/lint-makefile.sh

##@ CI check stages
test-github-backend: test-backend

test-github-frontend: test-frontend

test-github-e2e: test-e2e

test-github: test-github-backend test-github-frontend test-github-e2e

test-backend: lint-backend
	cd backend && ./gradlew test

lint-backend:
	cd backend && ./gradlew ktlintCheck

test-frontend-prepare:
	cd frontend && npm ci

test-frontend: test-frontend-prepare
	cd frontend && npm run type-check && npx eslint . --max-warnings=0 && npm run test:unit -- --run && npm run build-only

test-e2e:
	cd frontend && npm run test:e2e

##@ entrypoints
help: ## Show this help
	@awk 'BEGIN {FS=":.*##"} /^##@/ {printf "\n%s:\n", substr($$0,5)} /^[a-zA-Z0-9_.%-]+:.*##/ {printf "  %-32s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

all: docker-up ## Default target

clean: clean-artifacts clean-logs frontend-clean ## Remove backend artifacts/logs and frontend cache

fclean: clean frontend-clean-all ## Remove clean plus frontend node_modules and certs

re: fclean docker-build ## Full rebuild (clean + docker-build)
