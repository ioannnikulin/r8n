# Architecture Overview

This document describes the high-level architecture of the r8n application, a microservices-based platform built with Spring Boot, Kotlin, and React.

## System Overview

r8n is structured as a **microservices architecture** with 5 backend services, a PostgreSQL database, and a React frontend. All services communicate via HTTP or HTTPS depending on the environment.

```
                              ┌─────────────────────────────────────────────────┐
                              │                  Gateway-sv                     │
  Frontend (Nginx/Vite)  ───▶ │              local: 8080 / docker: 8080         │
  local:  http://5173         └──────┬──────────┬──────────┬──────────┬─────────┘
  docker: https://80,443             │          │          │          │
                                     ▼          ▼          ▼          ▼
                              opinions-sv   users-sv  messaging-sv  migration-sv
                              :8081         :8082     :8084          :8083
                                     │          │
                                     ▼          ▼
                              mock-sv        PostgreSQL
                              :8090          :5432
                                             (schemas: opinions, users, messaging)
```

**Ports (local HTTP / Docker internal HTTPS):**

| Service       | Local port | Docker port |
|---------------|-----------|-------------|
| gateway-sv    | 8080      | 8080 (exposed to host) |
| opinions-sv   | 8081      | 8080 (internal) |
| users-sv      | 8082      | 8080 (internal) |
| migration-sv  | 8083      | 8080 (internal) |
| messaging-sv  | 8084      | 8080 (internal) |
| mock-sv       | 8090      | 8080 (internal) |
| database      | 5432      | 5432 (exposed to host) |
| frontend      | 5173 (Vite) | 80 / 443 (Nginx) |

**Protocol:**
- Local: HTTP for all services (`SERVER_SSL_ENABLED=false`)
- Docker: HTTPS with TLS for all services (`SERVER_SSL_ENABLED=true`)

## Backend Architecture

### Tech Stack

- **Language**: Kotlin 2.2.21
- **Framework**: Spring Boot 4.0.4
- **Build Tool**: Gradle 9.3.0 with Kotlin DSL
- **Database**: PostgreSQL 15
- **Security**: TLS 1.2+, JWT tokens (when SSL enabled)
- **Architecture Pattern**: API-First Design, Multi-module Microservices

### Multi-Module Structure

The backend is organized as a Gradle multi-module project with the following structure:

```
backend/
├── settings.gradle.kts          # Module definitions
├── build.gradle.kts             # Root build configuration
├── build-logic/                  # Gradle convention plugins
├── platform/                     # Platform-wide BOM (dependency versions)
├── core/                         # Shared libraries
│   ├── api/                      # Common API types (pagination, etc.)
│   ├── security-common/          # Shared security utilities
│   ├── security-reactive/        # Reactive (WebFlux) security — used by gateway
│   ├── security-servlet/         # Servlet security — used by data services
│   ├── utils/                    # Common utilities
│   └── web/                      # Shared web config
│
├── gateway-sv/                   # API Gateway Service
│   └── service/
│
├── opinions-sv/                  # Opinions Service
│   ├── api/                      # Public API contracts & DTOs
│   ├── api-integration/          # Internal contracts (for other services)
│   ├── client/                   # REST clients for other services
│   └── service/
│       └── src/main/kotlin/.../opinions/
│           ├── access/           # Access control domain
│           │   ├── controller/
│           │   ├── database/
│           │   ├── domain/
│           │   ├── facade/
│           │   ├── persistence/
│           │   └── service/
│           ├── lists/            # Opinion lists domain
│           │   ├── controller/
│           │   ├── controllerInternal/
│           │   ├── database/
│           │   ├── domain/
│           │   ├── facade/
│           │   ├── persistence/
│           │   └── service/
│           └── opinions/         # Core opinions domain
│               ├── controller/
│               ├── database/
│               ├── domain/
│               ├── facade/
│               ├── persistence/
│               └── service/
│
├── users-sv/                     # Users Service
│   ├── api/                      # Public API contracts & DTOs
│   ├── api-integration/          # Internal contracts
│   ├── client/                   # REST clients
│   └── service/
│
├── messaging-sv/                 # Messaging Service
│   ├── api/                      # Public API contracts & DTOs
│   ├── api-integration/          # Internal contracts
│   ├── client/                   # REST clients
│   └── service/
│
├── migration-sv/                 # Migration Service (GDPR export/import)
│   ├── api/                      # Public API contracts & DTOs
│   └── service/
│
└── mock-sv/                      # Mock/Support Service
    ├── api/                      # Mock API contracts
    ├── api-integration/          # Internal contracts
    ├── client/                   # HTTP clients
    └── service/
        └── src/main/kotlin/.../mock/
            ├── controller/       # Stub controllers
            └── stub/             # Test data factories
```

