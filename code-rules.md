# Secured endpoints
Applies to: `**/*.kt`

When adding a controller endpoint, make sure it is covered by `SecurityConfiguration.kt`.

# API should be documented
Applies to:
```
**/api/**/*.kt
**/controller/**/*.kt
```

Document every endpoint input and output, and every field in its input and output, with `@Schema`.

# Pagination in controllers' endpoints
Applies to:
```
**/controller/**/*.kt
```

List endpoints should support pagination to prevent unnecessary data loading.

# Precise names
Applies to: 
```
**/*.kt
!**/*Test.kt
```

Name every class, function, variable, and bean after what it actually holds or does. Use `Base*` for abstract classes, `*Dto` for DTOs, and names such as `commitsCount` rather than `commits`; rename an element when its meaning changes.

# One class per file, in its layer package
Applies to: `**/*.kt`

Put each service, DTO, enum, entity, and properties class in its own file inside the feature's layer package, such as `service` / `service.impl` (interface and implementation), `dto`, `enums`, `entity`, `configuration`, `configuration.properties`, `metrics`, or `validator`.

# Logic lives in the module and layer that own it
Applies to: `**/*.kt`

Put domain logic in its `core/*` or `integrations/*` module rather than `app`. Handle integration errors inside the integration, keep business logic in services rather than SQL or repositories, and keep orchestrating services thin by extracting dedicated services.

# Idiomatic Spring bean wiring
Applies to: `**/*.kt`

Declare shared objects such as mappers, serializers, and index specifications as beans in a `configuration` class and inject them through the constructor. For several beans of one type, inject a `List` or rely on the parameter name instead of `@Qualifier`; use `@Component` instead of a manual `@Bean` for simple classes.

# Short, single-purpose methods
Applies to: `**/*.kt`

Split methods that exceed 40 lines or do multiple things (for example, handle issues and pull requests), and extract URL- or request-building code into named private functions.

# No speculative complexity
Applies to: `**/*`

Leave out code that is not needed, including extra single-item method variants, duplicate interfaces with the same contract, `AtomicReference` caches, custom timeouts or connection managers when library defaults work, `ON CONFLICT` on first inserts, trivial wrappers, and unnecessary seed changesets. Replacing a library's connection manager can lose its tuned defaults.

# Remove dead and redundant code
Applies to: `**/*`

Before opening a PR, remove unused methods, enum values, imports, fields, and extension functions. Also remove checks already performed elsewhere in the flow and files left empty after conflict resolution.

# Reuse before writing
Applies to: `**/*`

Before adding code, look for an existing service method, helper, stored value, or library utility, and extend existing logic instead of rewriting or duplicating it. Examples include `mavenArtifactService.resolveOrCreate`, the stored `minimizedReadme`, `JWTTokenProvider`, and `SSLContextBuilder`.

# Kotlin standard-library idioms
Applies to: `**/*.kt`

Prefer Kotlin idioms to manual code: use `partition` and `associateWith` instead of loops, `with(x)` instead of `Triple` destructuring, `when(subject)` with guards, and plain `flow` instead of `channelFlow` when nothing produces concurrently. Keep visibility modifiers consistent.

# No unrelated changes
Applies to: `**/*`

Revert unrelated edits before opening a PR, including whitespace or trailing-newline changes, brace or `()` tweaks, and stray `@Deprecated` or `@ConditionalOnProperty` annotations.

# No magic values
Applies to: 
```
**/*
!**/.*Test.kt
```

Replace magic literals with a named constant, enum (for example, `ReadmeType` instead of strings), or configuration property.
This includes durations such as `60*60*24*7`, timeouts, repeated strings such as `"androidx"`, and date formats.
Literals still can be used in annotations like @Min and @Max.

# No deprecated APIs
Applies to: `**/*`

Do not use deprecated APIs, endpoints, or build options. Examples include OpenSearch `build()` instead of `buildAsync`, the `GET` search endpoint instead of `POST`, and `kotlinOptions` instead of `compilerOptions`.

# Dedicated, documented API DTOs
Applies to:
```
**/api/**/*.kt
**/controller/**/*.kt
```

Give each layer its own types and never expose repository or entity types from a controller. Annotate every response field with `@field:Schema` and a realistic Kotlin-library example, and pass request data in a body DTO rather than path variables.

# Validate every input parameter
Applies to:
```
**/api/**/*.kt
**/controller/**/*.kt
**/mcp/**/*.kt
```

Validate every API and MCP tool parameter (for example, reject `TargetGroup.Unknown`) and give each validation annotation a `message`.

# Liquibase changeset hygiene
Applies to:
```
**/db/migration/**/*.yml
**/db/migration/**/*.yaml
```

In every Liquibase changeset, add preconditions covering every object it creates; use your real name as `author`; write single-line `comment:` values without `>` and without duplicated `- sql` entries; and set defaults with column `defaultValue` or `defaultValueDate` instead of a follow-up `UPDATE`.

# JPA for new persistence code
Applies to: `**/*.kt`

