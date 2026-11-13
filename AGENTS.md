# AGENTS.md — mVPN Engineering Excellence & Operating Manual

> **Scope**: This document defines the engineering standards, architecture, security policies, and operational directives for the **mVPN** platform. All human engineers and AI agents operating in this repository must strictly adhere to these guidelines, which align with Google / FAANG engineering, security, and SRE benchmarks.

---

## 1. System Vision & Architecture Overview

**mVPN** is an enterprise-grade, microservice-based VPN orchestrator and subscription management platform built with high-throughput event streaming, distributed object storage, automated node synchronization, and zero-trust authentication.

```
                      +---------------------------------------+
                      |         Public Ingress / Clients       |
                      |   (Web Frontend, Telegram Bot, VPN)    |
                      +-------------------+-------------------+
                                          | HTTPS (:443)
                                          v
                      +---------------------------------------+
                      |            Traefik v3 Proxy           |
                      |  (TLS ACME, Security Headers, Auth)   |
                      +---------+-------------------+---------+
                                |                   |
                      +---------v-------+   +-------v---------+
                      |  React 19 SPA   |   |  Spring Boot 3  |
                      | (TypeScript/Vite|   |  Backend (Java21|
                      +-----------------+   +--------+--------+
                                                     |
       +--------------------+------------------------+-----------------------+
       |                    |                        |                       |
+------v------+      +------v-------+        +-------v-------+       +-------v-------+
|  PostgreSQL |      | Apache Kafka |        |   MinIO S3    |       |  3X-UI Nodes  |
|  (Liquibase)|      | (Event Bus)  |        | (Keys/Configs)|       |  (VPN Panels) |
+-------------+      +--------------+        +---------------+       +---------------+
                                 \                  /
                                  +--------+-------+
                                           | Telemetry
                    +----------------------v----------------------+
                    | Central Observability (Prometheus / Loki)   |
                    | Scraped via Prometheus Agent & Promtail     |
                    +---------------------------------------------+
```

### Core Technology Stack

| Layer | Technologies | Standards / Guidelines |
|---|---|---|
| **Backend** | Java 21, Spring Boot 3.4+, Maven | Google Java Style, Immutability, MapStruct, Lombok |
| **Frontend** | React 19, TypeScript 5.8+, Vite 7, Tailwind | Strict TypeScript, Functional Components, Zustand, Zod |
| **Data & Persistence** | PostgreSQL 15, Liquibase, Spring Data JPA | Zero DDL auto-generation in prod, AES-256-GCM encryption |
| **Messaging & Events** | Apache Kafka (KRaft mode) 7.6+ | Event-driven architecture, idempotent consumer/producer |
| **Storage & Remote** | MinIO (S3 API), JSch (SSH2), 3X-UI REST | Encrypted SSH keys, secure object lifecycles |
| **Auth & Identity** | Keycloak OIDC / OAuth2 Resource Server | JWT Bearer verification, RBAC (`VPN_ADMIN`), check-sso |
| **Ingress & Gateway** | Traefik v3.6 | TLS termination, strict router priority, security headers |
| **Observability** | Micrometer, Prometheus Agent v3, Promtail 3.3, Loki | Golden Signals, MDC correlation IDs, multi-app label isolation |
| **Quality & Security** | JaCoCo, Checkstyle, SpotBugs, OWASP Dep-Check, CodeQL | Strict CI gates, CVSS < 9 enforcement, no secrets in VCS |

---

## 2. Repository Topology & Boundaries

```
mVPN/
├── .github/
│   └── workflows/          # CI/CD: ci.yml (tests/lint/quality), security.yml (CodeQL/OWASP)
├── backend/                # Spring Boot service
│   ├── src/main/java/      # Domain-driven Java modules (user, server, xui, tariff, etc.)
│   ├── src/main/resources/ # application*.yml, Liquibase changelogs, OpenAPI contracts
│   ├── src/test/java/      # JUnit 5 unit, integration, and contract tests
│   ├── Dockerfile          # Multi-stage Eclipse Temurin 21 container (non-root javauser:1001)
│   └── pom.xml             # Enforcer, JaCoCo, Checkstyle, SpotBugs, OpenAPI generator
├── Frontend/               # React 19 Single Page Application
│   ├── src/
│   │   ├── api/            # Centralized Axios client with Keycloak auth interceptor
│   │   ├── pages/          # Route-level views (Dashboard, Users, Servers, Tariffs, etc.)
│   │   ├── store/          # Zustand global state stores
│   │   └── types/          # Strict TypeScript interface and enum models
│   ├── Dockerfile          # Multi-stage build with Nginx Alpine static server
│   └── package.json        # ESLint flat config, Prettier, TypeScript configuration
├── prometheus/             # Prometheus Agent scraper config (metrics push to central mDash)
├── loki/                   # Promtail file/container log collector config (logs push to central Loki)
├── compose.dev.yml         # Local Docker development stack
├── compose.prod.yml        # Production Docker orchestration (Traefik v3, SSL, healthchecks)
└── AGENTS.md               # Master AI Agent & Engineering Directives (this file)
```

