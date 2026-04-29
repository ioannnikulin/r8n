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
    get-token refresh-token logout routed-request-opinion routed-request-mock routed-request-user-profile routed-request-gdpr routed-request-messaging-threads direct-request-opinion direct-request-mock \
    direct-request-swagger public-request-user routed-request-opinion-approved routed-request-opinion-forbidden routed-request-opinion-mine \
    routed-request-moderation-approve-flow routed-request-moderation-reject-flow routed-request-moderation-decisions \
    docker-database-create-data-folder docker-database-drop-volume-personal docker-database-drop-volume-campus docker-database-run docker-database-connect \
    build-opinions who-ate-all-the-space clean-the-fuck-out-of-this-campus-machine \
    frontend-install frontend-install-all frontend-check-node frontend-dev frontend-build frontend-lint \
    frontend-test frontend-test-unit frontend-test-e2e frontend-test-e2e-ui frontend-test-e2e-api frontend-clean frontend-clean-all frontend-cert frontend-cert-clean \
    lint-backend test-backend test-frontend-prepare test-frontend test-e2e routed-request-opinion-list \
    test-github-backend test-github-frontend test-github-e2e test-github \
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

##@ Docker artifacts & jars preparations
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
	if [ "$*" = "gateway" ]; then \
		port="$$GATEWAY_PORT"; \
	else \
		service_key=$$(printf "%s" "$*" | tr "[:lower:]-" "[:upper:]_"); \
		port_var="SERVICES_$${service_key}_PORT"; \
		eval "port=\$${$$port_var:-}"; \
	fi; \
	if [ -n "$$port" ] && command -v lsof >/dev/null 2>&1; then \
		pids="$$(lsof -ti tcp:$$port 2>/dev/null || true)"; \
		[ -z "$$pids" ] || kill $$pids 2>/dev/null || true; \
	fi

##@ Frontend
frontend-dev: frontend-install ## Start Vite dev server
	@bash -lc '$(LOAD_LOCAL_ENV) $(FRONTEND_SHELL) NODE_EXTRA_CA_CERTS="$(FRONTEND_GATEWAY_CERT)" frontend_npm run dev'

##@ Smoke tests

LOGIN_USER := test@test.test
LOGIN_PASS := 1234

get-token: ## Obtain a JWT token and refresh cookie (ENV=local|docker, default: local)
	@if [ "$(ENV)" = "docker" ]; then \
		$(LOAD_DOCKER_ENV) \
		protocol=https; \
		host=localhost; \
		port=8080; \
	else \
		$(LOAD_LOCAL_ENV) \
		protocol=$${INTERSERVICE_PROTOCOL:-http}; \
		host=$${GATEWAY_HOST:-localhost}; \
		port=$${GATEWAY_PORT:-8080}; \
	fi; \
	curl_args="-s"; \
	if [ "$$protocol" = "https" ]; then curl_args="$$curl_args -k"; fi; \
	echo "Fetching CSRF token..."; \
	csrf_headers=$$(curl $$curl_args -c .cookies -i -X GET "$$protocol://$$host:$$port/api/auth/csrf" | tr -d '\r'); \
	csrf_token=$$(echo "$$csrf_headers" | grep -i "Set-Cookie: XSRF-TOKEN=" | sed 's/.*XSRF-TOKEN=\([^;]*\).*/\1/'); \
	if [ -z "$$csrf_token" ]; then \
		echo "Failed to obtain CSRF token. Headers:"; \
		echo "$$csrf_headers"; \
		exit 1; \
	fi; \
	echo "Logging in..."; \
	login_response=$$(curl $$curl_args -b .cookies -c .cookies -X POST "$$protocol://$$host:$$port/api/auth/login" \
		-H "Content-Type: application/json" \
		-H "X-XSRF-TOKEN: $$csrf_token" \
		-d '{"login": "$(LOGIN_USER)", "password": "$(LOGIN_PASS)"}'); \
	token=$$(echo "$$login_response" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4); \
	if [ -z "$$token" ]; then \
		echo "Failed to obtain token from $$protocol://$$host:$$port/api/auth/login. Response:"; \
		echo "$$login_response"; \
		exit 1; \
	fi; \
	echo "$$token" > .access_token; \
	echo "Token obtained and cookies saved in .cookies"