### Microservices

#### 1. Gateway Service (`gateway-sv`)
- **Local Port**: 8080 (HTTP)
- **Docker Port**: 8080 (HTTPS with TLS)
- **Role**: API Gateway, routing, TLS termination
- **Routes requests to**: opinions-sv, users-sv, messaging-sv, migration-sv, mock-sv
- **HTTP/HTTPS**: Configured via `SERVER_SSL_ENABLED` environment variable
  - Local: `false` (HTTP only)
  - Docker: `true` (HTTPS only)

#### 2. Opinions Service (`opinions-sv`)
- **Local Port**: 8081 (HTTP)
- **Docker Port**: 8080 (HTTPS internally)
- **Role**: Core business logic for opinions
- **Database**: PostgreSQL `opinions` schema
- **Key Entities**: Opinion, OpinionSubject, Referent, OpinionNote, WeightedOpinionReference
- **HTTP/HTTPS**: Uses `INTERSERVICE_PROTOCOL` (http=local, https=Docker)
- **Packages**: `api/`, `api-integration/`, `client/` for API contracts and REST clients

#### 3. Mock Service (`mock-sv`)
- **Local Port**: 8090 (HTTP)
- **Docker Port**: 8080 (HTTPS internally)
- **Role**: Provides stub/test data for development and testing
- **Mocks**: Selectors, Recommendations
- **HTTP/HTTPS**: Uses `INTERSERVICE_PROTOCOL` (http=local, https=Docker)
- **Packages**: `api/`, `client/` for API contracts and REST clients

#### 4. Users Service (`users-sv`)
- **Local Port**: 8082 (HTTP)
- **Docker Port**: 8080 (HTTPS internally)
- **Role**: User management, profiles, authentication
- **Database**: PostgreSQL `users` schema
- **Key Features**: User profiles, avatar upload, authentication endpoints
- **HTTP/HTTPS**: Uses `INTERSERVICE_PROTOCOL` (http=local, https=Docker)
- **Packages**: `api/`, `api-integration/`, `client/` for API contracts and REST clients

#### 5. Migration Service (`migration-sv`)
- **Local Port**: 8083 (HTTP)
- **Docker Port**: 8080 (HTTPS internally)
- **Role**: Data export and import (GDPR data portability), async job processing
- **Database**: none (orchestrates data from other services)
- **Key Features**: Data export jobs, data import, user data compilation
- **HTTP/HTTPS**: Uses `INTERSERVICE_PROTOCOL` (http=local, https=Docker)
- **Packages**: `api/` for public contracts, `service/` for implementation

#### 6. Messaging Service (`messaging-sv`)
- **Local Port**: 8084 (HTTP)
- **Docker Port**: 8080 (HTTPS internally)
- **Role**: User messaging and thread management
- **Database**: PostgreSQL `messaging` schema
- **Key Features**: Message threads, direct messaging between users
- **HTTP/HTTPS**: Uses `INTERSERVICE_PROTOCOL` (http=local, https=Docker)
- **Packages**: `api/`, `api-integration/`, `client/` for API contracts and REST clients

### API-First Design Pattern

Each service follows an **API-first design**: the API contracts are defined separately from their implementations.

**Example for opinions-sv:**
```
opinions-sv/
├── api/ # Public API contracts (exposed to clients)
│   ├── OpinionApi.kt # Interface with @RequestMapping
│   └── dto/ # Data Transfer Objects
│       └── opinion/OpinionDto.kt
│
├── api-integration/ # Internal API contracts (for other services)
│   └── OpinionInternalApi.kt
│
├── client/ # REST clients to call other services
│   └── OpinionRestClient.kt
│
└── service/ # Implementation
    └── controller/
        └── OpinionController.kt
```

