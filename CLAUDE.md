# CLAUDE.md

## Project Overview

klibs.io backend — a Spring Boot Kotlin service that indexes, catalogs, and provides search for Kotlin Multiplatform libraries from Maven Central and GitHub, with AI-powered metadata generation.

## Tech Stack

- **Language:** Kotlin 2.1.0, JVM target: Java 21
- **Framework:** Spring Boot 3.5.9
- **Database:** PostgreSQL (Liquibase migrations, JPA + raw JDBC)
- **Build:** Gradle with Kotlin DSL, version catalog at `gradle/libs.versions.toml`
- **Key libraries:** Kotlin Coroutines 1.10.1, Spring AI 1.1.2 (OpenAI), Spring Cloud AWS 3.4.2 (S3), OkHttp 4.12.0, Kohsuke GitHub API 1.321, Maven Indexer 7.1.6, ShedLock, Bucket4j, Testcontainers

## Project Structure

```
app/                        # Main Spring Boot module (runnable). Configs, scheduled jobs, indexing services.
core/
  package/                  # Maven packages/artifacts
  project/                  # High-level project entity (aggregates packages + SCM repo)
  scm-owner/                # GitHub org/user owners
  scm-repository/           # Git repositories, README processing
  search/                   # Full-text search (PostgreSQL FTS, materialized views)
  storage/                  # S3 storage abstraction
integrations/
  ai/                       # OpenAI integration (descriptions, tags)
  github/                   # GitHub API integration
  maven/                    # Maven Central scanning and indexing
build-logic/                # Gradle convention plugins
build-settings-logic/       # Gradle settings plugins
```

Module structure follows "module by feature". Each core module has its own entity, repository, service, and controller layers.

## Build & Run

```bash
# Build
./gradlew build

# Build without tests
./gradlew build -x test

# Build runnable JAR
./gradlew bootJar
# Output: app/build/libs/app.jar

# Run locally (requires Docker for PostgreSQL via docker-compose)
# Run the main function from Application.kt — uses 'local' Spring profile
```

### Prerequisites

- JDK 21+
- Docker (for local PostgreSQL and Testcontainers in tests)

### Spring Profiles

- `local` — local development (uses docker-compose for DB)
- `prod` — production (restricts debug utilities)

### Configuration

Key config files in `app/src/main/resources/`:
- `application.yml` — base config
- `application-local.yml` — local dev
- `application-prod.yml` — production template

Important properties: `klibs.indexing` (enable/disable Maven Central scanning), GitHub/OpenAI tokens, S3 settings, cache directories.

## Testing

```bash
# Run all tests
./gradlew test

# Run tests for a specific module
./gradlew :app:test
./gradlew :core:package:test

# Run a specific test class
./gradlew :app:test --tests "io.klibs.app.example.SimpleExampleTest"
```

- **Framework:** JUnit 5, Spring Boot Test, MockMvc, Testcontainers (PostgreSQL), Mockito Kotlin
- **Base test classes:**
    - `SmokeTestBase` — web/API endpoint tests
    - `BaseUnitWithDbLayerTest` — database integration tests
- **Test locations:** `<module>/src/test/kotlin/`

Docker must be running for Testcontainers-based tests.

## Database

- PostgreSQL with Liquibase migrations in `app/src/main/resources/db/migration/` (organized by quarter: 2024-Q4, 2025-Q1, etc.)
- Mix of Spring Data JPA (packages) and custom JDBC (projects, search)
- Materialized views `project_index` and `package_index` for full-text search
- ShedLock table for distributed scheduling locks

## Key Architecture Decisions

- **PostgreSQL FTS** over ElasticSearch — simpler deployment; acknowledged tech debt, contained in `core/search`
- **Module by feature** — each domain is a separate Gradle module
- **Interface-based integrations** — `AiService`, `GitHubIntegration`, `MavenSearchClient`
- **S3 for README storage** with local cache
- **Scheduled jobs** — daily indexing (2 AM), GitHub metadata updates, AI description generation, materialized view refresh

## Coding Conventions

- Package namespace: `io.klibs.*`
- Naming: `*Entity`, `*Repository`, `*RepositoryJdbc`, `*Controller`, `*Service`, `*DTO`, `*Response`, `*Configuration`, `*Properties`
- Test method names use backtick syntax: `` `descriptive test name` ``
- 4-space indentation, 120-char max line length
- Kotlin coding conventions (camelCase functions/vars, PascalCase classes)

## Branching & Workflow

- `master` — production (auto-deployed)
- `release*` — current release branch, deployed to test environment
- `feature/KTL-<id>-<desc>` — feature branches from release
- `hotfix/KTL-<id>-<desc>` — hotfix branches from master
- Release tags: `release-yyyy.mm.dd`

## API Documentation

Swagger UI available at `/api-docs/swagger-ui.html`. Actuator at `/actuator/health` and `/actuator/info`.

## Updating JVM Version

Three places to update:
1. `build-logic/build.gradle.kts` — build logic module toolchain
2. `build-logic/src/main/kotlin/klibs.kotlin-jvm.gradle.kts` — convention plugin toolchain
3. Run `./gradlew updateDaemonJvm` — Gradle daemon JVM version

## Claude Code Working Agreement (Milestone Gating)

### Objective
Reduce context switching and rework. Prefer small, reviewable diffs and explicit stop points.

### Default interaction pattern
**Step 1 — Align (no code changes yet):**
- Restate the goal in 1–2 sentences.
- List assumptions + constraints (and note uncertainty explicitly).
- Propose a plan with **2–4 milestones**.
    - For each milestone: exact modules/files to touch, and how to validate (tests/commands).
- **STOP and wait for approval** before editing files.

**Step 2 — Execute (one milestone at a time):**
- Implement **only the next approved milestone**.
- Keep changes minimal and localized.
- **STOP after the milestone** and provide:
    - per-file change summary
    - validation commands
    - risks / follow-ups

### Change constraints (hard defaults)
- Prefer **minimal diff**; avoid large reformats.
- **No refactors** unless explicitly requested or required for correctness; if required, propose as a separate milestone first.
- Do not rename/move packages broadly.
- Do not change public APIs unless requested.
- Avoid dependency upgrades unless requested.

### Repo-specific expectations
- Kotlin/JVM 21 + Spring Boot 3.5.x conventions.
- Respect module-by-feature boundaries; don’t “leak” layers across modules.
- Prefer adding/adjusting tests early (JUnit5 / Spring Boot Test / Testcontainers when relevant).

### Output format
Use concise headings:
- Goal
- Assumptions / Constraints
- Plan (Milestones)
- Milestone N Results
- Validation
- Risks / Next Steps