refresh-token: ## Refresh the JWT token using the refresh cookie (ENV=local|docker)
	@if [ "$(ENV)" = "docker" ]; then \
		$(LOAD_DOCKER_ENV) \
		protocol=https; \
		host=localhost; \
		port=8080; \
	else \
		$(LOAD_LOCAL_ENV) \
		protocol=$${INTERSERVICE_PROTOCOL:-http}; \
		host=$${GATEWAY_HOST:-localhost}; \
		port=$${GATEWAY_PORT:-8080}; \
	fi; \
	curl_args="-s"; \
	if [ "$$protocol" = "https" ]; then curl_args="$$curl_args -k"; fi; \
	if [ ! -f .cookies ]; then echo "No cookies found. Run 'make get-token' first."; exit 1; fi; \
	csrf_token=$$(grep "XSRF-TOKEN" .cookies | awk '{print $$7}'); \
	refresh_response=$$(curl $$curl_args -b .cookies -c .cookies -X POST "$$protocol://$$host:$$port/api/auth/refresh" \
		-H "X-XSRF-TOKEN: $$csrf_token"); \
	token=$$(echo "$$refresh_response" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4); \
	if [ -z "$$token" ]; then \
		echo "Failed to refresh token. Response:"; \
		echo "$$refresh_response"; \
		exit 1; \
	fi; \
	echo "$$token" > .access_token; \
	echo "Token refreshed and cookies updated in .cookies"

logout: ## Logout and clear cookies (ENV=local|docker)
	@if [ "$(ENV)" = "docker" ]; then \
		$(LOAD_DOCKER_ENV) \
		protocol=https; \
		host=localhost; \
		port=8080; \
	else \
		$(LOAD_LOCAL_ENV) \
		protocol=$${INTERSERVICE_PROTOCOL:-http}; \
		host=$${GATEWAY_HOST:-localhost}; \
		port=$${GATEWAY_PORT:-8080}; \
	fi; \
	curl_args="-s"; \
	if [ "$$protocol" = "https" ]; then curl_args="$$curl_args -k"; fi; \
	if [ ! -f .cookies ]; then echo "No cookies found."; exit 1; fi; \
	csrf_token=$$(grep "XSRF-TOKEN" .cookies | awk '{print $$7}'); \
	curl $$curl_args -b .cookies -c .cookies -X POST "$$protocol://$$host:$$port/api/auth/logout" \
		-H "X-XSRF-TOKEN: $$csrf_token" -i; \
	rm -f .access_token .cookies; \
	echo "Logged out and local session files removed."

