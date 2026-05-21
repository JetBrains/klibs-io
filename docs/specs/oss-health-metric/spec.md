# Spec: OSS Health Metric

**Input:** verbatim seed text — preserved for traceability

> We need to implement OSS Health metric for project. Here is RFC, that contains a section describing this metric: `/Users/nikita.vlaev/Downloads/KTL-4246 Exploratory research for author-faced insights.md`. Do a thorough research on the mentioned papers and align the metric to the klibs.io usecase.

> ADJUSTMENT PROMPT:  Archived / disabled GitHub repo -- skip. GitHub returns 202 for /stats/participation or /stats/contributors -- use reliable graphql apis to compute that instead of relying on GH. as for the quota usage: spread it
across time, don't do heavy job all at once. do one repo per period of time. we also expose ossHealth in the search results DTO, yes. Prefer JPA and HQL for validation.

## 1. Goal
Compute and expose a per-project **OSS Health** score (0–100) — a composite, research-backed signal of repository activity and responsiveness — so that visitors of a klibs.io project page can judge whether a library is actively maintained without needing to skim GitHub themselves.

## 2. Problem
- klibs.io currently exposes only point-in-time GitHub fields per repository: `stars`, `open_issues`, `last_activity_ts`, `license`. None of these on their own tell a visitor whether a library is being actively maintained — a high-star library can be abandoned, a low-star one can be healthy.
- The research the RFC cites ([Iqbal et al., 2023](https://arxiv.org/abs/2309.12120v3)) shows that single raw indicators (stars, commit recency, issue counts) do not reliably identify OSS sustainability. A composite is needed.
- **Affected:** end users of klibs.io evaluating library trustworthiness; library maintainers who want a neutral, defensible signal of their project's activity surfaced on the platform.

## 3. User scenarios & acceptance

### Scenario 1 — Visitor sees health on a project page (P1)
- **Given:** a project whose backing GitHub repo has at least 12 weeks of recorded activity and the OSS Health score has been computed.
- **When:** the visitor opens the project details page (`/project/{ownerLogin}/{projectName}/details`).
- **Then:** the response includes an `ossHealth` field with an integer 0–100, plus the timestamp at which it was last computed.
- **Independent test:** seed `scm_repo` + an `oss_health_score` row with a fixture score; call the details endpoint; assert `ossHealth` is present and within [0, 100].

### Scenario 2 — Insufficient data is reported, not hidden silently (P1)
- **Given:** a project whose backing repo has fewer than 12 weeks of recorded commit/issue/PR activity, OR for which the drip job has not yet picked the repo up.
- **When:** the visitor opens the project details page.
- **Then:** `ossHealth` is `null` (or carries an explicit "insufficient data" indicator — see FR-008) and a machine-readable reason is conveyed so the frontend can render *"Insufficient activity data"* rather than a misleading "0".
- **Independent test:** seed a brand-new repo with one commit; assert the field is `null`/insufficient and not `0`.

### Scenario 3 — Health refreshes on a predictable cadence (P1)
- **Given:** a project whose `oss_health_score.computed_at` is older than 7 days (`klibs.oss-health.eligibility-window-days`).
- **When:** the drip job's next iteration runs.
- **Then:** that project is the staleest eligible repo, gets picked, and its score is recomputed using the latest 12-week window of commit/issue/PR data; the stored `computed_at` moves forward.
- **Independent test:** run the drip-job iteration in a test harness with a stubbed `GitHubIntegration`; assert the score row is upserted with the new `computed_at`.

### Scenario 4 — Rate-limit-aware drip (P1)
- **Given:** the drip job is about to process the next repo, and the remaining GitHub GraphQL points budget is below the safety margin.
- **When:** the iteration ticks.
- **Then:** the iteration short-circuits without making a GraphQL call; the repo's `computed_at` is left untouched so it remains eligible on the next tick; a `oss_health_skipped_rate_limit_total` counter increments.
- **Independent test:** unit test the iteration with a fake `GitHubIntegration.getRateLimitInfo()` that returns `remaining < safetyMargin`; assert no GraphQL call is made and the score row is unchanged.

### Edge cases
- **Repo younger than 12 weeks:** report insufficient data. Do not extrapolate (per RFC's "negative impact risk" note — a misleadingly low score is worse than no score).
- **Repo with zero issues / zero PRs over the window:** the Issue and PR sub-scores have undefined ratios. We treat the sub-component as `0` for the ratio term but **must not** zero out the entire `I` or `P` — see FR-009.
- **Repo with one contributor:** `TopContributorCommitShare = 1.0`, so the diversity sub-score becomes `0.6 * (1/5) + 0.4 * 0 = 0.12`. This is intended; a one-person project legitimately scores low on diversity.
- **Archived / disabled GitHub repo:** skip computation entirely. If a score row already exists for the repo, transition it to `status = NOT_APPLICABLE` and null out the integer `score` (so it stops being shown). The drip job filters these out at eligibility-select time.
- **GitHub stats-endpoint 202s avoided entirely:** we do *not* call REST `/stats/participation` or `/stats/contributors` — both return `HTTP 202 Accepted` on cold reads with no firm completion ceiling. Instead we derive weekly commit buckets and contributor share from a single paginated **GraphQL** commit-history query (see FR-002 and FR-005), which is deterministic and returns the same shape on every call.
- **Repository renamed / transferred:** the `nativeId` (numeric repo ID) is stable across renames; the score row must key on `scm_repo.id` (which is derived from `nativeId`), not on `owner/name`.
- **klibs egress NAT IP pool is shared** between `klibs-stage` and `klibs-features`; the drip job's GraphQL points draw must stay within the shared token's 5,000 pts/hr budget and yield well before exhaustion — see NFR-Rate-limits.

## 4. Functional requirements

- **FR-001:** System MUST compute an OSS Health score in `[0, 100]` per scm-repository, defined as `100 * (0.30*C + 0.25*I + 0.25*P + 0.20*A)`, where the four sub-scores `C, I, P, A ∈ [0, 1]` are computed per the formula below.
- **FR-002:** System MUST compute `C` (Commit Consistency) over the last 12 weeks of weekly commit buckets as `C = max(0, 1 - CV / 0.6)` where `CV = stdev(weekly_commits) / mean(weekly_commits)`. If `mean == 0`, then `C = 0` (no commits = no consistency). Weekly buckets are derived from a paginated **GraphQL** query against `repository.defaultBranchRef.target ... on Commit { history(since: now-12w) { ... committedDate, author { user { login } } } }`; bucket aggregation is done in-process. This deliberately replaces REST `/stats/participation`, which returns `HTTP 202` on cold reads.
- **FR-003:** System MUST compute `I` (Issue Responsiveness) as `I = 0.5 * min(1, IssueCloseRatio / 0.4) + 0.5 * max(0, 1 - MedianIssueCloseDays / 21)`. Both inputs come from a single paginated **GraphQL** query against `repository.issues(filterBy: { since: now-12w }, orderBy: { field: UPDATED_AT, direction: DESC })` returning `createdAt`, `closedAt`, `state` per issue:
    - `IssueCloseRatio = issues_closed_in_window / issues_opened_in_window`, both counted from the same result set (an issue counts as "opened in window" if `createdAt >= now-12w`; "closed in window" if `state == CLOSED && closedAt >= now-12w`).
    - `MedianIssueCloseDays = median(closedAt - createdAt)` in days, over issues closed in the window.
- **FR-004:** System MUST compute `P` (PR Management) as `P = 0.5 * min(1, PRMergeRatio / 0.5) + 0.5 * max(0, 1 - MedianPRMergeDays / 14)`, with `PRMergeRatio` and `MedianPRMergeDays` derived analogously to FR-003 but via a single paginated **GraphQL** `repository.pullRequests(orderBy: { field: UPDATED_AT, direction: DESC })` query returning `createdAt`, `mergedAt`, `closedAt`, `state` per PR. `PRMergeRatio = prs_merged_in_window / prs_opened_in_window` — a PR closed without merge counts in the denominator but **not** the numerator, per the RFC. `MedianPRMergeDays = median(mergedAt - createdAt)` over PRs *merged* in the window.
- **FR-005:** System MUST compute `A` (Author Diversity) as `A = 0.6 * min(1, ActiveContributors / 5) + 0.4 * (1 - TopContributorCommitShare)`. Both inputs are derived from the **same GraphQL commit-history result set used for FR-002** (`author.user.login` per commit), with no additional API call: `ActiveContributors` is the count of distinct non-null logins with ≥ 1 commit in the last 12 weeks; `TopContributorCommitShare = top_committer_commits / total_commits` in that window. If `total_commits == 0`, `TopContributorCommitShare = 0` and `ActiveContributors = 0` → `A = 0`. This deliberately replaces REST `/stats/contributors`, which returns `HTTP 202` on cold reads.
- **FR-006:** System MUST NOT introduce a separate daily snapshot job. All four sub-components' inputs are derived from the three GraphQL queries in the per-repo recompute pass (FR-002 commits, FR-003 issues, FR-004 PRs). The daily-diff plan from the source RFC is obsoleted by the GraphQL switch.
- **FR-007:** System MUST run the OSS Health recompute as a **drip job**: a `@Scheduled` task that fires every N seconds (default 30s — mirroring `GitHubRepositoryUpdatingJob`'s existing cadence) and processes **exactly one repo per iteration** — picking the staleest eligible repo (`oss_health_score.computed_at IS NULL OR < now() - 7 days`, excluding archived/disabled). This spreads the GitHub GraphQL points draw evenly across the week instead of bursting at a single cron tick, and keeps headroom for the existing 30-second `GitHubRepositoryUpdatingJob` that shares the same PAT + NAT egress. ShedLock name: `computeOssHealthLock`. The "weekly" cadence is a property of the *eligibility window* (`computed_at` cutoff), not the job firing frequency.
- **FR-008:** System MUST distinguish "score not yet computed" / "insufficient data" from "score is 0". The persisted shape is `(score INT NULL, computed_at TIMESTAMP NULL, status ENUM: OK | INSUFFICIENT_DATA | STALE)`. The API field is nullable; the response also carries a status enum.
- **FR-009:** System MUST treat *missing* sub-component inputs distinctly from *zero* sub-component inputs. Specifically: if a repo has zero issues opened in the window, its `IssueCloseRatio` ratio term contributes `0` (the project simply didn't get any issues to respond to), but the median term contributes its full `1.0` weight (there were no slow closes either). Same for PRs. The RFC's formula already accommodates this via the `min(1, …)` / `max(0, …)` clamps; this requirement just makes the intent explicit.
- **FR-010:** System MUST require **at least 12 weeks** of `scm_repo` history (since `created_at`) before computing a score. Repos younger than 12 weeks are marked `INSUFFICIENT_DATA`.
- **FR-011:** System MUST expose `ossHealth` on **both** API surfaces: `ProjectDetailsDTO` (nullable integer `ossHealth` plus the enum `ossHealthStatus`) and `SearchProjectResult` (nullable integer `ossHealth` only — a `null` is the "do not show" signal for the listing). This requires `project_index` to gain an `oss_health` column (additive view migration).
- **FR-012:** System MUST be controllable via a `klibs.*` feature toggle so the drip job can be disabled in `local` / `prod` independently (mirroring the existing `klibs.indexing` toggle pattern).
- **FR-013:** System MUST NOT compute or display an OSS Health score for archived or disabled GitHub repositories. The drip job MUST filter these out at eligibility-select time. If a previously-computed score exists for a repo that has since been archived, the row is updated to `status = NOT_APPLICABLE` and `score` is nulled.
- **FR-014:** System MUST NOT change the weights / thresholds from the RFC formula without an explicit spec amendment — they are deliberately frozen to the RFC's simplified CSI variant. (See §8 Option A vs B.)

## 5. Non-functional requirements

- **Performance:** Score computation is offline (job-driven), not request-time. The API read path (`/project/.../details`) MUST add no more than a single indexed lookup on the score table (or a join on `scm_repo_id`). No GitHub API call on the read path.
- **External rate limits:**
    - GitHub GraphQL: per-repo cost is **3 GraphQL queries** (commits → C + A, issues → I, PRs → P), each potentially paginated. GraphQL has a complexity-points budget of 5,000 pts/hr per token; each non-paginated query costs ~1 pt and each additional page adds proportionally. Even at a pessimistic ~30 pts/repo, the shared NAT pool comfortably accommodates ~150 repos/hr — well above what the drip job actually consumes.
    - The drip design (FR-007: one repo every ~30s ⇒ ~120 repos/hr) keeps comfortable headroom for the existing 30-second `GitHubRepositoryUpdatingJob`, which shares the same PAT + NAT egress (see `[[reference_klibs_egress_ips]]`). No burst contention with the existing job; no big-bang weekly cron.
    - The job MUST still check `GitHubIntegration.getRateLimitInfo()` (extended to surface GraphQL points if not already exposed there) and skip this iteration if `remaining < safetyMargin` (default 200), deferring the repo to the next tick.
- **Concurrency:** One new ShedLock key: `computeOssHealthLock`. The drip job and the existing `updateGitHubRepositoryLock` job both call GitHub and share the same PAT + NAT egress; the per-iteration rate-limit guard above is what keeps them out of each other's way.
- **Observability:**
    - Micrometer counter per new GraphQL query type (`oss_health.graphql.commits`, `oss_health.graphql.issues`, `oss_health.graphql.pulls`) plus pagination-page counters, consistent with the per-request-type pattern already in `GitHubIntegrationKohsukeLibrary`.
    - Counters / gauges for `oss_health_compute_success_total`, `oss_health_compute_failure_total{reason}`, `oss_health_insufficient_data_total`, `oss_health_skipped_archived_total`, `oss_health_skipped_rate_limit_total`.
    - INFO log line per repo with the four sub-scores so a surprising final number can be debugged without re-running.
- **Security:** Read-only endpoint addition (no auth boundary change). The score is non-sensitive aggregate metadata. Contributor logins fetched via GraphQL are used only for the `ActiveContributors` count and `TopContributorCommitShare` aggregation; no GitHub logins are persisted (we store only the resulting numeric `a_component`).

## 6. Out of scope

- Author-facing analytics (the P3 items in the RFC: page views, snippet copies, outbound clicks, search impressions/clicks). Those depend on the GitHub OAuth flow described in the RFC's "Authentication" section — separate spec.
- Number-of-dependents (P1 in the RFC) — already in the codebase (`project.dependent_count`). Not part of this spec.
- Maven downloads, pub.dev–style Likes/Points, trending. Not in this spec.
- Triangular-membership / fuzzy normalization from the original CSI paper (arXiv 2504.00542). We deliberately use the RFC's simplified linear-with-clamps form — see §8.
- Repository-centrality network analysis (arXiv 2405.07508). Explicitly rejected by the RFC as "too much computation for klibs purposes."
- Showing a numeric score below a display cutoff (the RFC suggests "show only if ≥ 40, else *Insufficient activity data*"). The **backend always returns the score**; the **display cutoff is a frontend concern**, except that we expose the `status` enum (FR-008) so the frontend can implement the rule trivially.
- Showing more than just `ossHealth` on search results — the listing surfaces the integer score only (or hides it on `null`). The `ossHealthStatus` enum is details-page-only.

## 7. Klibs.io technical surface

- **Modules touched:**
    - `core/scm-repository` — new sub-package `oss-health` (or a sibling module `core/oss-health`) housing the `OssHealthScore` JPA entity, repository, and the calculator. Keeps concerns out of `ScmRepositoryEntity` itself.
    - `integrations/github` — new GraphQL-backed methods on `GitHubIntegration`: `getCommitsSince(repositoryId, since): List<CommitMeta>` (returns `committedDate` + `authorLogin`), `getIssuesActivitySince(repositoryId, since): List<IssueActivity>` (`createdAt`, `closedAt`, `state`), `getPullRequestsActivitySince(repositoryId, since): List<PrActivity>` (`createdAt`, `mergedAt`, `closedAt`, `state`). The existing Kohsuke client is REST-only; the GraphQL queries go through the existing OkHttp + GitHub PAT path. New per-query micrometer counters. **No 202 handling needed** — GraphQL does not stall on these queries.
    - `app` — new scheduled drip job; new Liquibase migration; wiring in `ProjectDetailsService` and the search-results assembly path to surface the score.
    - `core/project` — additive change to `ProjectDetailsDTO`.
    - `core/search` — additive change to `SearchProjectResult` and the `project_index` materialized-view SQL.
- **Database:**
    - New JPA-managed table `oss_health_score`: `scm_repo_id INT PK (FK to scm_repo)`, `score INT NULL`, `status VARCHAR (enum: OK | INSUFFICIENT_DATA | STALE | NOT_APPLICABLE)`, `c_component NUMERIC(4,3)`, `i_component NUMERIC(4,3)`, `p_component NUMERIC(4,3)`, `a_component NUMERIC(4,3)`, `computed_at TIMESTAMP NULL`. Storing the sub-components alongside the final score makes debugging and a future "why is my score X?" breakdown trivial.
    - **No daily snapshot table** (per FR-006) — all inputs come from the per-iteration GraphQL pass.
    - Additive change to the `project_index` materialized view: gain a nullable `oss_health INT` column, sourced from `LEFT JOIN oss_health_score ON oss_health_score.scm_repo_id = project.scm_repo_id`. The view's defining SELECT is updated; a `LEFT JOIN` keeps it null-tolerant for repos with no score yet.
    - Migration folder: `app/src/main/resources/db/migration/2026-Q2/` (current quarter per the survey).
    - Additive-only: yes. No backfill of historical data needed — GraphQL pulls the last 12 weeks of history in one pass, so a mature repo can be scored on its first eligible drip tick post-deploy.
- **Persistence style:** **JPA + HQL** for the new `oss_health_score` table. Rationale per maintainer guidance: HQL gives compile-time-checked queries against the JPA model and a stronger validation surface than raw JDBC, and there's no high-throughput hot-path on this table (one read per details fetch — served via the materialized view for search — and one upsert per repo per eligibility window).
- **Search / materialized views:**
    - `project_index` gains a nullable `oss_health INT` column via a `LEFT JOIN` on the new `oss_health_score` table. Refresh cadence is unchanged (the existing 10-minute `MaterializedViewUpdatingJob` picks it up via `REFRESH MATERIALIZED VIEW CONCURRENTLY`). The view is recreated in the new migration (drop + create — note Liquibase additive-migration policy per `[[feedback_ask_before_additive_migration]]`).
    - `package_index` is unaffected (per-package, not per-project).
- **External integrations:**
    - GitHub GraphQL API (single endpoint `https://api.github.com/graphql`). Three queries per repo per recompute: `repository.defaultBranchRef.target ... on Commit { history }` (commits over 12 weeks), `repository.issues(filterBy: { since: ... })` (issue activity), `repository.pullRequests(orderBy: UPDATED_AT)` (PR activity). All paginate via standard `pageInfo { endCursor hasNextPage }`.
    - **No `HTTP 202` handling needed** — GraphQL does not stall on stats. Standard 5xx / secondary-rate-limit retry-after handling applies.
    - Authentication uses the existing `klibs.integration.github.personalAccessToken`. Scope: `public_repo` is sufficient.
- **Scheduled jobs:**
    - One new job: `OssHealthComputeJob`, `@Scheduled(fixedRate = 30s, initialDelay = 30s)` (rate configurable via `klibs.oss-health.fixed-rate-ms`). ShedLock name `computeOssHealthLock`. Each iteration: pick the single staleest eligible repo (`oss_health_score.computed_at IS NULL OR < now() - 7 days` AND repo is not archived/disabled), run the three GraphQL queries, compute, upsert. **No batch fan-out**, no per-cron burst.
    - No daily snapshot job. The existing `GitHubRepositoryUpdatingJob` is unchanged.
    - Idempotency: upsert keyed by `scm_repo_id`. Re-running for the same repo within the same eligibility window MUST yield an identical score (deterministic computation from the same 12-week window).
- **Storage:** No S3 / no local cache impact. README handling is untouched.
- **Configuration:** New `klibs.oss-health.*` properties — `enabled: Boolean = true`, `fixed-rate-ms: Long = 30_000`, `eligibility-window-days: Int = 7`, `rate-limit-safety-margin: Int = 200`. Profile defaults: enabled in `local` and `prod`; disabled in `test` via property override (per `[[feedback_property_toggles_over_profile]]`).
- **API surface:** Additive changes to **two** DTOs: `ProjectDetailsDTO` gains nullable `ossHealth: Int?` and `ossHealthStatus: String` (or enum); `SearchProjectResult` gains nullable `ossHealth: Int?` (status enum omitted — `null` is the "do not show" signal). OpenAPI doc auto-regenerates. **Not** a breaking change.
- **Frontend contract:** `klibs-frontend` needs to:
    1. Render the new field on **both** the project-details page (using `ossHealth` + `ossHealthStatus`) and the search-results listing (using `ossHealth` only — render nothing when null).
    2. On the details page, apply the RFC's "show only if ≥ 40, else *Insufficient activity data*" rule client-side, driven by the `ossHealthStatus` enum.
    3. Optionally render a tooltip explaining the four sub-scores (the breakdown is stored server-side; a future `/oss-health/{projectId}/breakdown` endpoint is cheap to add — out of scope for v1).

## 8. Design options considered

### Option A — Paper-faithful CSI (triangular membership functions)
Use the original CSI paper's normalization: each sub-component is normalized via a triangular function with the paper's target values (`μ_c = 0.25, σ_c = 0.25`, etc.) and the paper's stability thresholds (CSI ≥ 0.7 = stable).

- **Pros:** Defensible against academic critique; the paper's authors picked those constants for a reason.
- **Cons:** The paper itself is "conceptual" and "open to debate"; empirical validation is acknowledged-pending. Triangular membership functions are non-monotonic — a repo can score *worse* by getting *more* commits than the target, which is counterintuitive for authors and harder to explain.

### Option B — RFC's simplified linear-with-clamps form (recommended)
Use the formula in the RFC verbatim: linear normalization clamped by `min(1, x / threshold)` for "more is better" terms and `max(0, 1 - y / threshold)` for "less is better" terms. Always monotonic.

- **Pros:** Monotonic and explainable — "more closed issues is always better, up to the cap"; the RFC's thresholds (`IssueCloseRatio / 0.4`, `MedianIssueCloseDays / 21`, `PRMergeRatio / 0.5`, `MedianPRMergeDays / 14`, `ActiveContributors / 5`) are concrete and reviewable. Simpler to implement and to defend to an author who asks "why did my score drop?"
- **Cons:** Less "faithful" to the source paper; thresholds are picked by the RFC author, not from a published study. (Mitigation: the [Iqbal et al. paper](https://arxiv.org/abs/2309.12120v3) the RFC cites supports the *idea* of a composite over single signals but does not prescribe specific constants — so any choice is a judgment call.)

### Option C — Recompute on-demand from raw GitHub (no persistence)
Cache nothing; compute on first request per project per week and cache in-process.

- **Pros:** Minimal schema change; trivially correct.
- **Cons:** Hot path now depends on GitHub availability; shared NAT IP rate limits make this fragile under traffic; defeats the entire premise of a precomputed score.

**Decision:** **Option B**. Rationale: matches the RFC author's intent; monotonic and explainable; thresholds are reviewable in this spec rather than buried in a paper's appendix. Option A can be revisited if Option B's scores prove poorly calibrated against a hand-labeled sample of known-healthy / known-abandoned repos — but that's empirical follow-up work, not a v1 concern.

## 9. Key entities

- **OssHealthScore:** one row per `scm_repo`. JPA-managed. Stores the final integer score, the four sub-components (`c, i, p, a` as numeric for debugging), a status enum (`OK | INSUFFICIENT_DATA | STALE | NOT_APPLICABLE`), and a `computed_at` timestamp. Lifecycle: created lazily on first successful drip-job run for a repo; refreshed when `computed_at` becomes older than 7 days; transitions to `NOT_APPLICABLE` if the parent repo is archived/disabled; deleted only if the parent `scm_repo` is deleted (cascade).

## 10. Test strategy

- **Unit:**
    - `OssHealthCalculator` (pure function from `(weeklyCommits, openedClosedIssueCounts, medianIssueDays, openedClosedPrCounts, medianPrDays, activeContributors, topShare) -> Int score + components`). Test the formula edge cases: zero commits (C=0), zero issues, zero PRs, single contributor, perfectly balanced repo. Cover the FR-009 "missing vs zero" semantics with explicit cases.
    - Drip-job iteration unit test (with mocked `GitHubIntegration`) that verifies: (a) rate-limit yield (Scenario 4) defers the repo without state change, (b) an archived/disabled repo is filtered out at eligibility-select time without an API call, (c) idempotency — running the same iteration twice produces identical score rows.
- **DB-integration:** `BaseUnitWithDbLayerTest` subclasses for the new `OssHealthScore` JPA repository. Method-level `@Sql` seeds per `[[feedback_sql_seed_method_level]]`. Verify upsert idempotency and that the `project_index` materialized-view refresh picks up new score rows via the `LEFT JOIN`.
- **Web / smoke:** `SmokeTestBase` test that hits `/project/.../details` against a seeded fixture and asserts the new fields appear in the JSON response with the expected shape (null case and populated case). A second `SmokeTestBase` test on the search endpoint asserts `ossHealth` is present in `SearchProjectResult` JSON.
- *Reviewer-only — manual / staging:* deploy to `klibs-features` (per `[[feedback_never_prod_unprompted]]`), let the drip job run for a few hours, watch the new counters in the actuator output, spot-check 3–5 known repos (one obviously healthy, one obviously abandoned, one new) and confirm the scores feel directionally right. Compare against GraphQL points-budget headroom on the shared NAT IP pool.

## 11. Assumptions

- Per-repo GraphQL pagination is capped at ~3 pages (≤ 300 items) per query for issues and PRs. If a repo has more than 300 closed issues / PRs in 12 weeks, the median over the first 300 is a fine approximation; commit-history pagination is uncapped (we need all commits in the window for accurate weekly buckets and contributor share, but in practice 12 weeks of commits stays well within a handful of pages for typical klibs libraries).
- **No cold-start warmup**: because the GraphQL queries pull the last 12 weeks of history directly, a mature repo can be scored on its first eligible drip-job tick post-deploy. Only repos with `scm_repo.created_at > now - 12 weeks` are `INSUFFICIENT_DATA` (FR-010).
- Display semantics (`≥ 40 ⇒ show number`, `< 40 ⇒ show "Insufficient activity data"`) are a frontend concern. The backend always returns the raw number when computable; the `ossHealthStatus` enum is the contract. This separation lets us tune the display cutoff later without re-deploying the backend.
- The numeric `nativeId` of a GitHub repo is stable across renames/transfers (GitHub's documented behavior); we key the new table off `scm_repo.id`, which already keys off `nativeId`.

## 12. References

- RFC, source of truth for this spec: `/Users/nikita.vlaev/Downloads/KTL-4246 Exploratory research for author-faced insights.md`.
- *Introducing Repository Stability* — arXiv [2504.00542](https://arxiv.org/abs/2504.00542) (2025). Source of the CSI formula structure (weights 0.30 / 0.25 / 0.25 / 0.20). Paper acknowledges its "conceptual phase" status; we adopt the four-dimension structure but **not** its triangular-membership normalization (see §8).
- *Individual context-free online community health indicators fail to identify open source software sustainability* — arXiv [2309.12120v3](https://arxiv.org/abs/2309.12120v3) (2023, rev. 2024). Motivates the composite-over-single-metric choice; cited in problem statement.
- *Revealing the value of Repository Centrality in lifespan prediction of OSS Projects* — arXiv [2405.07508](https://arxiv.org/abs/2405.07508) (2024). Explicitly out of scope per the RFC (too compute-heavy).
- Existing klibs.io entities referenced: `ScmRepositoryEntity` (`core/scm-repository`), `ProjectDetailsDTO` (`core/project`), `GitHubIntegration` (`integrations/github`), `project_index` materialized view (`core/search`).
- Memory: `[[reference_klibs_egress_ips]]` (shared NAT pool — rate-limit budget is per-IP, not per-replica), `[[reference_ghratelimit_record_ctor]]` (kohsuke ctor-order gotcha), `[[feedback_property_toggles_over_profile]]`, `[[feedback_sql_seed_method_level]]`, `[[feedback_ask_before_additive_migration]]`.