**Benefits:**
- Clear contract between services
- Can generate API documentation from interfaces
- Easy to implement mock/stub versions
- Frontend can develop against API contracts

### Design Patterns Used

1. **API-First**: Separate API contracts from implementation
2. **Facade Pattern**: Merges domain models with DTOs from other microservices and converts everything into the service's DTOs. Calls the service layer which creates domain entities and implements business logic (calculating data relying only on objects owned by the current service). The repository+persistence layer handles database interaction without modifying objects.
3. **Repository Pattern**: Data access abstraction
4. **Dependency Injection**: Via Spring Framework
5. **DTO Pattern**: Separate API models from domain entities

### Database Architecture

**PostgreSQL with Multi-Schema Design:**
```
r8n_db/
├── opinions (schema)
│   ├── opinions              (id, owner, subject, mark, status, timestamp)
│   ├── subjects              (id, name, referent)
│   ├── referents             (id, name, address, latitude, longitude, referent_group)
│   ├── opinion_notes         (id, opinion_id, type, description)
│   └── weighted_opinion_references (id, parent_opinion, child_opinion, weight)
│
├── users (schema)
│   ├── users                 (id, status, status_timestamp, password_hash)
│   ├── pii                   (user_id, name, email, phone)
│   ├── sessions              (id, user_id, created, expires, ip, user_agent)
│   ├── consents              (id, user_id, type, accepted, session)
│   ├── users_role_assignments (id, user, role, granted_by, timestamp)
│   └── refresh_tokens        (id, token_id, user_id, parent_id, issued_at, expires_at, revoked, used)
│
└── messaging (schema)
    ├── support_threads       (id, owner_user_id)
    ├── support_messages      (id, thread_id, author_user_id, author_role, text, created_at)
    ├── conversations         (id, type, created_by_user_id, created_at, last_message_at)
    └── conversation_participants (id, conversation_id, user_id, participant_role, joined_at, archived_at, last_read_at)
```

**Database Configuration:**
- Connection pooling via HikariCP
- Liquibase migrations per service (`backend/<service>/service/src/main/resources/db/changelog/`)
- Separate schemas per service for isolation
- Connection via environment variables

### Security Architecture

1. **TLS/SSL** in Docker: All inter-service communication encrypted (TLS 1.2+)
2. **HTTP** in local deployments: Plain HTTP for simplicity
3. **Authentication**: JWT Bearer tokens
4. **Certificate Generation**: TLS certificates generated on build machine before Docker deployment

### Port & Protocol Configuration

**Environment determined:**
- **Local (make local-run-all)**: HTTP only
  ```
  INTERSERVICE_PROTOCOL=http
  SERVER_SSL_ENABLED=false
  Ports: 8080, 8081, 8082, 8083, 8084, 8090
  ```

- **Docker (make docker-up)**: HTTPS only
  ```
  INTERSERVICE_PROTOCOL=https
  SERVER_SSL_ENABLED=true
  Ports: 8080 (HTTPS), 8443 (HTTPS), database:5432
  ```

## Frontend Architecture

### Tech Stack

- **Language**: TypeScript 5.8.3
- **Framework**: Vite 8.0.1 + React 18.3.1
- **Architecture**: Component/feature-organized structure
- **Styling**: CSS/SASS (custom design system)

### Directory Structure

```
frontend/
└── src/
    ├── assets/               # Static assets (images, fonts, icons)
    ├── components/           # Reusable UI components
    │   ├── auth/             # Auth-related components
    │   ├── layout/           # Layout components (AppSidebar, etc.)
    │   ├── server-state/     # Data-fetching wrapper components
    │   └── ui/               # Generic UI primitives
    ├── hooks/                # Custom React hooks
    ├── lib/                  # Utilities and integrations
    │   ├── api/              # API client functions per domain
    │   ├── auth/             # Authentication logic
    │   ├── e2e/              # E2E test bootstrap helpers
    │   └── server-state/     # TanStack Query setup
    │       └── hooks/        # Query/mutation hooks per domain
    ├── pages/                # Page components (route targets)
    └── test/                 # Unit and component tests
```