# Temporary reviewer helpers for issue #93. Remove once subject/opinion creation and moderation
# can be verified fully through the normal frontend flow.
routed-request-moderation-approve-flow: ## Temporary full moderation approve smoke flow (ENV=local|docker)
	@if [ "$(ENV)" = "docker" ]; then \
		$(LOAD_DOCKER_ENV) \
		protocol=https; host=localhost; port=8080; \
	else \
		$(LOAD_LOCAL_ENV) \
		protocol=$${INTERSERVICE_PROTOCOL:-http}; host=$${GATEWAY_HOST:-localhost}; port=$${GATEWAY_PORT:-8080}; \
	fi; \
	set -e; \
	curl_args="-s"; \
	if [ "$$protocol" = "https" ]; then curl_args="$$curl_args -k"; fi; \
	api="$$protocol://$$host:$$port"; \
	login() { \
		email="$$1"; \
		cookie_file="$$(mktemp)"; \
		csrf_headers=$$(curl $$curl_args -c "$$cookie_file" -i "$$api/api/auth/csrf" | tr -d '\r'); \
		csrf_token=$$(printf "%s" "$$csrf_headers" | sed -n 's/.*XSRF-TOKEN=\([^;]*\).*/\1/p' | head -n1); \
		token=$$(curl $$curl_args -b "$$cookie_file" -c "$$cookie_file" -X POST "$$api/api/auth/login" \
			-H "Content-Type: application/json" \
			-H "X-XSRF-TOKEN: $$csrf_token" \
			-d "{\"login\":\"$$email\",\"password\":\"1234\"}" \
			| sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p'); \
		rm -f "$$cookie_file"; \
		printf "%s" "$$token"; \
	}; \
	json_id() { sed -n 's/.*"id":"\([^"]*\)".*/\1/p' | head -n1; }; \
	owner_token=$$(login anna@r8n.test); \
	moderator_token=$$(login lena@r8n.test); \
	if [ -z "$$owner_token" ] || [ -z "$$moderator_token" ]; then echo "Failed to log in seeded owner/moderator users"; exit 1; fi; \
	echo "Creating subject..."; \
	subject_json=$$(curl $$curl_args -X POST "$$api/api/subjects" \
		-H "Authorization: Bearer $$owner_token" \
		-H "Content-Type: application/json" \
		-d '{"name":"Moderation Approve Cafe","referentName":"Moderation Approve Cafe Berlin","address":"Teststrasse 1, Berlin","latitude":52.52,"longitude":13.405}'); \
	subject_id=$$(printf "%s" "$$subject_json" | json_id); \
	echo "$$subject_json"; \
	if [ -z "$$subject_id" ]; then echo "Subject creation failed"; exit 1; fi; \
	echo "Creating draft opinion..."; \
	opinion_json=$$(curl $$curl_args -X POST "$$api/api/opinions" \
		-H "Authorization: Bearer $$owner_token" \
		--data-urlencode "subjectId=$$subject_id" \
		--data-urlencode "mark=4.5" \
		--data-urlencode "subjective=Good coffee, calm room" \
		--data-urlencode "objective=Paid 4.20 EUR for espresso"); \
	opinion_id=$$(printf "%s" "$$opinion_json" | json_id); \
	echo "$$opinion_json"; \
	if [ -z "$$opinion_id" ]; then echo "Opinion creation failed"; exit 1; fi; \
	echo "Submitting opinion for moderation..."; \
	submit_json=$$(curl $$curl_args -X POST "$$api/api/opinions/$$opinion_id/submit-for-moderation" -H "Authorization: Bearer $$owner_token"); \
	echo "$$submit_json"; \
	printf "%s" "$$submit_json" | grep -q '"status":"PENDING_PREMODERATION"'; \
	echo "Checking moderator queue contains the opinion..."; \
	queue_json=$$(curl $$curl_args "$$api/api/opinions/moderation?page=0&size=20" -H "Authorization: Bearer $$moderator_token"); \
	echo "$$queue_json"; \
	printf "%s" "$$queue_json" | grep -q "$$opinion_id"; \
	echo "Approving opinion..."; \
	approve_json=$$(curl $$curl_args -X POST "$$api/api/opinions/$$opinion_id/approve" -H "Authorization: Bearer $$moderator_token"); \
	echo "$$approve_json"; \
	printf "%s" "$$approve_json" | grep -q '"status":"PUBLISHED"'; \
	echo "Checking persisted moderation decision..."; \
	decisions_json=$$(curl $$curl_args "$$api/api/opinions/moderation/decisions?page=0&size=20" -H "Authorization: Bearer $$moderator_token"); \
	echo "$$decisions_json"; \
	printf "%s" "$$decisions_json" | grep -q "$$opinion_id"; \
	printf "%s" "$$decisions_json" | grep -q '"action":"APPROVED"'; \
	echo "Approve moderation flow passed."