---

## 3. High-Priority Directives for AI Agents

When interacting with this codebase, every agent **MUST** observe these non-negotiable rules:

1. **Contract-First (API First)**:
   - All REST contracts originate in [`backend/src/main/resources/static/api/openapi.yaml`](file:///E:/Programs/Java/mVPN/backend/src/main/resources/static/api/openapi.yaml).
   - **NEVER** edit files under `backend/target/generated-sources` or manually modify files in `ru.maxow.mvpn.api` / `ru.maxow.mvpn.model`.
   - Always update the OpenAPI YAML spec first, then regenerate models via `mvn compile` or `./mvnw compile`.
2. **Entity & DTO Segregation**:
   - JPA entities (`@Entity`) must **NEVER** cross the boundary into Controllers, REST responses, or Kafka event payloads.
   - Use MapStruct mappers (`*Mapper.java`) with unmapped target checks to convert between internal entities and external DTOs.
3. **Immutability & Safety**:
   - Prefer immutable data structures. Annotate Spring services and components with `@RequiredArgsConstructor` and `@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)`.
   - Avoid mutable global state. Use Java 21 `record` types for internal carrier objects, value objects, and event messages.
4. **Zero Secret Leakage**:
   - Never commit tokens, passwords, private keys, or API credentials into source code, YAML configs, or Dockerfiles.
   - All secrets must be referenced via environment variables `${VAR_NAME}` in configurations with secure fallbacks or required bindings.
5. **No Regressions in Quality Gates**:
   - Code must pass Checkstyle (Google Java Style), SpotBugs, JaCoCo code coverage, and ESLint without warnings.
   - Never use `-DskipTests` or `-DskipSecurityScan` in production pipelines or pull requests.
6. **Code Comment Discipline & Zero Noise**:
   - Code must be self-documenting through expressive naming and clean abstractions.
   - **NEVER** write redundant comments or JavaDoc that merely rephrase method/field names (e.g., `/** Returns download bytes */ public long getDownloadBytes()`).
   - Comments must be concise and reserved strictly for:
     - The **WHY**, not the **WHAT** (business constraints, non-obvious domain rules, intentional design trade-offs).
     - External protocol quirks (3X-UI API nuances, VLESS stream settings, cryptographic IV constraints), or workarounds for third-party bugs.
   - If a method needs a paragraph of explanation to be understood, prefer refactoring into smaller, clearly named functions over commenting.
7. **Commit Hygiene & No Micro-Commit Spam**:
   - **NEVER** pollute Git history with dozens of micro-commits (`fix`, `fix test`, `typo`, `wip`).
   - Prior to merging into `main`/`master`, squash or amend iterative fixes into the parent commit where those files/features originated (`git commit --fixup`, interactive rebase).
   - Only create a new commit when introducing a distinctly new capability or touching a separate responsibility. Keep the branch history atomic, clean, and linear.
8. **Column Limit & Line Wrapping (Google / FAANG Benchmark)**:
   - Enforce a strict **100-character column limit** across all Java files and tests per Google Java Style Guide (Section 4.4).
   - Any statement, method signature, lambda, log call, annotation, or comment exceeding 100 characters must be line-wrapped.
   - This ensures readable side-by-side diffing and review in split editor panes on standard displays without horizontal scrolling.

---

## 4. Backend Engineering Standards (Java 21 / Spring Boot 3)

### 4.1 Layered Architecture & Separation of Concerns

Each feature package (e.g. `ru.maxow.mvpn.server`, `ru.maxow.mvpn.user`) must strictly isolate responsibilities:

```
[ HTTP / REST Ingress ] -> Controller (Implements generated OpenAPI interface)
                                     |
[ DTO Conversion ]      -> MapStruct Mapper
                                     |
[ Business Logic ]      -> Service / ServiceImpl (@Transactional boundaries)
                                     |
[ Data Access ]         -> Spring Data JPA Repository (Specifications, JpaRepository)
                                     |
[ Database ]            -> PostgreSQL (Liquibase managed schema)
```

- **Controllers**: Thin orchestration layer. Validate requests via `@Valid`, delegate directly to Services, return DTOs with appropriate HTTP statuses.
- **Services**: Enforce domain rules, manage transactional boundaries (`@Transactional(readOnly = true)` by default, explicitly `@Transactional` for writes), publish Kafka events, execute remote node updates.
- **Repositories**: Standard Spring Data interfaces. Use `@EntityGraph` or explicit `JOIN FETCH` to eliminate N+1 query antipatterns.
- **Entities**: Contain table mappings, JPA constraints, and `@Version` for optimistic locking where concurrent updates are expected.

### 4.2 Error Handling & RFC 7807 Standard

All uncaught exceptions are intercepted by [`GlobalExceptionHandler`](file:///E:/Programs/Java/mVPN/backend/src/main/java/ru/maxow/mvpn/util/exception/GlobalExceptionHandler.java).
- **No Stack Traces**: Internal exception details or database stack traces must never be returned to the client.
- **Correlation ID Tracking**: Every error response contains a `correlationId` retrieved from the SLF4J `MDC`. This ID is returned to the client and printed in server logs for SRE incident tracing.
- **Explicit Domain Exceptions**:
  - `NotFoundException` -> HTTP 404
  - `BadRequestException` / `MethodArgumentNotValidException` -> HTTP 400
  - `ResourceAlreadyExistsException` / `DataIntegrityViolationException` -> HTTP 409
  - `UnauthorizedException` -> HTTP 401
  - `XuiUnavailableException` -> HTTP 503 (Service Temporarily Unavailable)

### 4.3 Database Migrations & Cryptography

1. **Liquibase Exclusivity & Master Changelog Registry**:
   - `spring.jpa.hibernate.ddl-auto` must remain `validate` in production environments.
   - All schema alterations, table additions, column additions, and foreign keys must be
     authored as reversible YAML/SQL changesets under `backend/src/main/resources/db/changelog/changes/`.
   - Every single changelog file in `changes/` **MUST** be explicitly included via
     `- include: file: ...` in `backend/src/main/resources/db/changelog/db.changelog-master.yaml`.
     Orphaned changelogs that are never executed in production are strictly prohibited.
   - Never alter an already committed and executed changeset; always create an incremental changeset.
2. **Preconditions Syntax Guardrails**:
   - In YAML changelogs, specify `onFail: "MARK_RAN"` (or other failure handling) **once** per
     `preConditions` container. Never place multiple `onFail` directives at the same YAML
     mapping level (which causes `ChangeLogParseException: Multiple nodes match null/onFail`).
   - Use `tableExists` when altering existing tables and `not: tableExists` when creating tables
     to guarantee safe, idempotent execution on both fresh and pre-existing databases.
3. **Mandatory Schema & Migration Contract Testing**:
   - Every entity or schema modification must be verified against `LiquibaseSchemaValidationTest`.
   - The test suite strictly validates:
     a) **Changelog Completeness**: Asserts all files in `changes/` are included in master changelog.
     b) **Fresh DB Execution**: Verifies Liquibase executes cleanly from scratch without parser errors.
     c) **JPA Entity Schema Validation**: Starts Spring with `hibernate.ddl-auto: validate` and
        Liquibase enabled to guarantee 100% sync between JPA `@Entity` definitions and Liquibase.