Implement new entities and repositories with JPA (`@Entity` and Spring Data `save`). Do not use data classes with `JdbcClient` or manual insert/update branching; all new entities should use JPA.

# Materialized view recreation
Applies to:
```
**/db/migration/**/*
```

When recreating `project_index` or `package_index`, recreate all of its indexes in the same recreation file. Reference the existing unchanged SQL file instead of adding an identical copy; each recreation copies changesets from the latest creation file, so anything kept elsewhere can be forgotten.

# Strict schema constraints
Applies to:
```
**/db/migration/**/*
**/entity/**/*.kt
```

Make new columns `NOT NULL` with a default where possible, and keep Kotlin nullability and null checks aligned with the schema. Use a `BIGINT` surrogate id plus a unique constraint instead of a composite primary key.

# Changelog order and unreleased changesets
Applies to:
```
**/db.changelog-master.yml
**/db.changelog-master.yaml
**/db/migration/**/*.yml
**/db/migration/**/*.yaml
```

Keep `db.changelog-master.yml` in chronological order, and amend a changeset that has not been released yet instead of adding a fix-up migration.

# Avoid redundant external I/O
Applies to: `**/*.kt`

Keep I/O to a minimum: do cheap database checks before calling remote APIs (`nativeQuery=true` is allowed for this); fetch only the metadata you need; avoid a separate remote call per version or artifact when the response you already fetch contains the data (for example, a release date from a POM's `Last-Modified` header or versions from `maven-metadata.xml`); and cache values that would otherwise be recomputed on every access. An extra call per version can raise the request count from fewer than `n` to `n+1` for `n` versions.

# Explicit transaction boundaries
Applies to: `**/*.kt`

Make multi-write service methods `@Transactional`, and use `REQUIRES_NEW` per item when one failure must not lose the other items' status updates. Never log success or read lazy fields after a transaction has ended or while it can still roll back; success logs can otherwise report writes that never commit.

# No silent fallbacks
Applies to: `**/*.kt`

Fail loudly instead of silently falling back to a default (for example, a default scraper). When a fallback is genuinely needed, make it explicit, such as PostgreSQL full-text search when OpenSearch fails.

# Log the paths that matter
Applies to: `**/*.kt`

Log every handled error, discarded request, and throttling cooldown. Log only facts the code actually knows; do not claim a `404` if the status is unknown.

# Minimal, explicit configuration properties
Applies to:
```
**/configuration/**/*.kt
**/application*.yml
**/application*.yaml
```

Add only the configuration properties you need and derive the rest. Declare defaults explicitly in `application.yml` instead of using `matchIfMissing`, and make alternative implementations mutually exclusive through a single type property. Otherwise, multiple alternative beans could be enabled at the same time.

# Dependency declarations
Applies to:
```
**/module.yaml
gradle/libs.versions.toml
```

Take dependency versions from `gradle/libs.versions.toml` and drop redundant `module.yaml` dependencies. Add the catalog entry in the same commit as the code that uses it.

# Assert observable behavior
Applies to: `**/src/test/**/*.kt`

Test real behavior through the unit's entry point (for example, `processRequest`). Do not use reflection that imitates Spring, `verify(never())` checks, assertions on behavior that exists only in a test double, or time-based `createdAt` assertions; time-based assertions are flaky.

# Reuse the existing test infrastructure
Applies to: `**/src/test/**/*.kt`

Build database tests on the existing test setup: use `BaseUnitWithDbLayerTest` rather than `SmokeTestBase` plus manual cleanup; add `@ActiveProfiles("test")`; enable features only in tests that need them; name SQL fixture directories after the test class; refresh materialized views in `@BeforeEach` through the service; and use realistic data.

# Precise test names, one scenario each
Applies to: `**/src/test/**/*.kt`

Name each test after the exact scenario it checks (avoid copy-pasted names) and split multi-case tests. Make assertion messages state the input, expected value, and actual value.

# Test file conventions
Applies to: `**/src/test/**/*.kt`

Name the tested instance `uut`, put helper and factory functions at the bottom of the test file, and make shared base test classes `abstract`.

# Test new logic and every side effect
Applies to: `**/src/test/**/*.kt`

Add a test for each new piece of logic and assert every side effect it has (for example, verify that the raw readme is written as well as the processed one).

# Explain the non-obvious why
Applies to: `**/*`

Add a short why-comment or KDoc, with a concrete example, for non-obvious decisions, abbreviations, new configuration properties, and similar-looking fields. Examples include refreshing before an alias swap and distinguishing `lastSyncedAt` from `lastProcessedAt`.

# Task links only in TODOs
Applies to: `**/*`

Give every `TODO`, temporary workaround, or leftover file a YouTrack `KTL-…` link. Keep task links out of ordinary comments, KDoc, and version-catalog comments; task links belong in the commit message.

# No redundant comments
Applies to: `**/*`

Remove comments that merely restate the code, such as a comment above a `require`, inline `/* param = */` hints, or `Entry point for the scheduled job` KDoc.

# Proofread
Applies to: `**/*`

Proofread identifiers, comments, migration texts, and MCP tool descriptions for typos and awkward wording.