routed-request-moderation-reject-flow: ## Temporary full moderation reject smoke flow (ENV=local|docker)
	@if [ "$(ENV)" = "docker" ]; then \
		$(LOAD_DOCKER_ENV) \
		protocol=https; host=localhost; port=8080; \
	else \
		$(LOAD_LOCAL_ENV) \
		protocol=$${INTERSERVICE_PROTOCOL:-http}; host=$${GATEWAY_HOST:-localhost}; port=$${GATEWAY_PORT:-8080}; \
	fi; \
	set -e; \
	curl_args="-s"; \
	if [ "$$protocol" = "https" ]; then curl_args="$$curl_args -k"; fi; \
	api="$$protocol://$$host:$$port"; \
	login() { \
		email="$$1"; \
		cookie_file="$$(mktemp)"; \
		csrf_headers=$$(curl $$curl_args -c "$$cookie_file" -i "$$api/api/auth/csrf" | tr -d '\r'); \
		csrf_token=$$(printf "%s" "$$csrf_headers" | sed -n 's/.*XSRF-TOKEN=\([^;]*\).*/\1/p' | head -n1); \
		token=$$(curl $$curl_args -b "$$cookie_file" -c "$$cookie_file" -X POST "$$api/api/auth/login" \
			-H "Content-Type: application/json" \
			-H "X-XSRF-TOKEN: $$csrf_token" \
			-d "{\"login\":\"$$email\",\"password\":\"1234\"}" \
			| sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p'); \
		rm -f "$$cookie_file"; \
		printf "%s" "$$token"; \
	}; \
	json_id() { sed -n 's/.*"id":"\([^"]*\)".*/\1/p' | head -n1; }; \
	owner_token=$$(login anna@r8n.test); \
	moderator_token=$$(login lena@r8n.test); \
	if [ -z "$$owner_token" ] || [ -z "$$moderator_token" ]; then echo "Failed to log in seeded owner/moderator users"; exit 1; fi; \
	echo "Creating subject..."; \
	subject_json=$$(curl $$curl_args -X POST "$$api/api/subjects" \
		-H "Authorization: Bearer $$owner_token" \
		-H "Content-Type: application/json" \
		-d '{"name":"Moderation Reject Cafe","referentName":"Moderation Reject Cafe Berlin","address":"Teststrasse 2, Berlin","latitude":52.51,"longitude":13.4}'); \
	subject_id=$$(printf "%s" "$$subject_json" | json_id); \
	echo "$$subject_json"; \
	if [ -z "$$subject_id" ]; then echo "Subject creation failed"; exit 1; fi; \
	echo "Creating draft opinion..."; \
	opinion_json=$$(curl $$curl_args -X POST "$$api/api/opinions" \
		-H "Authorization: Bearer $$owner_token" \
		--data-urlencode "subjectId=$$subject_id" \
		--data-urlencode "mark=2.0" \
		--data-urlencode "subjective=Needs checking before publishing" \
		--data-urlencode "objective=Contains a claim that should be supported"); \
	opinion_id=$$(printf "%s" "$$opinion_json" | json_id); \
	echo "$$opinion_json"; \
	if [ -z "$$opinion_id" ]; then echo "Opinion creation failed"; exit 1; fi; \
	echo "Submitting opinion for moderation..."; \
	submit_json=$$(curl $$curl_args -X POST "$$api/api/opinions/$$opinion_id/submit-for-moderation" -H "Authorization: Bearer $$owner_token"); \
	echo "$$submit_json"; \
	printf "%s" "$$submit_json" | grep -q '"status":"PENDING_PREMODERATION"'; \
	echo "Rejecting opinion..."; \
	reject_json=$$(curl $$curl_args -X POST "$$api/api/opinions/$$opinion_id/reject" \
		-H "Authorization: Bearer $$moderator_token" \
		-H "Content-Type: application/json" \
		-d '{"reason":"Please add factual support and remove unverifiable claims."}'); \
	echo "$$reject_json"; \
	printf "%s" "$$reject_json" | grep -q '"status":"REJECTED"'; \
	echo "Checking persisted moderation decision..."; \
	decisions_json=$$(curl $$curl_args "$$api/api/opinions/moderation/decisions?page=0&size=20" -H "Authorization: Bearer $$moderator_token"); \
	echo "$$decisions_json"; \
	printf "%s" "$$decisions_json" | grep -q "$$opinion_id"; \
	printf "%s" "$$decisions_json" | grep -q '"action":"REJECTED"'; \
	printf "%s" "$$decisions_json" | grep -q 'Please add factual support'; \
	echo "Reject moderation flow passed."