## Infrastructure

### Docker Deployment

**Services (docker-compose.yml):**
1. **database** (PostgreSQL 15)
   - Port: 5432 (exposed to host)
   - Volume: `postgres_data` (stored in repository folder under `deployment/database/data/`)

2. **opinions** (Spring Boot)
   - Port: 8080 (internal HTTPS only)
   - TLS: `/certs/keystore-opinions.p12`
   - Depends on: database

3. **mock** (Spring Boot)
   - Port: 8080 (internal HTTPS only)
   - TLS: `/certs/keystore-mock.p12`

4. **messaging** (Spring Boot)
   - Port: 8080 (internal HTTPS only)
   - TLS: `/certs/keystore-messaging.p12`
   - Depends on: database

5. **users** (Spring Boot)
   - Port: 8080 (internal HTTPS only)
   - TLS: `/certs/keystore-users.p12`
   - Depends on: database

6. **migration** (Spring Boot)
   - Port: 8080 (internal HTTPS only)
   - TLS: `/certs/keystore-migration.p12`
   - Depends on: database + all services (healthy)

7. **gateway** (Spring Boot)
   - Port: `GATEWAY_HOST_PORT` → `GATEWAY_CONTAINER_PORT` (HTTPS, externally accessible)
   - TLS: `/certs/keystore-gateway.p12`
   - Routes to all backend services
   - Depends on: all services (healthy)

8. **frontend** (Nginx)
   - Ports: `FRONTEND_HTTP_HOST_PORT` → 80, `FRONTEND_HTTPS_HOST_PORT` → 443
   - Serves the React application, proxies `/api/**` to gateway
   - Depends on: gateway (healthy)

### Docker Port Mapping Summary

```
Host                        ⇄  Container
────────────────────────────────────────────────
FRONTEND_HTTP_HOST_PORT     ⇄  frontend:80
FRONTEND_HTTPS_HOST_PORT    ⇄  frontend:443
GATEWAY_HOST_PORT           ⇄  gateway:GATEWAY_CONTAINER_PORT (HTTPS)
localhost:5432              ⇄  database:5432 (PostgreSQL)

Internal only (not exposed to host):
  gateway → opinions:8080   (HTTPS)
  gateway → users:8080      (HTTPS)
  gateway → messaging:8080  (HTTPS)
  gateway → migration:8080  (HTTPS)
  gateway → mock:8080       (HTTPS)
```

### Local Development

**Without Docker:**
```bash
make local-run-all  # Starts all backend services (opinions, users, messaging, migration, mock, gateway) via HTTP
make local-stop-all # Stops all services
```

**With Docker:**
```bash
make docker-up   # Build and start all containers via HTTPS
make docker-down # Stop and remove containers
```

### Environment Configuration

**Key Environment Files** (not committed to the repository — must be created locally):
- `deployment/config/docker.env` - Docker environment (HTTPS)
- `deployment/config/local.env` - Local development (HTTP)
- `deployment/secrets/docker.secrets.env` - Secrets (TLS passwords)

### Network Configuration

- All services connect via Docker network `r8n_net`
- Internal service resolution via service names
- Gateway routes:
  - `/api/auth/**` → users-sv
  - `/api/opinions/**`, `/api/access-requests/**`, `/api/opinion-lists/**`, `/api/subjects/**`, `/api/referents/**` → opinions-sv
  - `/api/users/**`, `/api/admin/**` → users-sv
  - `/api/messaging/**` → messaging-sv
  - `/api/export/**`, `/api/import/**` → migration-sv
  - `/api/selectors/**` → mock-sv

### Make Targets

**Service Management:**
- `make local-run-all` - Start all services locally (HTTP)
- `make local-stop-all` - Stop all services
- `make docker-up` - Docker build and run (HTTPS)
- `make docker-down` - Docker stop

**Testing:**
- `make direct-request-opinion` - Test opinions service directly
- `make routed-request-opinion` - Test via gateway
- `make direct-request-mock` - Test mock service directly