4. **Attribute Encryption (AES-256-GCM)**:
   - Sensitive columns (such as node access tokens, passwords, and private parameters) must use
     JPA `@Convert(converter = AttributeEncryptor.class)`.
   - The [`AttributeEncryptor`](file:///E:/Programs/Java/mVPN/backend/src/main/java/ru/maxow/mvpn/util/converter/AttributeEncryptor.java) applies authenticated encryption (`AES/GCM/NoPadding`) using a 12-byte cryptographically
     random IV generated via `SecureRandom` and a 128-bit authentication tag.

### 4.4 Concurrency & Asynchronous Resilience

- Use Java 21 Virtual Threads where applicable or bounded task executors (`ThreadPoolTaskExecutor`) with clear thread naming prefixes.
- In background tasks (e.g. `ServerMonitoringServiceImpl`, subscription synchronizers):
  - Isolate concurrent node checks to avoid locking database rows across remote network timeouts.
  - Set explicit connection and read timeouts on all HTTP and SSH clients (`JSch`, `OkHttpClient`).

---

## 5. Frontend Engineering Standards (React 19 / TypeScript 5)

### 5.1 Strict Type System & Code Hygiene

- **Strict Mode**: TypeScript strict mode is enabled. The use of `any` is strictly prohibited. Use `unknown` with type guards or schema validation (`zod`) when parsing uncertain payloads.
- **API Client**: All network communication passes through [`Frontend/src/api/index.ts`](file:///E:/Programs/Java/mVPN/Frontend/src/api/index.ts). The client automatically handles:
  - Base URL injection from Vite environment.
  - JWT Bearer token attachment via the dynamic `setTokenGetter`.
  - Automatic Keycloak token refresh before expiration (`updateToken(30)`).
  - Normalized `ApiError` propagation with status codes.

### 5.2 Component & State Patterns

- **Functional Components**: Use standard functional components with hooks. Avoid class components.
- **State Partitioning**:
  - **Local UI state**: `useState`, `useReducer`.
  - **Global application state**: Zustand stores (`Frontend/src/store/`). Stores must be modular and actions kept deterministic.
  - **Authentication context**: Handled via `AdminContext` and `AuthProvider` with Keycloak silent SSO.
- **Accessibility & Security**:
  - Prevent XSS: Never utilize `dangerouslySetInnerHTML` without DOMPurify sanitization.
  - Sanitize all outbound redirect URLs to avoid open-redirect vulnerabilities.

---

## 6. Security & Zero Trust Architecture

### 6.1 Authentication & Authorization Matrix

1. **Keycloak OIDC Integration**:
   - Spring Security acts as an OAuth2 Resource Server validating JWT signatures against Keycloak realm keys (`${KEYCLOAK_URL}/realms/mID`).
   - Role claims are extracted from Keycloak token payload via `KeycloakRoleConverter`.
2. **Access Control**:
   - Management endpoints (`/v1/users/**`, `/v1/servers/**`, `/v1/tariffs/**`, `/v1/subscriptions/**`, `/v1/promocodes/**`, etc.) strictly require role **`ROLE_VPN_ADMIN`**.
   - Public client endpoints (`/v1/config/**`) are scoped exclusively to user configuration download tokens and must never expose node credentials.
   - Actuator endpoints (`/actuator/**`) are permitted internally for health and metrics, but **MUST BE BLOCKED** at the external proxy layer.

### 6.2 Container & Infrastructure Hardening

1. **Non-Root Execution**:
   - Backend runs as system user `javauser` (UID 1001, GID 1001) with `no-install-recommends` packages.
   - Any host directory bind-mounted into `/app/logs` or data volumes must possess ownership `1001:1001` (`chown -R 1001:1001 ./logs`).
2. **Network Isolation**:
   - `web_proxy`: Ingress network shared exclusively between Traefik, Frontend, and Backend.
   - `internal`: Internal network connecting Backend to PostgreSQL, Apache Kafka, and MinIO. Database and message brokers must **never** be exposed on host ports in production.
   - `monitoring`: Dedicated network isolating Prometheus Agent and Promtail with backend metric endpoints.

### 6.3 Traefik v3 Reverse Proxy Guardrails

- **Unique Middleware Names**: Never declare the same Traefik middleware name across multiple containers. Define common middlewares on the proxy container or declare them uniquely to avoid silent router invalidation.
- **Routing Rules**: Traefik v3 router expressions use backticks for values and negation with `!`:
  ```yaml
  traefik.http.routers.mvpn-backend.rule: Host(`${API_DOMAIN}`) && !PathPrefix(`/actuator`)
  ```
- **Security Headers**: Always enforce HTTP Strict Transport Security (HSTS), `contentTypeNosniff: true`, and `referrerPolicy: no-referrer`.

---

## 7. Observability, Logging & SRE Telemetry

### 7.1 Multi-Tenant Observability Isolation (Loki & Prometheus)

When mVPN shares a centralized Prometheus/Loki cluster (e.g. with other internal projects like `maudio`), **all metrics and logs must be strictly isolated by service and application tags**.

- **Promtail Configuration (`loki/promtail-config.yml`)**:
  - Global client `external_labels` must declare `app: mvpn` and `project: mvpn`.
  - Scrape targets must append `app: mvpn` and `service: backend`.
  ```yaml
  clients:
    - url: https://${DASH_DOMAIN}/loki/api/v1/push
      external_labels:
        project: mvpn
        app: mvpn
  ```
- **Prometheus Scraper (`prometheus/prometheus.yml`)**:
  - Global `external_labels` must include `app: 'mvpn'` and `project: 'mvpn'`.
  - Target labels must maintain `service: 'backend'` and `app: 'mvpn'`.

### 7.2 LogQL Query Discipline in Dashboards

In centralized Grafana/mDash log viewers, always scope queries with the application identifier:
```logql
{app="mvpn", service=~"backend|db|prometheus|promtail|frontend"}
```

### 7.3 Micrometer & Golden Signals

- **Latency & Throughput**: Monitored via custom AOP metrics in [`RepositoryMetricsAspect`](file:///E:/Programs/Java/mVPN/backend/src/main/java/ru/maxow/mvpn/metrics/RepositoryMetricsAspect.java) emitting `repository.method.duration` with percentile histograms enabled.
- **Errors**: Emitted as `repository.errors` with tags `[repository, method, exception]``.
- **Health Checks**: `/actuator/health` exposes readiness and liveness states without disclosing internal component topologies to unauthenticated requests.

---

## 8. Testing Pyramid & Quality Gates

```
       /\
      /  \      Integration Tests (Spring Boot Test, Testcontainers, H2/Postgres)
     /----\     Contract & API Tests (OpenAPI validation, MockMvc, Security tests)
    /------\    Unit Tests (JUnit 5, Mockito, AssertJ, isolations)
```

### 8.1 Backend Verification Gates

```bash
# Standard local build & unit tests:
./mvnw clean verify

# Strict CI verification with Checkstyle, SpotBugs, and JaCoCo coverage rules:
./mvnw clean verify -Pci

# OWASP Vulnerability Check (fails build if CVSS >= 9):
./mvnw org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=9 -DfailOnError=false
```

- **Liquibase Schema Contract Gate**: All builds must pass `LiquibaseSchemaValidationTest`, ensuring zero syntax errors in YAML changelogs, zero orphaned changelogs, and complete alignment between Liquibase and JPA entities with `ddl-auto: validate`.
- **JaCoCo Thresholds**: Minimum 55% instruction coverage and 35-40% branch coverage on domain service packages. Exclude generated OpenAPI DTOs and MapStruct implementations.
- **Checkstyle & Line Length**: Adheres to Google Java Style conventions (`google_checks.xml`). Enforce a strict **100-character column limit** for all Java source and test files to ensure comfortable reading in dual-split editor panes without horizontal scrolling.

### 8.2 Frontend Verification Gates

```bash
# Type checking & production bundle compilation:
npm run build

# Static analysis and linting:
npm run lint
```

---

## 9. Git Workflow & Conventional Commits

All changes pushed to this repository must follow the **Conventional Commits** specification:

- `feat(scope)`: A new feature or capability (e.g. `feat(server): implement SSH key rotation via MinIO`)
- `fix(scope)`: A bug fix (e.g. `fix(traefik): eliminate duplicate middleware definition`)
- `sec(scope)`: A security improvement, vulnerability patch, or dependency upgrade
- `refactor(scope)`: Code changes that neither fix a bug nor add a feature
- `test(scope)`: Adding or updating unit, integration, or regression tests
- `chore(scope)`: Routine maintenance, CI/CD tweaks, build system upgrades

### 9.1 Branch Hygiene & Commit Squashing Policy

1. **Atomic & Purposeful Commits**: Each commit must represent a single, cohesive change. Avoid commingling refactoring, feature work, and formatting in a single commit.
2. **Zero Fix Spam in Branches**: While iterating on a PR or feature branch, do not leave trails of `fix`, `fix tests`, or `wip` commits. Use `git commit --fixup` and interactive rebase (`git rebase -i --autosquash`) to fold fixes into the commit where the modified code was originally introduced.
3. **Linear History**: Keep branch history linear and rebased onto `master`. Avoid unnecessary merge commits in feature branches.
4. **CI Gates Enforced**: Every Pull Request must pass the automated GitHub Actions pipelines ([`ci.yml`](file:///E:/Programs/Java/mVPN/.github/workflows/ci.yml) and [`security.yml`](file:///E:/Programs/Java/mVPN/.github/workflows/security.yml)) prior to merging into `main` or `master`.