routed-request-moderation-decisions: ## Temporary request for persisted moderation decisions (ENV=local|docker)
	@if [ "$(ENV)" = "docker" ]; then \
		$(LOAD_DOCKER_ENV) \
		protocol=https; host=localhost; port=8080; \
	else \
		$(LOAD_LOCAL_ENV) \
		protocol=$${INTERSERVICE_PROTOCOL:-http}; host=$${GATEWAY_HOST:-localhost}; port=$${GATEWAY_PORT:-8080}; \
	fi; \
	set -e; \
	curl_args="-s"; \
	if [ "$$protocol" = "https" ]; then curl_args="$$curl_args -k"; fi; \
	api="$$protocol://$$host:$$port"; \
	cookie_file="$$(mktemp)"; \
	csrf_headers=$$(curl $$curl_args -c "$$cookie_file" -i "$$api/api/auth/csrf" | tr -d '\r'); \
	csrf_token=$$(printf "%s" "$$csrf_headers" | sed -n 's/.*XSRF-TOKEN=\([^;]*\).*/\1/p' | head -n1); \
	token=$$(curl $$curl_args -b "$$cookie_file" -c "$$cookie_file" -X POST "$$api/api/auth/login" \
		-H "Content-Type: application/json" \
		-H "X-XSRF-TOKEN: $$csrf_token" \
		-d '{"login":"lena@r8n.test","password":"1234"}' \
		| sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p'); \
	rm -f "$$cookie_file"; \
	if [ -z "$$token" ]; then echo "Failed to log in seeded moderator user"; exit 1; fi; \
	curl $$curl_args "$$api/api/opinions/moderation/decisions?page=0&size=20" -H "Authorization: Bearer $$token"; \
	echo

routed-request-opinion-forbidden: ## failing gateway request to opinions (ENV=local|docker)
	@if [ ! -f .access_token ]; then $(MAKE) get-token ENV=$(ENV); fi
	@if [ "$(ENV)" = "docker" ]; then \
		$(LOAD_DOCKER_ENV) \
		protocol=https; \
		host=localhost; \
		port=8080; \
	else \
		$(LOAD_LOCAL_ENV) \
		protocol=$${INTERSERVICE_PROTOCOL:-http}; \
		host=$${GATEWAY_HOST:-localhost}; \
		port=$${GATEWAY_PORT:-8080}; \
	fi; \
	curl_args="-i"; \
	if [ "$$protocol" = "https" ]; then curl_args="$$curl_args -k"; fi; \
	curl $$curl_args "$$protocol://$$host:$$port/api/opinions/30000000-0000-0000-0000-000000000003" -H "Authorization: Bearer $$(cat .access_token)"

routed-request-opinion-mine: ## Gateway request to opinions (ENV=local|docker)
	@if [ ! -f .access_token ]; then $(MAKE) get-token ENV=$(ENV); fi
	@if [ "$(ENV)" = "docker" ]; then \
		$(LOAD_DOCKER_ENV) \
		protocol=https; \
		host=localhost; \
		port=8080; \
	else \
		$(LOAD_LOCAL_ENV) \
		protocol=$${INTERSERVICE_PROTOCOL:-http}; \
		host=$${GATEWAY_HOST:-localhost}; \
		port=$${GATEWAY_PORT:-8080}; \
	fi; \
	curl_args="-i"; \
	if [ "$$protocol" = "https" ]; then curl_args="$$curl_args -k"; fi; \
	curl $$curl_args "$$protocol://$$host:$$port/api/opinions/30000000-0000-0000-0000-000000000002" -H "Authorization: Bearer $$(cat .access_token)"

routed-request-opinion-approved: ## Gateway request to opinions (ENV=local|docker)
	@if [ ! -f .access_token ]; then $(MAKE) get-token ENV=$(ENV); fi
	@if [ "$(ENV)" = "docker" ]; then \
		$(LOAD_DOCKER_ENV) \
		protocol=https; \
		host=localhost; \
		port=8080; \
	else \
		$(LOAD_LOCAL_ENV) \
		protocol=$${INTERSERVICE_PROTOCOL:-http}; \
		host=$${GATEWAY_HOST:-localhost}; \
		port=$${GATEWAY_PORT:-8080}; \
	fi; \
	curl_args="-i"; \
	if [ "$$protocol" = "https" ]; then curl_args="$$curl_args -k"; fi; \
	curl $$curl_args "$$protocol://$$host:$$port/api/opinions/30000000-0000-0000-0000-000000000001" -H "Authorization: Bearer $$(cat .access_token)"

routed-request-mock: ## Gateway request to mock (ENV=local|docker)
	@if [ ! -f .access_token ]; then $(MAKE) get-token ENV=$(ENV); fi
	@if [ "$(ENV)" = "docker" ]; then \
		$(LOAD_DOCKER_ENV) \
		protocol=https; \
		host=localhost; \
		port=8080; \
	else \
		$(LOAD_LOCAL_ENV) \
		protocol=$${INTERSERVICE_PROTOCOL:-http}; \
		host=$${GATEWAY_HOST:-localhost}; \
		port=$${GATEWAY_PORT:-8080}; \
	fi; \
	curl_args="-i"; \
	if [ "$$protocol" = "https" ]; then curl_args="$$curl_args -k"; fi; \
	curl $$curl_args "$$protocol://$$host:$$port/api/opinion-lists/00000000-0000-0000-0000-000000000000/summary" -H "Authorization: Bearer $$(cat .access_token)"

routed-request-messaging-threads: ## Gateway request to messaging thread summaries (ENV=local|docker)
	@if [ ! -f .access_token ]; then $(MAKE) get-token ENV=$(ENV); fi
	@if [ "$(ENV)" = "docker" ]; then \
		$(LOAD_DOCKER_ENV) \
		protocol=https; host=localhost; port=8080; \
	else \
		$(LOAD_LOCAL_ENV) \
		protocol=$${INTERSERVICE_PROTOCOL:-http}; host=$${GATEWAY_HOST:-localhost}; port=$${GATEWAY_PORT:-8080}; \
	fi; \
	curl_args="-i"; \
	if [ "$$protocol" = "https" ]; then curl_args="$$curl_args -k"; fi; \
	curl $$curl_args "$$protocol://$$host:$$port/api/messaging/support/threads?page=0&size=10" \
		-H "Authorization: Bearer $$(cat .access_token)"

routed-request-user-profile: ## Gateway request to users-sv (ENV=local|docker)
	@if [ ! -f .access_token ]; then $(MAKE) get-token ENV=$(ENV); fi
	@if [ "$(ENV)" = "docker" ]; then \
		$(LOAD_DOCKER_ENV) \
		protocol=https; \
		host=localhost; \
		port=8080; \
	else \
		$(LOAD_LOCAL_ENV) \
		protocol=$${INTERSERVICE_PROTOCOL:-http}; \
		host=$${GATEWAY_HOST:-localhost}; \
		port=$${GATEWAY_PORT:-8080}; \
	fi; \
	curl_args="-i"; \
	if [ "$$protocol" = "https" ]; then curl_args="$$curl_args -k"; fi; \
	curl $$curl_args "$$protocol://$$host:$$port/api/users/00000000-0000-0000-0000-000000000000" -H "Authorization: Bearer $$(cat .access_token)"

routed-request-gdpr: ## Gateway GDPR export request with timeout (ENV=local|docker)
	@if [ ! -f .access_token ]; then $(MAKE) get-token ENV=$(ENV); fi
	@if [ "$(ENV)" = "docker" ]; then \
		$(LOAD_DOCKER_ENV) \
		protocol=https; \
		host=localhost; \
		port=8080; \
	else \
		$(LOAD_LOCAL_ENV) \
		protocol=$${INTERSERVICE_PROTOCOL:-http}; \
		host=$${GATEWAY_HOST:-localhost}; \
		port=$${GATEWAY_PORT:-8080}; \
	fi; \
	curl_args="-i"; \
	if [ "$$protocol" = "https" ]; then curl_args="$$curl_args -k"; fi; \
	\
	echo "=== Step 1: Starting GDPR export..."; \
	curl $$curl_args -X POST "$$protocol://$$host:$$port/api/export/start" \
		-H "Authorization: Bearer $$(cat .access_token)"; \
	echo ""; \
	\
	echo "=== Step 2: Polling for export status (timeout 30s)..."; \
	timeout=30; \
	while [ $$timeout -gt 0 ]; do \
		status=$$(curl $$curl_args "$$protocol://$$host:$$port/api/export/status" \
			-H "Authorization: Bearer $$(cat .access_token)" | grep -o '"status":"[^"]*"' | cut -d'"' -f4); \
		echo "Status: $$status ($$timeout s remaining)"; \
		if [ "$$status" = "COMPLETED" ]; then \
			break; \
		fi; \
		sleep 2; \
		timeout=$$((timeout - 2)); \
	done; \
	\
	if [ $$timeout -le 0 ]; then \
		echo "Timeout waiting for export to be ready"; \
		exit 1; \
	fi; \
	\
	echo "=== Step 3: Downloading export data..."; \
	curl $$curl_args "$$protocol://$$host:$$port/api/export/download" \
		-H "Authorization: Bearer $$(cat .access_token)" | head -c 500; \
	echo "..."

routed-request-opinion-list: ## Gateway request to smoke test opinion list (ENV=local|docker)
	@if [ ! -f .access_token ]; then $(MAKE) get-token ENV=$(ENV); fi
	@if [ "$(ENV)" = "docker" ]; then \
		$(LOAD_DOCKER_ENV) \
		protocol=https; host=localhost; port=8080; \
	else \
		$(LOAD_LOCAL_ENV) \
		protocol=$${INTERSERVICE_PROTOCOL:-http}; host=$${GATEWAY_HOST:-localhost}; port=$${GATEWAY_PORT:-8080}; \
	fi; \
	curl_args="-i"; \
	if [ "$$protocol" = "https" ]; then curl_args="$$curl_args -k"; fi; \
	curl $$curl_args "$$protocol://$$host:$$port/api/opinion-lists/80000000-0000-0000-0000-000000000000" \
		-H "Authorization: Bearer $$(cat .access_token)"

public-request-user: ## user-sv access through public api
	@if [ "$(ENV)" = "docker" ]; then \
		$(LOAD_DOCKER_ENV) \
		protocol=https; \
		host=localhost; \
		port=8080; \
	else \
		$(LOAD_LOCAL_ENV) \
		protocol=$${INTERSERVICE_PROTOCOL:-http}; \
		host=$${GATEWAY_HOST:-localhost}; \
		port=$${GATEWAY_PORT:-8080}; \
	fi; \
	curl_args="-i"; \
	if [ "$$protocol" = "https" ]; then curl_args="$$curl_args -k"; fi; \
	\
	curl $$curl_args -X GET "$$protocol://$$host:$$port/api/public/users/00000000-0000-0000-0000-000000000000" \
		-H "X-API-KEY: r8n_test-key_1234"

direct-request-opinion: ## HTTP direct request to opinions (local)
	@if [ ! -f .access_token ]; then $(MAKE) get-token; fi
	curl "http://localhost:8081/api/opinions/30000000-0000-0000-0000-000000000002" -i -H "Authorization: Bearer $$(cat .access_token)"

direct-request-mock: ## HTTP direct request to mock (local)
	@if [ ! -f .access_token ]; then $(MAKE) get-token; fi
	curl "http://localhost:8090/api/opinion-lists/00000000-0000-0000-0000-000000000000/summary" -i -H "Authorization: Bearer $$(cat .access_token)"

direct-request-swagger: ## HTTP direct request to users swagger (local)
	@if [ ! -f .access_token ]; then $(MAKE) get-token; fi
	curl "http://localhost:8082/v3/api-docs" -i -H "Authorization: Bearer $$(cat .access_token)"

# frontend
frontend-check-node: ## Check Node.js version (attempts nvm if too old)
	@FRONTEND_NODE_VERSION="$(FRONTEND_NODE_VERSION)" ./scripts/frontend-check-node.sh

frontend-build: frontend-install ## Build frontend dist (installs deps if missing)
	@bash -lc '$(LOAD_LOCAL_ENV) $(FRONTEND_SHELL) [ -d node_modules ] || frontend_npm ci; frontend_npm run build'

frontend-lint: frontend-check-node ## Run frontend lint
	@bash -lc '$(LOAD_LOCAL_ENV) $(FRONTEND_SHELL) frontend_npm run lint'

frontend-test-unit: frontend-check-node ## Run frontend unit tests
	@bash -lc '$(LOAD_LOCAL_ENV) $(FRONTEND_SHELL) frontend_npm run test:unit'

frontend-test: frontend-test-unit frontend-test-e2e ## Run all frontend tests

frontend-test-e2e: frontend-test-e2e-ui frontend-test-e2e-api ## Run all frontend E2E tests

frontend-test-e2e-ui: frontend-check-node ## Run frontend Playwright UI tests
	@bash -lc '$(LOAD_LOCAL_ENV) $(FRONTEND_SHELL) frontend_npm run test:e2e -- --project ui'

frontend-test-e2e-api: frontend-check-node ## Run frontend Playwright API tests
	@bash -lc '$(LOAD_LOCAL_ENV) $(FRONTEND_SHELL) frontend_npm run test:e2e -- --project api'

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

##@ CI check stages
check-makefile: ## Lint Makefile formatting and conflicts
	chmod +x utils/lint-makefile.sh
	./utils/lint-makefile.sh

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
	$(LOAD_LOCAL_ENV) \
	cd frontend && npm run type-check && npx eslint . --max-warnings=0 && npm run test:unit -- --run && npm run build-only

test-e2e:
	$(LOAD_LOCAL_ENV) \
	cd frontend && npm run test:e2e

##@ entrypoints
help: ## Show this help
	@awk 'BEGIN {FS=":.*##"} /^##@/ {printf "\n%s:\n", substr($$0,5)} /^[a-zA-Z0-9_.%-]+:.*##/ {printf "  %-32s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

all: docker-up ## Default target

clean: clean-artifacts clean-logs frontend-clean ## Remove backend artifacts/logs and frontend cache

fclean: clean frontend-clean-all ## Remove clean plus frontend node_modules and certs

re: fclean docker-build ## Full rebuild (clean + docker-build)
