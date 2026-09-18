# Plan: `suspicious-package-candidate-collection`

**Spec:** [spec.md](spec.md)

## Approach

Three layers, landed bottom-up. The table and its JPA mapping go first and ship empty — nothing reads or writes them, so they merge on their own. The collection statement lands second, by itself, because it holds the only real risk: the two-level `SELECT` has to count distinct `groupId`s rather than `package` rows, and getting that wrong fails silently in both directions (FR-011). The scheduled job lands last and is one repository call plus a log line.

Everything but the migration and the job lives in `core/package`. The DB-integration tests live in `app/src/test`, because `BaseUnitWithDbLayerTest` boots `io.klibs.app.Application` and no `core` module holds DB tests (spec §11). Every validation command needs Docker. The indexing pipeline is not touched.

## Milestone 1 — Table and entity
- **Files:** `app/src/main/resources/db/migration/2026-Q3/2026-09-07_create_suspicious_package_candidate_table.yml`, `app/src/main/resources/db/migration/db.changelog-master.yml`, `core/package/.../enums/CandidateStatus.kt`, `core/package/.../entity/SuspiciousPackageCandidateEntity.kt` and its `@IdClass` key
- **What:** create `suspicious_package_candidate` with the natural-key PK `(project_id, artifact_id, group_id)` and an `ON DELETE NO ACTION` FK to `project`, mapped with `@IdClass` per `TagEntity`/`TagKey`.
- **Validation:** `./gradlew :app:test` — `ddl-auto: validate` checks every mapped column against the Liquibase-created schema at startup, so a wrong name, an incompatible type or a broken `@IdClass` fails the run. Do **not** add a repository test exercising `CrudRepository` defaults.
- **Depends on:** —

## Milestone 2 — The collection statement
- **Files:** `core/package/.../repository/SuspiciousPackageCandidateRepository.kt` (new), `app/src/test/kotlin/io/klibs/core/pckg/repository/SuspiciousPackageCandidateCollectionDbTest.kt`, `app/src/test/resources/sql/SuspiciousPackageCandidateCollectionDbTest/*.sql`
- **What:** add `CrudRepository<SuspiciousPackageCandidateEntity, SuspiciousPackageCandidateKey>` — per `ProjectTagRepository`, and the source of the `count()` Milestone 3 logs — holding one `@Modifying @Transactional @Query(nativeQuery = true)` method, `insertMissingCandidates()`, native as both aggregate queries in `PackageRepository` already are: the two-level `SELECT` over `package` filtered `project_id IS NOT NULL` — shape per spec §13, which gives both acceptable forms and the constraint that Postgres rejects `COUNT(DISTINCT …) OVER (…)` — then `ON CONFLICT (project_id, artifact_id, group_id) DO NOTHING`, returning rows inserted.
- **Validation:** `./gradlew :app:test --tests "io.klibs.core.pckg.repository.SuspiciousPackageCandidateCollectionDbTest"`, working through the spec §11 assertion list — FR-001, FR-003, FR-004, FR-005, FR-006, FR-008, FR-009, FR-010, FR-011. FR-002 and FR-007 are structural and land with the schema in Milestone 1. The trap is FR-011's single-`groupId`-many-versions and one-version-conflict cases, which a flat `COUNT(*)` gets wrong in opposite directions; a native statement is not validated at startup, so this test is also the only thing that catches a mistyped column. `truncate.sql` needs no change — it scans `pg_tables`.
- **Depends on:** Milestone 1

## Milestone 3 — Scheduled job
- **Files:** `app/src/main/kotlin/io/klibs/app/job/CollectSuspiciousPackageCandidatesJob.kt`
- **What:** modelled on `RefreshDependentCountJob` — `@Scheduled(initialDelay = 0, fixedRate = 1, timeUnit = DAYS)`, `@SchedulerLock(name = "collectSuspiciousPackageCandidatesLock")` with no `lockAtMostFor`, the 10m default being ample for a 0.11s statement, and `@ConditionalOnProperty("klibs.indexing", havingValue = "true")` — calling the repository and logging rows inserted and total candidates (`count()`).
- **Validation:** no test for the job — `application-test.yml` sets `klibs.indexing: false` so the bean is unregistered in tests, and the level worth covering is the statement it calls (Milestone 2). Run against the restored production copy with `klibs.indexing=true`: the first run inserts **2,132** rows, an immediate second run inserts **0** (FR-008/FR-009).
- **Depends on:** Milestone 2
