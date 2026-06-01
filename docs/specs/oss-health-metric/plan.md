# Plan: `oss-health-metric`

**Spec:** [spec.md](spec.md)

## Approach

Build the feature bottom-up in four independently-mergeable slices. The **data model + pure formula** land first (a new `oss_health` table, a JPA entity/repository, and a side-effect-free `OssHealthCalculator`) — fully testable in isolation with no external calls. In parallel, the **GitHub GraphQL fetch** is added to `integrations/github` as a self-contained capability returning its own activity model, with no dependency on `core`. The **scheduled job** in `app` then wires the two together (it owns the mapping from the GitHub activity model to the calculator's input contract, the skip/insufficient logic, persistence, backoff, and metrics). Finally, **API exposure** recreates `project_index` to carry the raw score + status and adds the gated `ossHealth` field to the search and project-details DTOs.

Module boundaries are kept clean: `integrations/github` stays free of `core`, the calculator's input type lives in `core/scm-repository`, and `app` does the cross-module mapping (the existing "app orchestrates" pattern). The raw score is persisted and stored in the view; the `< 40` cutoff is applied only at DTO assembly (per §8). Note for M1: the new JPA entity/repository package must be covered by the app's entity/repository scanning (the module is otherwise JDBC) — verify alongside the existing JPA-based `core/package`.

## Milestone 1 — OSS Health data model + score calculator
- **Files:** `app/src/main/resources/db/migration/2026-Q2/2026-06-01_create_oss_health_table.yml` (+ register in `db.changelog-master.yml`); `core/scm-repository/.../health/{OssHealthEntity,OssHealthStatus,OssHealthRepository,RepositoryActivityInput,OssHealthCalculator}.kt`; tests `OssHealthCalculatorTest.kt`, `OssHealthRepositoryTest.kt`.
- **What:** Add the `oss_health` table (one row per `scm_repo`: raw score, four sub-scores, status enum, `computed_at`), its JPA `@Entity` + Spring Data JPA repository (with an HQL "find stalest / not-yet-computed" query), and the pure `OssHealthCalculator` implementing the §8 formula incl. zero-denominator handling. Ensure the new package is on the entity/repository scan path.
- **Validation:** `./gradlew :core:scm-repository:test --tests "*OssHealthCalculatorTest" --tests "*OssHealthRepositoryTest"` — calculator pins the §8 worked example + every edge case; repository test (Testcontainers) exercises the migration and HQL round-trip via `BaseUnitWithDbLayerTest`.
- **Depends on:** —

## Milestone 2 — GitHub GraphQL activity fetch
- **Files:** `integrations/github/.../GitHubIntegration.kt` (new method, e.g. `getRepositoryActivity(nativeId, since)`); `integrations/github/.../GitHubIntegrationKohsukeLibrary.kt`; `integrations/github/.../graphql/GitHubGraphQlClient.kt`; `integrations/github/.../GitHubRepositoryActivity.kt` (model); test `GitHubGraphQlActivityTest.kt`.
- **What:** Add an OkHttp-based GraphQL client (POST to `api.github.com/graphql` with the existing PAT) and one integration method returning a `GitHubRepositoryActivity` model: paginated 12-week commit history (weekly buckets + per-author counts), issues (`createdAt`/`closedAt`), PRs (`createdAt`/`mergedAt`), and `isArchived`/`isDisabled`. Add a Micrometer counter consistent with existing request metrics.
- **Validation:** `./gradlew :integrations:github:test --tests "*GitHubGraphQlActivityTest"` — drives the parser/pagination against recorded GraphQL JSON via `MockWebServer`; asserts the archived/disabled flags and the bucketed/aggregated fields.
- **Depends on:** —

## Milestone 3 — OSS Health computation job
- **Files:** `app/.../job/{OssHealthUpdatingJob,OssHealthUpdatingService}.kt`; config in `app/src/main/resources/application.yml` + `application-local.yml` (tick interval, threshold, enabled toggle) + `application-test.yml` (toggle off); test `OssHealthUpdatingServiceTest.kt`.
- **What:** `@Scheduled` job (rate from property, `@SchedulerLock("updateOssHealthLock")`) that picks the single stalest health record, fetches activity (M2), maps it to `RepositoryActivityInput`, sets `SKIPPED` for archived/disabled and `INSUFFICIENT_DATA` for repos < 12 weeks old, otherwise computes (M1) and persists; reuses the existing `BackoffProvider` and emits success/skipped/failed metrics + a pending-count gauge.
- **Validation:** `./gradlew :app:test --tests "*OssHealthUpdatingServiceTest"` — mocks integration + repository + calculator; asserts archived→`SKIPPED`, too-new→`INSUFFICIENT_DATA`, success→`COMPUTED` persisted, and failure→backoff with the prior score retained.
- **Depends on:** Milestone 1, Milestone 2

## Milestone 4 — Expose `ossHealth` in search & project details
- **Files:** `app/src/main/resources/db/migration/2026-Q2/2026-06-01_recreate_project_index_with_oss_health.{yml,sql}`; `core/search/.../{SearchProjectResult,SearchProjectResultDTO}.kt` + `ProjectSearchRepositoryJdbc.kt` (row mapper); `core/project/.../ProjectDetailsDTO.kt` (+ assembly site); display-threshold property wiring; OpenAPI annotations; tests `ProjectIndexOssHealthTest.kt` (DB-integration) + a `SmokeTestBase` case.
- **What:** Recreate `project_index` joining `project → scm_repo → oss_health` to carry raw score + status; add the gated `ossHealth` field to both DTOs, applying the `< 40` cutoff at DTO assembly (numeric only when `>= 40`; `"insufficient"` for `<40`/`INSUFFICIENT_DATA`; absent/null for `SKIPPED`/no row).
- **Validation:** `./gradlew :core:search:test --tests "*OssHealth*"` and `./gradlew :app:test --tests "*OssHealth*Smoke*"` — DB-integration asserts the view join surfaces the score (null for `SKIPPED`); smoke asserts the three JSON states are distinguishable in search + details responses.
- **Depends on:** Milestone 1

## Open questions carried from spec
- **None** — all `[NEEDS CLARIFICATION]` markers were resolved in the spec.
- Carry-forward notes (not blockers): (1) the JPA-in-a-JDBC-module divergence is a deliberate, user-requested choice flagged in §8 "Tension to confirm" — confirm entity scanning during M1. (2) §8 "Revisit if": validate the formula constants against a sample of real repos on `klibs-features` (the reviewer-only step in §11) before relying on the score distribution.
</content>
</invoke>
