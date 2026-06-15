# CLAUDE.md

## Project Overview

klibs.io — a service that indexes, catalogs, and provides search for Kotlin Multiplatform libraries from Maven Central and GitHub, with AI-powered metadata generation.

## Tech Stack

- **Language:** Kotlin 2.0+, JVM target: Java 21+
- **Framework:** Spring Boot
- **Database:** PostgreSQL 15+ (Liquibase migrations, JPA + raw JDBC)
- **Build:** Gradle with Kotlin DSL, version catalog at `gradle/libs.versions.toml`
- **Docker:** PostgreSQL, localstack, Testcontainers

Note: avoid introducing new dependencies unless absolutely necessary.

## Project Structure
```
app/                        # Main Spring Boot module (runnable). Configs, scheduled jobs, indexing services.
core/
  package/                  # Maven packages/artifacts
  project/                  # High-level project entity (aggregates packages + SCM repo)
  readme/                   # README fetch & processing (minimize, link rewrite, androidx providers)
  scm-owner/                # GitHub org/user owners
  scm-repository/           # Git repositories, SCM metadata
  search/                   # Full-text search (PostgreSQL FTS, materialized views)
  storage/                  # S3 storage abstraction
integrations/
  ai/                       # OpenAI integration (descriptions, tags)
  github/                   # GitHub API integration
  maven/                    # Maven Central scanning and indexing
build-logic/                # Gradle convention plugins
build-settings-logic/       # Gradle settings plugins
frontend/                   # Frontend app (React)
```

Module structure follows "module by feature". Each core module has its own entity, repository, service, and controller layers.

## Build & Run

```bash
# Build
./gradlew build

# Build without tests
./gradlew build -x test

# Run locally with Spring profile (requires Docker for PostgreSQL and localstack via docker-compose)
./gradlew bootRun --args='--spring.profiles.active=local'
```

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
  - `BaseUnitWithDbLayerTest` — database integration tests
  - `SmokeTestBase` — web/API endpoint tests
    - use only for end-to-end tests, otherwise avoid
- **Test locations:** `<module>/src/test/kotlin/`

Docker must be running for Testcontainers-based tests.

### Spring Profiles

- `local` — local development (uses docker-compose for DB)
- `prod` — production (restricts debug utilities)

### Configuration

Key config files in `app/src/main/resources/`:
- `application.yml` — base config
- `application-local.yml` — local dev
- `application-prod.yml` — production template

## Database

- PostgreSQL with Liquibase migrations in `app/src/main/resources/db/migration/` (organized by quarter: 2024-Q4, 2025-Q1, etc.)
- Mix of Spring Data JPA (packages) and custom JDBC (projects, search)
  - Note: this project is JPA-first, even if this costs some performance.
    Avoid using JDBC in new code.
- Materialized views `project_index` and `package_index` for full-text search
- ShedLock table for distributed scheduling locks

## Key Architecture Decisions

(Rationale only — module layout is under Project Structure, FTS details under Database.)

- **PostgreSQL FTS** — chosen for simpler deployment; acknowledged tech debt, contained in `core/search`
- **S3** — cold cache only (readme, GMaven package metadata)
- **Scheduled jobs** — drive the data-processing pipelines

## Coding Conventions

- Package namespace: `io.klibs.*`
- Naming: `*Entity`, `*Repository`, `*RepositoryJdbc`, `*Controller`, `*Service`, `*DTO`, `*Response`, `*Configuration`, `*Properties`
- Test method names use backtick syntax: `` `descriptive test name` ``
- 4-space indentation, 120-char max line length
- Kotlin coding conventions (camelCase functions/vars, PascalCase classes)

## Branching & Workflow
Important: DONT PUSH! PUSH IS PROHIBITED!
Don't delete branches either.

- `master` — production (auto-deployed)
- `release*` — current release branch, deployed to test environment
- `feature/KTL-<id>-<desc>` — feature branches from release
- `hotfix/KTL-<id>-<desc>` — hotfix branches from master
- Release tags: `release-yyyy.mm.dd`

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
- **IMPORTANT: prefer minimal diff** — avoid large reformats.
- Methods should fit on one screen (50 lines max).
- Avoid excessive comments. Keep code self-documenting.
- Keep comments clear and concise. 3 lines max.
- **IMPORTANT: no refactors** unless explicitly requested or required for correctness; if required, propose as a separate milestone first.
- Do not rename/move packages broadly.
- Do not change public APIs unless requested.
- Avoid dependency upgrades unless requested.
- Tests:
  - Make tests check business logic, not implementation details or method call sequence.
  - Avoid mocking unless absolutely necessary.
  - Main thing: check that post-condition corresponds to expected behavior. Don't check implementation details.

### Output format
Use concise headings:
- Goal
- Assumptions / Constraints
- Plan (Milestones)
- Milestone N Results
- Validation
- Risks / Next Steps