**Database:**
- `make docker-database-run` - Start only database
- `make docker-database-connect` - Connect to DB with psql

## Communication Patterns

### External → Gateway → Services

```
Client (Browser)
    ↓ HTTPS (Docker) or HTTP (local)
Gateway (Port 8080)
    ↓ HTTPS (Docker) or HTTP (local)
    ├──┬─→ Opinions (Port 8081 local / 8080 Docker)
    │
    └─→ Mock (Port 8090 local / 8080 Docker)
```

### Protocol Configuration

**Protocol controlled by environment variables:**
- `INTERSERVICE_PROTOCOL` - Protocol for service-to-service communication
  - Local: `http` (deployment/config/local.env)
  - Docker: `https` (deployment/config/docker.env)

- `SERVER_SSL_ENABLED` - Whether service listens via HTTPS
  - Local: `false` (HTTP)
  - Docker: `true` (HTTPS)

### Authentication Flow

1. Client includes JWT token: `Authorization: Bearer <token>`
2. Gateway validates token or passes through
3. Services validate JWT tokens; integration with OAuth2/Auth0 planned for production
4. Production: integrate with OAuth2/Auth0

### API Examples

For local HTTP requests, use the Makefile targets (see `make help` for full list):

**Local Development (HTTP):**
```bash
make routed-request-opinion  # Via Gateway
make routed-request-mock     # Opinions list via Gateway
```

**Docker/Production (HTTPS):**
```bash
make https-routed-request-opinion  # Via Gateway (HTTPS)
make https-routed-request-mock     # Opinions list via Gateway (HTTPS)
```
## Data Flow Example

### Request Flow: Get Opinion by ID

1. **Client** → GET `/opinions/{id}` to **Gateway**
2. **Gateway** → Routes to **Opinions Service** (TLS in Docker, plaintext in local)
3. **Opinions Controller** → Validates input, calls `OpinionFacade`
4. **OpinionFacade** → Orchestrates this service with other services (merges DTOs, converts to service DTOs), prepares response DTO
5. **Controller** → Returns data to Gateway → Client

### Mock Flow: Get Selectors

1. **Client** → GET `/selectors` to **Gateway**
2. **Gateway** → Routes to **Mock Service** (TLS in Docker, plaintext in local)
3. **Mock Controller** → Returns stub data from **Data Factory**
4. **Response** → Returns static/test data

## Data Flow Example (Docker/HTTPS Mode)

```
Request: https://localhost:8080/opinions/123
         │
         ▼
┌─────────────────────────────────┐
│ Gateway Service (Port 8080)    │
│ HTTPS (TLS)                     │
└─────────────────────────────────┘
         │
         │ https://opinions:8080
         │ (HTTPS, internal)
         ▼
┌─────────────────────────────────┐
│ Opinions Service (Port 8080)   │
│ HTTPS (TLS)                     │
└─────────────────────────────────┘
         │
         │ PostgreSQL (Port 5432)
         │ (plaintext localhost)
         ▼
┌─────────────────────────────────┐
│ PostgreSQL Database             │
│ opinions schema                 │
└─────────────────────────────────┘
```

## Future Architecture Extensions

Based on **42 requirements** (noted as "?" in requirements doc):

- ✅ Implemented: Microservices, ORM, standard auth, PWA support
- 🔄 Planned/TBD:
  - OAuth 2.0 integration
  - Machine learning recommendation system
  - Content moderation AI
  - ELK stack for logging
  - Prometheus + Grafana monitoring
  - Health check & status page
  - Multi-language support (i18n)
  - Playwright for cross-browser testing

## References

- **Backend Developer Onboarding** (VSCode): `docs/backend_developer_onboarding_VSCode.md`
- **Backend Developer Onboarding** (IntelliJ): `docs/backend_developer_onboarding_IntelliJIDEA.md`
- **Frontend Developer Onboarding**: `docs/frontend_developer_onboarding.md`
- **42 Requirements**: `docs/42_points_requirements.md`
- **Docker Compose**: `docker-compose.yml`
- **Service List**: `deployment/config/services.list`
