_This project has been created as part of the 42 curriculum by dbisko, iatopchu, inikulin, lshapkin, mkulikov._

# r8n - Microservices Platform

## Description

**You don't need everyone's opinion. You need the right people's opinion.**

**r8n** platform lets you build a personal network of trusted reviewers — friends, food critics you follow, colleagues with similar taste — and see only their honest takes on restaurants and cafés. Reviews are private by design, shared only on request, which means they're protected under German law and can never be forced down.

**r8n** is a scalable microservices-based platform built with Spring Boot, Kotlin, and React, designed to demonstrate modern software architecture patterns and distributed systems principles. The platform implements a multi-service architecture with an API Gateway and core business logic services, all communicating via HTTP/HTTPS with TLS encryption.

### Goals
- Demonstrate microservices architecture patterns
- Implement API-first design methodology
- Create a feature-rich, interactive web application
- Apply modern development practices (CI/CD, containerization, component-organized frontend)
- Fulfill 42 curriculum requirements for web development

### Key Features
- **Multi-service Backend**: Gateway, Opinions, Users, Messaging, Migration, and Mock services with clear separation of concerns
- **API-First Design**: Separate API contracts from implementation for clear service boundaries
- **Secure Communication**: TLS/SSL encryption between services
- **Database Management**: PostgreSQL with isolated schemas per service
- **Modern Frontend**: React application with TypeScript, TanStack Query for server state
- **Containerized Deployment**: Docker Compose setup for easy local development and production
- **Developer Experience**: Makefile targets for common operations, IDE setup guides

## Instructions

### Prerequisites

**Backend:**
- JDK 21 (`java --version` should show 21.x)
- Docker & Docker Compose (for containerized deployment)
- Make (for build automation)

**Frontend:**
- Node.js 22.13.0 or higher (`node -v` should show >= 22.13.0)
- npm (comes with Node.js)

**Version Control:**
- Git

> **⚠️ 42 Campus machines:** Campus machines do not have enough disk space to build or deploy this project directly from your home directory.
>
> **Setup steps for campus:**
> 1. Clone the repository into `/sgoinfre/<your_login>/` — not into `$HOME`
> 2. Run `make move-caches-to-goinfre` to move Docker and Gradle caches out of `$HOME` to `/goinfre`
> 3. Copy the env template and set your database password:
>    ```bash
>    cp deployment/config/.env.example deployment/config/docker.env
>    # Edit docker.env and replace the database password with your own
>    ```
> 4. Generate JWT keys:
>    ```bash
>    make generate-jwt-keys-docker
>    ```
> 5. Start the project:
>    ```bash
>    make
>    ```
>
> Check required disk space with `make who-ate-all-the-space`.

### Installation

**1. Clone the repository:**
```bash
git clone <repository-url>
cd r8n
```

**2. Backend setup:**
```bash
cd backend
./gradlew build
cd ..
```

**3. Frontend setup:**
```bash
cd frontend
npm ci
cd ..
```

**4. Environment configuration:**
- For Docker: Copy and configure `deployment/config/docker.env` and `deployment/secrets/docker.secrets.env`
- For local: Ensure `deployment/config/local.env` exists (it should by default)

### Running the Application

#### Option 1: Local Development (HTTP - No SSL)

**Start backend services:**
```bash
# In terminal 1
make local-run-all
```

**Start frontend:**
```bash
# In terminal 2
cd frontend && npm run dev
```

**Access:**
- Frontend: http://localhost:5173
- Gateway API: http://localhost:8080
- Opinions service: http://localhost:8081

#### Option 2: Docker/Production (HTTPS with TLS)

**Build and start all services:**
```bash
make docker-up
```

**Access:**
- Frontend: https://localhost (Nginx on port 443)
- Gateway API: https://localhost:8080
- Other services: Internal only (HTTPS)

**Stop services:**
```bash
make docker-down
```

### Database Setup

**Create database schema:**
```bash
make docker-database-run
```

**Connect to database:**
```bash
make docker-database-connect
# Then: \c r8n
# Then: \dt (to see tables)
```

### Testing API Endpoints

Use the make rules to test API endpoints (documented in Make Commands Reference below).

### Make Commands Reference

**Docker:**
- `make docker-up` - Build and start all containers (builds images, generates certs)
- `make docker-down` - Stop Docker stack
- `make docker-build` - Build all Docker images without starting
- `make build-<service>` - Rebuild one Docker service (e.g. `make build-opinions`)
- `make restart-<service>` - Rebuild and restart one service
- `make docker-logs` - Tail logs for all services

**Local Backend:**
- `make local-run-all` - Start all backend services locally (opinions, users, messaging, migration, mock, gateway)
- `make local-stop-all` - Stop all local services

**Frontend:**
- `make frontend-dev` - Start Vite dev server
- `make frontend-build` - Build frontend dist
- `make frontend-lint` - Run linter
- `make frontend-test-unit` - Run unit tests
- `make frontend-test-e2e` - Run Playwright E2E tests
- `make frontend-clean` - Remove build output and cache

**Database:**
- `make docker-database-run` - Start only the database container
- `make docker-database-connect` - Connect to database with psql
- `make docker-database-drop-volume-personal` - Delete local DB volume (personal machine)
- `make docker-database-drop-volume-campus` - Delete local DB volume (campus machine)

**Certificates:**
- `make docker-certs` - Generate TLS certs (skips if valid)
- `make docker-certs-force` - Force regenerate all TLS certs
- `make edge-certs` - Generate frontend HTTPS certs
- `make generate-jwt-keys-<env>` - Generate RSA JWT keypair (e.g. `make generate-jwt-keys-local`)

**Artifacts:**
- `make prebuild-jars` - Build all backend bootJar artifacts
- `make prepare-artifacts` - Copy JARs into deployment folders
- `make gradle-<service>-bootJar` - Build one service JAR

**Smoke Tests:**
- `make get-token` - Obtain JWT token (ENV=local|docker)
- `make refresh-token` - Refresh JWT token
- `make logout` - Logout and clear cookies
- `make routed-request-opinion` - Test opinion endpoint via gateway
- `make direct-request-opinion` - Test opinions service directly
- `make routed-request-user-profile` - Test users endpoint
- `make routed-request-messaging-threads` - Test messaging endpoint
- `make routed-request-gdpr` - Test GDPR export
- `make routed-import-gdpr` - Test GDPR import

**Cleanup:**
- `make clean` - Remove backend artifacts, logs, and frontend cache
- `make fclean` - Full clean including node_modules and certs
- `make re` - Full rebuild (clean + docker-build)

**42 Campus Utilities:**
- `make move-caches-to-goinfre` - Move Docker/Gradle caches to `/goinfre`
- `make who-ate-all-the-space` - Show top-level disk usage
- `make clean-the-fuck-out-of-this-campus-machine` - Remove large local caches

## Resources

### Official Documentation
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [React Documentation](https://react.dev/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Docker Documentation](https://docs.docker.com/)

### Tutorials & Guides
- [TanStack Query Documentation](https://tanstack.com/query/latest)
- [API-First Design Pattern](https://swagger.io/resources/articles/adopting-an-api-first-approach/)
- [Microservices Patterns](https://microservices.io/patterns/)

### 42 Curriculum References
- [42 (school)](https://www.42.us.org/)
- [42 Networking & Web Development Track](https://www.42.us.org/)

### AI Usage Documentation

AI was used throughout the development process as an active coding assistant, not just for documentation.

**Development workflow:**
1. Ask AI to propose an idiomatic solution for a given problem
2. Read, understand, and make design choices
3. Implement a vertical stack manually, defining all layers
4. Ask AI for code review and suggestions
5. Iterate on the implementation
6. Ask AI to apply the same pattern to other components

**Specific tasks where AI assistance was used:**
- **Architecture documentation**: Analysis of multi-module structure, creation of ARCHITECTURE.md
- **Git workflow recovery**: Removing accidentally committed binary database files
- **README restructuring**: Restructuring and expansion to meet 42 curriculum requirements

## Team Information

### Team Structure

**Backend Team:**
- **inikulin** - Project Owner, Tech Lead, Backend Developer
  - Led architecture design, implemented Gateway and Opinions services
  - Database design, API contracts
- **iatopchu** - Backend Developer
  - Backend service development, business logic implementation
- **lshapkin** - Backend Developer
  - Backend service development, testing

**Frontend Team:**
- **mkulikov** - Project Manager, Frontend Lead, UI/UX Developer
  - Frontend architecture, React development, design system
- **dbisko** - Frontend Developer
  - React component development, API integration

## Project Management

### Work Organization

**Task Distribution:**
- Clear separation of concerns (backend team, frontend team, devops)
- Individual ownership of services (Gateway, Opinions, Frontend)

**Meeting Structure:**
- Ad-hoc sync sessions as needed
- Technical review sessions for architecture decisions

### Tools Used

**Project Management:**
- GitHub repository for version control
- GitHub Issues for task tracking

**Communication:**
- Primary channel: Slack
- Video calls: Google Meet
- Documentation: Notion

**Development Environment:**
- Code hosting: GitHub
- CI/CD: GitHub Actions
- Container registry: Local Registry

### Branch Strategy
- Main branch: `master`
- Feature branches: created per GitHub issue, named `<issue-number>-<short-description>`
- Workflow: create issue → branch → implement → open PR → code review → merge to master

## Technical Stack

### Backend Technologies

**Language & Framework:**
- **Kotlin 2.2.21** - Modern, concise, null-safe
- **Spring Boot 4.0.4** - Microservice framework
- **Gradle 9.3.0 with Kotlin DSL** - Build automation

**Justification:**
- Kotlin provides excellent interop with Java ecosystem
- Spring Boot offers battle-tested microservice patterns
- Gradle Kotlin DSL provides type-safe build configuration

**Database:**
- **PostgreSQL 15** - Relational database with JSONB support
- **HikariCP** - Connection pooling
- **Spring Data JPA** - ORM abstraction
- **Liquibase** - Schema migrations

**Justification:**
- PostgreSQL offers robust ACID (Atomicity, Consistency, Isolation, Durability) guarantees and excellent performance
- Separate schemas provide clean service isolation
- Spring Data JPA simplifies data access patterns

**Security:**
- **TLS 1.2+** - Transport layer encryption
- **JWT tokens** - Authentication
- **Spring Security** - Security framework

**Communication:**
- HTTP/HTTPS for inter-service communication
- Spring Cloud Gateway for routing
- RESTful APIs with JSON

### Frontend Technologies

**Language & Framework:**
- **TypeScript 5.8.3** - Type-safe JavaScript
- **React 18.3.1** - Component library
- **Vite 8.0.1** - Build tool and dev server

**Justification:**
- TypeScript catches errors at compile time
- React provides excellent ecosystem and performance
- Vite offers fast HMR and optimized builds

**Styling:**
- **Custom CSS/SASS** - Design system implementation
- **Component-scoped styling**

**State Management:**
- **React hooks** - Local component state
- **TanStack Query 5** - Server state, caching, and data fetching

### Infrastructure & DevOps

**Containerization:**
- **Docker** - Container runtime
- **Docker Compose** - Multi-container orchestration

**Scripting & Automation:**
- **Make** - Build automation
- **Shell scripts** - Environment setup

**Monitoring & Observability:**
- **Log files** (file-based)
- **Health check endpoints** (future)

**Development Tools:**
- **Git** - Version control
- **GitHub** - Code hosting
- **IDE**: IntelliJ IDEA or VS Code (recommendations in docs)

## Database Schema

### PostgreSQL Structure

Database: `r8n`, three schemas:

```
opinions schema:
  opinions                    (id uuid PK, owner uuid, subject uuid→subjects, mark double, status varchar, timestamp timestamptz)
  subjects                    (id uuid PK, name varchar, referent uuid→referents)
  referents                   (id uuid PK, name varchar, address text, latitude double, longitude double, referent_group uuid)
  opinion_notes               (id uuid PK, opinion_id uuid→opinions, type varchar, description text)
  weighted_opinion_references (id uuid PK, parent_opinion uuid→opinions, child_opinion uuid→opinions, weight double)

users schema:
  users                 (id uuid PK, status varchar, status_timestamp timestamptz, password_hash varchar)
  pii                   (user_id uuid PK→users, name varchar, email varchar, phone varchar)
  sessions              (id uuid PK, user_id uuid→users, created timestamptz, expires timestamptz, ip varchar, user_agent text)
  consents              (id uuid PK, user_id uuid→users, type varchar, accepted timestamptz, session uuid→sessions)
  users_role_assignments (id uuid PK, user uuid→users, role varchar, granted_by uuid→users, timestamp timestamptz)
  refresh_tokens        (id uuid PK, token_id uuid, user_id uuid→users, parent_id uuid, issued_at timestamptz, expires_at timestamptz, revoked boolean, used boolean)

messaging schema:
  support_threads            (id uuid PK, owner_user_id uuid)
  support_messages           (id uuid PK, thread_id uuid→support_threads, author_user_id uuid, author_role varchar, text text, created_at timestamptz)
  conversations              (id uuid PK, type varchar, created_by_user_id uuid, created_at timestamptz, last_message_at timestamptz)
  conversation_participants  (id uuid PK, conversation_id uuid→conversations, user_id uuid, participant_role varchar, joined_at timestamptz, archived_at timestamptz, last_read_at timestamptz)
```

### Relationships

**opinions schema:**
- **opinions** → **subjects**: Many-to-One (each opinion has one subject)
- **subjects** → **referents**: Many-to-One (each subject links to one referent — e.g. a restaurant)
- **opinions** → **opinion_notes**: One-to-Many
- **opinions** → **weighted_opinion_references**: One-to-Many (as parent and as child — opinions reference other opinions with a weight)

**users schema:**
- **users** → **pii**: One-to-One
- **users** → **sessions**: One-to-Many
- **users** → **refresh_tokens**: One-to-Many
- **users** → **users_role_assignments**: One-to-Many

**messaging schema:**
- **support_threads** → **support_messages**: One-to-Many
- **conversations** → **conversation_participants**: One-to-Many

### Index Strategy
- Primary keys: UUID v7 (`@UuidGenerator(style = VERSION_7)`)
- Foreign key indexes on join columns

### Migration Management
- Managed per-service via **Liquibase** (changelogs in `backend/<service>/service/src/main/resources/db/changelog/`)
- Each service owns its own schema and migrations independently

## Features List

### Implemented Features

**Authentication & Authorization:**
- JWT token-based authentication
- Authorization header validation
- Basic security middleware

**Opinions Management:**
- Create/read/update/delete opinions
- Opinion status tracking (draft, published, archived)
- Opinion note attachments
- Subject categorization

**Data Access Control:**
- Incoming access request management
- Outgoing access request tracking
- Request status workflows (sent, accepted, rejected, hidden)

**Business Logic:**
- Weighted opinion calculations
- Referent linking system
- Opinion list organization
- Selector-based overlay display

**Testing & Development:**
- Comprehensive test data factories
- Local development HTTP mode
- Docker HTTPS production mode

**Frontend:**
- Component-organized React application
- Responsive UI components
- API integration layer (per-domain functions in `lib/api/`)
- Server state management with TanStack Query

## Modules

### Selected Modules (Based on 42 Requirements)

#### Major Modules (2 points each)

**1. Use a framework for both the frontend and backend**

- **Points**: 2 points (Major module)
- **Description**: Use a frontend framework (React, Vue, Angular, Svelte, etc.) and a backend framework (Express, NestJS, Django, Flask, Ruby on Rails, etc.)
- **Justification**: Frameworks provide structured, maintainable codebases with built-in best practices, reducing development time and ensuring consistency across the project. Using established frameworks allows the team to focus on business logic rather than reinventing the wheel.
- **Implementation**:
  - **Frontend**: React 18.3.1 with TypeScript 5.8.3, Vite 8.0.1 as build tool
  - **Backend**: Spring Boot 4.0.4 with Kotlin 2.2.21, using Spring Cloud Gateway for API routing and Spring Data JPA for database access
  - Both frameworks chosen for their strong ecosystems, type safety, and industry adoption
- **Team Members**: All team members (dbisko, iatopchu, inikulin, lshapkin, mkulikov)

**2. Allow users to interact with other users**

- **Points**: 2 points (Major module)
- **Description**: A basic chat system (send/receive messages between users) and a profile system (view user information)
- **Justification**: User interaction features are essential for building a social platform where users can connect, share opinions, and build trusted networks. These features enable the core value proposition of r8n - connecting people with trusted reviewers.
- **Implementation**:
  - **Profile System**: User profiles with personal information
  - **Chat System**: Basic messaging capabilities for direct communication between users
  - **User Management**: User service handles authentication, profile data, and relationship management
  - **Frontend Integration**: React components for profile views and messaging interfaces
- **Team Members**: mkulikov, iatopchu

**3. A public API to interact with the database with a secured API key, rate limiting, documentation, and at least 5 endpoints**

- **Points**: 2 points (Major module)
- **Description**: Public API with secured API key authentication, rate limiting, comprehensive documentation, and at least 5 endpoints including GET, POST, PUT, DELETE operations
- **Justification**: A well-designed public API enables external integrations, third-party developers, and programmatic access to the platform's data. Security measures (API keys, rate limiting) protect against abuse while documentation ensures ease of use.
- **Implementation**:
  - **API Gateway**: Spring Cloud Gateway for centralized API management and routing
  - **Security**: API key-based authentication with Spring Security
  - **Rate Limiting**: Request rate limiting to prevent API abuse
  - **Documentation**: OpenAPI/Swagger documentation for all endpoints
  - **Endpoints**: 5+ RESTful endpoints under `/api/public/`:
    - GET /api/public/opinions/{id} - Retrieve opinion by ID
    - GET /api/public/opinions - Retrieve opinions for a subject
    - POST /api/public/opinions - Create new opinion
    - PATCH /api/public/opinions/{id} - Update opinion
    - DELETE /api/public/opinions/{id} - Delete opinion
    - GET /api/public/users/{id} - Retrieve user profile
    - GET /api/public/subjects - Retrieve subjects
    - GET /api/public/messaging - Retrieve messaging threads
- **Team Members**: inikulin

**4. Standard user management and authentication**

- **Points**: 2 points (Major module)
- **Description**: Users can update their profile information, upload an avatar, see other users' online status, and have a profile page displaying their information
- **Justification**: Comprehensive user management and authentication are fundamental to any social platform. These features enable user identity, personalization, and engagement - all core to the r8n platform's value proposition of building trusted reviewer networks.
- **Implementation**:
  - **Profile Management**:
    - User profile creation and editing
    - Personal information updates (name, about, location)
  - **Avatar System**:
    - Avatar upload with JPEG/PNG/WebP validation
    - Replace or delete current avatar
  - **Online Status**:
    - Last seen timestamp per user
  - **Profile Pages**:
    - Public profile pages with user information
  - **Backend**: User service with Spring Security and profile data management
  - **Frontend**: React profile components and avatar upload interface
- **Team Members**: inikulin, mkulikov

**5. Advanced permissions system**

- **Points**: 2 points (Major module)
- **Description**: View, edit, and delete users (CRUD), roles management (admin, user, guest, moderator, etc.), and different views and actions based on user role
- **Justification**: An advanced permissions system is crucial for platform security, content moderation, and administrative control. Role-based access control (RBAC) ensures users can only access features and data appropriate to their role, protecting both the platform and user data.
- **Implementation**:
  - **User CRUD Operations**:
    - View user list with filtering and search
    - Edit user information and settings
    - Delete users with proper confirmation and data cleanup
    - User status management (active, suspended, deletion pending, deleted)
  - **Roles Management**:
    - Role definitions: Admin, Moderator, AI_Moderator, User, Support
    - Role assignment and modification via dedicated UI page
  - **Access Control**:
    - Method-level security with `@PreAuthorize` Spring Security annotations
    - URL-based access control
    - API endpoint protection per role
  - **Role-Based Views**:
    - Opinion moderation interface for moderators
    - Role assignment page for admins
    - Conditional UI rendering based on user role
  - **Backend**: Spring Security with role-based access control in `users-sv`
  - **Frontend**: Role-aware React components, `OpinionModeration` and `RoleAssignment` pages
- **Team Members**: inikulin

**6. Backend as microservices**

- **Points**: 2 points (Major module)
- **Description**: Design loosely-coupled services with clear interfaces, use REST APIs or message queues for communication, and ensure each service has a single responsibility
- **Justification**: Microservices architecture enables scalability, maintainability, and independent deployment of different parts of the system. Loosely-coupled services with clear boundaries allow teams to work independently, choose appropriate technologies for each service, and scale components based on demand.
- **Implementation**:
  - **Service Architecture**:
    - Gateway Service: API routing, load balancing, authentication
    - Opinions Service: Core business logic for opinions management
    - Users Service: User management, authentication, profiles
    - Migration Service: Data export and import (GDPR data portability)
    - Messaging Service: User messaging and thread management
    - Mock Service: Testing and development support
  - **Loose Coupling**:
    - Clear service boundaries with well-defined interfaces
    - API contracts using OpenAPI specifications
    - DTOs for data transfer between services
  - **Communication**:
    - REST APIs over HTTPS with TLS encryption
    - JSON for data serialization
  - **Single Responsibility**:
    - Each service handles one business domain
    - Separate PostgreSQL schemas per service (opinions, users, messaging)
    - Service-specific business logic
  - **Infrastructure**:
    - Docker containerization for each service
    - Docker Compose for orchestration
    - Separate configuration per service
    - Service-specific logging
  - **Development**:
    - Independent service development
    - Service-specific integration tests
    - Gradle multi-module project structure
- **Team Members**: inikulin, iatopchu, lshapkin



#### Minor Modules (1 point each)

**1. Use a frontend framework (React, Vue, Angular, Svelte, etc.)**

- **Points**: 1 point (Minor module)
- **Description**: Use a frontend framework (React, Vue, Angular, Svelte, etc.)
- **Justification**: Frontend frameworks provide structured, component-based architecture that improves code maintainability, reusability, and developer experience. React was chosen for its large ecosystem, strong TypeScript support, and industry adoption.
- **Implementation**:
  - React 18.3.1 with TypeScript 5.8.3 for type safety
  - Vite 8.0.1 as build tool for fast development and optimized production builds
  - Component-organized structure with custom design system
- **Team Members**: mkulikov, dbisko

**2. Use a backend framework (Express, Fastify, NestJS, Django, etc.)**

- **Points**: 1 point (Minor module)
- **Description**: Use a backend framework (Express, Fastify, NestJS, Django, etc.)
- **Justification**: Backend frameworks provide robust foundations for building scalable, maintainable server-side applications with built-in patterns for routing, middleware, and database integration. Spring Boot was chosen for its comprehensive ecosystem, strong Kotlin support, and production-ready features.
- **Implementation**:
  - Spring Boot 4.0.4 with Kotlin 2.2.21 for backend services
  - Spring Cloud Gateway for API routing and authentication
  - Spring Data JPA for database abstraction and ORM
  - Gradle with Kotlin DSL for build automation
  - Microservices architecture with separate services (Gateway, Opinions, Users, Messaging, Migration, Mock)
- **Team Members**: iatopchu, inikulin

**3. Use an ORM for the database**

- **Points**: 1 point (Minor module)
- **Description**: Use an Object-Relational Mapping (ORM) tool for database operations
- **Justification**: ORMs provide abstraction layers that simplify database interactions, improve code maintainability, and reduce SQL injection risks. They enable developers to work with objects instead of raw SQL while maintaining performance through optimized queries.
- **Implementation**:
  - **Spring Data JPA**: Primary ORM framework for database operations
  - **Hibernate**: JPA implementation providing object-relational mapping
  - **Entity Mapping**: Kotlin data classes mapped to database tables
  - **Repository Pattern**: Spring Data repositories for CRUD operations
  - **Query Methods**: Derived query methods and custom JPQL queries
  - **Schema Management**: Hibernate validates schema on startup (`ddl-auto: validate`); schema is managed by Liquibase
- **Team Members**: iatopchu, inikulin

**4. Custom-made design system with reusable components, including a proper color palette, typography, and icons (minimum: 10 reusable components)**

- **Points**: 1 point (Minor module)
- **Description**: Custom-made design system with reusable components, including a proper color palette, typography, and icons (minimum: 10 reusable components)
- **Justification**: A custom design system ensures visual consistency across the application, improves development efficiency through reusable components, and creates a cohesive user experience. It establishes design standards that scale with the project.
- **Implementation**:
  - **Color Palette**: Custom CSS variables for primary, secondary, accent, muted, and neutral colors (light/dark modes)
  - **Typography**: Geist Sans (UI text) and Instrument Serif (display) with defined weights
  - **Icon System**: lucide-react icon library
  - **Component Library**: shadcn/ui components (Radix UI primitives) with custom theme applied, included in source code (10+):
    - Button, Input, Textarea, Select, Checkbox
    - Card, Badge, Avatar
    - Dialog, Alert Dialog, Drawer, Sheet
    - Navigation, Sidebar, Breadcrumb
    - Form with validation, Toast/Sonner notifications
  - **Design Tokens**: Centralized design variables for consistency
  - **Component Documentation**: Usage guidelines and examples
- **Team Members**: mkulikov, dbisko

**5. Implement advanced search functionality with filters, sorting, and pagination**

- **Points**: 1 point (Minor module)
- **Description**: Implement advanced search functionality with filters, sorting, and pagination
- **Justification**: Advanced search capabilities are essential for users to efficiently find relevant content in large datasets. Filters, sorting, and pagination improve user experience by allowing precise control over search results and manageable data presentation.
- **Implementation**:
  - **Search**: User search by name and status
  - **Filter System**:
    - Opinions: filter by subject, opinion list, status, and date (`since`)
    - Access requests: filter by list, status, and time
  - **Sorting**: Configurable sort by property with ASC/DESC direction
  - **Pagination**: Page-based pagination with configurable page size
  - **Backend**: Custom `@Query` JPQL queries in Spring Data repositories
  - **Frontend**: React components for search interface, filter controls, and result display
- **Team Members**: inikulin

**6. Avatar upload and management**

- **Points**: 1 point (Minor module)
- **Description**: Support avatar uploads with client-side and server-side validation, secure storage with proper access control, and the ability to replace or delete the current avatar
- **Justification**: Avatar upload is needed for profile personalization. Scope is intentionally narrow to reduce privacy and storage risk.
- **Implementation**:
  - **Supported Types**: JPEG, PNG, WebP
  - **Client-side Validation**: file type and size validation before upload
  - **Server-side Validation**: MIME type verification, size and dimension limits
  - **Secure Storage**: access control based on user identity, stored under `/var/lib/r8n/uploads/avatars`
  - **File Management**: replace or delete current avatar
  - **Backend**: Spring Boot multipart file handling in `users-sv`
  - **Frontend**: React avatar upload component with preview
- **Team Members**: mkulikov

**7. GDPR compliance features**

- **Points**: 1 point (Minor module)
- **Description**: Allow users to request their data, delete their account with confirmation, and export user data in JSON format
- **Justification**: GDPR compliance is essential for any platform handling user data, especially in European markets. These features demonstrate commitment to data protection, user privacy rights, and legal compliance, building trust with users and avoiding regulatory penalties.
- **Implementation**:
  - **Data Export**:
    - Async export: request generation → check status (PENDING/READY) → download JSON
    - Exported data includes: profile (PII), opinions, access requests (incoming/outgoing), messages, consents, account status
    - Data import from previously exported JSON
  - **Data Deletion**:
    - Account deletion with confirmation by entering email address in the UI
    - Data removal across all services
  - **Consent Tracking**: User consents stored and included in data export
  - **Backend**: Migration service for export/import, User service for account deletion
  - **Frontend**: React components for export request, status polling, and deletion confirmation
- **Team Members**: lshapkin, inikulin



### Module Point Calculation

**Total Points**: 19 points (6 Major modules, 7 Minor modules)
**Minimum Required**: 14 points

**Implemented**: 19 points ✅ (Exceeds minimum requirement by 5 points)
**Planned**: Additional modules for enhanced functionality

**Distribution:**
- Major modules completed: 6 * 2 = 12 points
- Minor modules completed: 7 * 1 = 7 points

## Individual Contributions

### **inikulin** - Tech Lead & Backend Developer

**Primary Responsibilities:**
- Led architectural design discussions
- Implemented core microservices pattern
- Backend framework setup and configuration
- Database schema design
- API contract definitions
- Code review and team guidance

**Technical Contributions:**
- **Gateway Service**: Implemented Spring Cloud Gateway routing
- **Opinions Service**: Core business logic, persistence layer
- **Core Modules**: Shared utilities, security
- **Database**: PostgreSQL schema design and migration scripts
- **API Design**: OpinionApi, DTO definitions, OpenAPI planning

**Key Challenges Overcome:**
- Designed microservices communication patterns
- Implemented TLS/SSL inter-service security
- Resolved database connection pooling issues
- Established API-first design methodology

### **iatopchu** - Backend Developer

**Primary Responsibilities:**
- Backend service development
- Business logic implementation
- Testing and quality assurance

**Technical Contributions:**
- **Opinions Service**: Business logic development
- **Backend Features**: Service implementation and refinement
- **Testing**: Unit and integration tests

### **lshapkin** - Backend Developer

**Primary Responsibilities:**
- Backend service development
- Testing infrastructure

**Technical Contributions:**
- **Service Development**: Backend logic implementation
- **Testing**: Test coverage and QA processes

### **mkulikov** - Frontend Lead & UI/UX Developer

**Primary Responsibilities:**
- Frontend architecture design
- UI/UX design implementation
- Component library development
- API integration layer
- User experience optimization

**Technical Contributions:**
- **Frontend Architecture**: Component-organized React application structure
- **React Application**: Core component development
- **Design System**: Custom reusable components (buttons, inputs, layouts)
- **API Integration**: HTTP client setup and integration
- **Styling**: CSS/SASS design system with color palette and typography

**Key Challenges Overcome:**
- Designed component-organized frontend structure
- Designed responsive component system
- Integrated with backend microservices APIs
- Managed TypeScript configuration and types

### **dbisko** - Frontend Developer

**Primary Responsibilities:**
- React component development
- API integration
- Frontend testing

**Technical Contributions:**
- **React Development**: Component implementation
- **API Integration**: Frontend-backend integration
- **UI Polish**: User interface improvements

## Additional Information

### Known Limitations

1. **Authentication**: JWT-based; production should implement OAuth 2.0
2. **Monitoring**: ELK stack and Prometheus/Grafana are planned but not fully implemented
3. **File Upload**: Infrastructure ready but not yet implemented
4. **i18n**: Planned but pending implementation
5. **PWA Features**: Service worker infrastructure available but offline functionality incomplete

### Future Enhancements

**High Priority:**
- OAuth 2.0 integration for production authentication
- ELK stack for centralized logging
- Prometheus + Grafana for monitoring and alerting
- Complete file upload system

**Medium Priority:**
- Full i18n support (minimum 3 languages)
- PWA offline support and installability
- Advanced content moderation AI
- Recommendation system using machine learning

**Low Priority:**
- Additional browser compatibility testing
- Performance optimizations
- Enhanced documentation

### License

This project is created as part of the 42 curriculum and follows the school's guidelines for academic projects.

### Credits

- **42 School** - Curriculum and educational framework
- **Anthropic (Claude Code)** - AI assistant for documentation and problem-solving
- **OpenAI (Codex/ChatGPT)** - AI coding assistant
- **JetBrains (Junie)** - AI coding assistant
- **Spring Team** - Spring Boot framework
- **React Team** - React library and ecosystem
- **Docker Team** - Containerization platform

---

## Quick Reference

**Documentation Index:**
- Architecture: `docs/ARCHITECTURE.md`
- Backend Setup: `docs/backend_developer_onboarding_*.md`
- Frontend Setup: `docs/frontend_developer_onboarding.md`
- Requirements: `docs/42_points_requirements.md`
- End User Workflow: `docs/end_user_workflow.md`

**Quick Commands:**
```bash
# Local development
make local-run-all

# Docker deployment
make docker-up

# Database console
make docker-database-connect

**Access URLs:**
- Frontend: http://localhost:5173 (local) / https://localhost:5173 (Docker)
- Gateway API: http://localhost:8080 (local) / https://localhost:8080 (Docker)
