# Spec: Suspicious fork auto-ban

## 1. Goal
Ban automatically the suspicious package candidates that are provably republished forks: a package under a `github`-namespaced `groupId` whose encoded owner does not own the repository it points at, where that owner holds a GitHub fork of that repository and nothing outside its own project is known to depend on it. Everything else is left for manual review.

## 2. Problem
Candidate collection records every `(project_id, artifact_id, group_id)` entry where one `artifactId` is published under several `groupId`s within a project — 2,132 rows against a production copy — all `PENDING`. Nothing acts on them, so every one waits on a person.

Part of that queue is mechanically decidable. When a `groupId` encodes a GitHub account (`io.github.<owner>`, `com.github.<owner>`) it can be compared against the owner of the repository the package resolves to, and a fork under the suspect's account confirms republication without judgement.

Affected: maintainers working the candidate queue, and authors whose libraries are republished under someone else's coordinates.

## 3. User scenarios & acceptance

### Scenario 1 — A confirmed fork is banned (P1)
- **Given:** a `PENDING` candidate whose encoded owner differs from the repository owner, where that owner holds a fork of the repository and nothing outside the candidate's project is known to depend on the coordinate.
- **When:** the job runs.
- **Then:** the coordinate is in `banned_packages` with a reason naming the forked repository, and the candidate row is `RESOLVED` with the same text in `notes`.
- **Independent test:** seed a conflict, stub the fork lookup, run the job, assert the ban row and the resolved candidate.

### Scenario 2 — A candidate with dependants is never auto-banned (P1)
- **Given:** a candidate meeting every other condition, but a package in another project depends on its coordinate.
- **When:** the job runs.
- **Then:** nothing is banned and the candidate is left `PENDING`.
- **Independent test:** seed a dependant in a second project, run the job, assert no ban row.

### Scenario 3 — An owner mismatch that is not a fork is left alone (P1)
- **Given:** a candidate whose encoded owner differs because the account was renamed or the repository transferred, so no fork exists.
- **When:** the job runs.
- **Then:** nothing is banned and the candidate stays `PENDING`.
- **Independent test:** stub the lookup to report no fork, assert no ban.

### Scenario 4 — A reviewer's decision is never overridden (P1)
- **Given:** a candidate a reviewer has already set to `RESOLVED`, which nonetheless meets every condition for an automatic ban.
- **When:** the job runs.
- **Then:** nothing is banned and the row keeps the reviewer's `status` and `notes`.
- **Independent test:** resolve a qualifying candidate by hand, run the job, assert no ban row and the row unchanged.

### Edge cases
- **The fork was renamed or deleted.** Neither is found, so the candidate stays `PENDING` — a miss costs a manual review, not a wrong ban.
- **GitHub is unavailable or rate-limited.** No decision is recorded; a failed lookup is never read as "not a fork".
- **The candidate no longer conflicts.** Collection runs right before the ban and deletes `PENDING` rows that no longer conflict, so the ban only sees current conflicts. `RESOLVED` rows are kept, since they hold a reviewer's decision.
- **A ban can remove a conflict during the run.** Banning one entry deletes its `package` rows, which can leave a sibling as the only `groupId` for that artifact. Candidates are read once, right after collection, so a ban never disqualifies a sibling in the same run.
- **The ban is refused.** `BlacklistService` refuses a coordinate that is already banned or no longer in `package`, e.g. after a manual ban during the run; a reviewer may also resolve the row first. Nothing is changed for that candidate.

## 4. Functional requirements
- **FR-001:** The system MUST ban a candidate only when all hold: it is still `PENDING`, so no reviewer has already decided it; its `groupId` is `io.github.<owner>` or `com.github.<owner>`; the encoded owner differs from the owner of the repository the candidate's project resolves to; that owner holds a GitHub fork of that repository; and nothing outside the candidate's own project is known to depend on the coordinate.
- **FR-002:** A ban MUST apply only to the exact `(groupId, artifactId)` evaluated — never to other artifacts under the same `groupId`, nor to artifacts not yet published.
- **FR-003:** When a coordinate is banned, the system MUST record a reason identifying the ban as automated and naming the forked repository, and MUST mark the candidate row `RESOLVED` with the same text in `notes`. If the ban is refused, the row MUST stay unchanged.
- **FR-004:** The system MUST NOT alter a candidate's `status` or `notes` except when banning that row's coordinate.
- **FR-005:** When the fork determination cannot be completed, the system MUST NOT ban. An incomplete determination MUST NOT count as evidence that no fork exists.
- **FR-006:** A candidate whose `(project_id, artifact_id)` no longer spans more than one `groupId` MUST NOT be banned. This is judged against the conflict set right after that run's collection, so banning one member MUST NOT disqualify its siblings within that same run.
- **FR-007:** Repeated runs MUST NOT produce duplicate ban rows or re-ban an already-banned coordinate.
- **FR-008:** Before each ban, collection MUST delete `PENDING` candidate rows it no longer detects as conflicting, and MUST keep `RESOLVED` ones.

## 5. Non-functional requirements
- **Dataset size:** small. Measured against a production copy with data through 2026-06-22: of the 2,132 candidate rows, 329 are `io.github.*` and 6 `com.github.*`; **119** have an encoded owner differing from the repository owner, collapsing to 41 `groupId`s and **44 distinct `(owner, repository)` pairs**. The qualifying set is one set-based query; only the fork lookup happens outside it.
- **External rate limits:** ~44 GitHub repository lookups per run. Candidates still `PENDING` are looked up again on each daily run, so a failed lookup is retried and a fork created later is caught. The lookups share the authenticated budget with the existing GitHub jobs. Exhausting the budget must degrade to "no decision", not "no fork" (FR-005).
- **Concurrency:** one daily job under one ShedLock name: collection, then the ban.
- **Observability:** per run, counts of stale rows deleted, evaluated, banned, and skipped-by-reason. Each ban is logged individually with its coordinate and the forked repository, since it deletes `package` rows. A refused ban is logged as an error.

## 6. Out of scope
- **Impersonation that is not a fork.** Copying source into a fresh repository leaves no fork relationship — 13 of the 69 labelled candidates. These stay `PENDING`.
- **Namespace-wide bans**, un-banning, and comparing artifact contents. All remain manual.
- **Collapsing namespace migrations to one `groupId`.** A separate concern from banning forks.

## 7. Klibs.io technical surface
- **Modules touched:** `core/package` gains the qualifying query and the stale-row delete; `integrations/github` gains a fork-parent lookup; `core/project` gains a transactional service that bans one candidate, next to `BlacklistService`; `app` adds the ban to the existing collection job. `core/project` already depends on `core/package`. Neither depends on `integrations/github`, so the fork lookup is orchestrated from `app`.
- **Database:** no schema change — no tables, columns, or migrations. Reads `suspicious_package_candidate` (KTL-4790), `package`, `project`, `scm_repo`, `scm_owner`, `package_dependency`; writes `banned_packages` and candidate `status`/`notes`, and deletes stale `PENDING` candidate rows. Implementation therefore depends on KTL-4790 being merged.
- **Persistence style:** JPA, matching `core/package`. The ban goes through `BlacklistService`, which handles the `banned_packages` row, the `package` deletion, and the project recompute; the indexing queue already refuses banned coordinates, so enforcement needs nothing new.
- **External integrations:** GitHub, one lookup per distinct `(suspect owner, repository)`. `GitHubRepository` carries no fork information, and it stays that way: Kohsuke's `getParent()` can make a hidden API call, and `toModel()` runs for every GitHub job. `GitHubIntegration` instead gains a separate method returning a repository's fork parent, used only by this feature.
- **Scheduled jobs:** no new job. The existing collection job in `app` (KTL-4790) runs collection and then the ban, gated on `klibs.indexing`, with one ShedLock name and an explicit `lockAtMostFor`.
- **Configuration / API / frontend:** unchanged. No new properties or endpoints.

## 8. Design decisions

### Decision — Only the suspect's `groupId` must be github-namespaced
- **Choice:** require the suspect to be `io.github.<owner>` or `com.github.<owner>`; place no constraint on the other coordinates in the conflict.
- **Why:** the comparison needs the owner encoded in the suspect's `groupId` and the repository's owner from `scm_owner.login`, which comes from the GitHub API rather than any `groupId`. The counterpart's owner is never parsed, so its format is irrelevant — and requiring it to be github-namespaced would exclude exactly the cases where an official coordinate is republished (`app.cash.zipline`, `io.coil-kt.coil3`, `cafe.adriel.lyricist`).
- **Rejected:** requiring a counterpart whose encoded owner matches the repository owner, which cuts the 119 qualifying rows to 32 and discards every case above.
- **Limitation:** requiring a mismatch misses a republisher whose package resolves to their *own* fork rather than the original — no mismatch, no candidate. In the labelled set that is 8 of 61 forks, so this condition reaches 87% of them. It is the funnel's largest recall gap and it sits before every other condition.

### Decision — Dependants are counted per coordinate, not per project
- **Choice:** query `package_dependency` for the suspect's exact `(groupId, artifactId)`, excluding dependants inside its own project.
- **Why:** `project.dependent_count` is per project, and the suspect shares a project with the coordinate it copied — that shared project is what makes them a conflict. It would measure the original's popularity instead: `cashapp/zipline`'s project reports 3 dependants and `coil-kt/coil`'s 82, neither about the copy. Excluding same-project dependants avoids blocking a ban to protect siblings the same operation removes.
- **Rejected:** `project.dependent_count`, which cannot express a coordinate-level fact at any granularity; and storing a per-candidate count, which would need keeping in sync.
- **Limitation:** `package_dependency` only holds dependencies declared by packages klibs has indexed — 290,278 of 551,881 packages contribute any edges. An application or unindexed consumer is invisible, so this establishes *no known dependants*, never *no dependants*. It is one-sided: a hit blocks the ban, a miss is not proof of safety. A cheap veto, not a guarantee — the fork check is what authorises the ban.

### Decision — Fork confirmation is a direct repository lookup
- **Choice:** look up `<suspect owner>/<repository name>` and require it to be a fork whose parent is the repository the candidate resolves to.
- **Why:** one call per pair. A fork normally keeps the original's name.
- **Rejected:** enumerating the owner's repositories and matching on parent — survives renames, but costs several paginated calls per owner.
- **Revisit if:** renamed forks prove common; a miss currently costs only a manual review.

### Decision — Collection and ban run in one job
- **Choice:** the collection job deletes stale `PENDING` rows, then runs the ban. The job itself is not transactional: collection commits before the ban reads, and each ban is its own transaction, so a refused ban rolls back only that candidate.
- **Why:** the ban always sees current conflicts, so its query needs no conflict check of its own. With two jobs, nothing sets which runs first.
- **Rejected:** two separate jobs, where the ban query must re-check every conflict because it can't know whether collection has run; and deleting `RESOLVED` stale rows too, which would send a reviewer-approved original back for review if a new fork appears.

## 11. Test strategy
- **Integration (`integrations/github`, recorded responses):** the fork-parent lookup returns the parent's full name for a fork, nothing for a non-fork or a missing repository, and throws on other errors.
- **DB-integration (`BaseUnitWithDbLayerTest`, method-level `@Sql`, stubbed `GitHubIntegration`):** the four scenarios end to end; the fork outcomes — right parent bans, while wrong parent, not a fork and missing repository leave the row `PENDING`, and a thrown lookup bans nothing (FR-005); that a same-project sibling dependant does *not* block a ban; that banning one of a republisher's artifacts leaves their other artifacts unbanned and still indexed (FR-002); that a refused ban leaves the row unchanged (FR-003); that a second run neither duplicates a ban nor re-processes one; that collection deletes stale `PENDING` rows and keeps stale `RESOLVED` ones (FR-008), and the ban that follows never bans a stale row, while two qualifying members of one conflict are both banned in a single run (FR-006); and that a non-qualifying candidate's reviewer-set `status` and `notes` survive (FR-004).
- *Reviewer-only, manual on staging:* run against a production copy with the lookup live, then confirm a banned coordinate is not re-indexed on the next indexing run.

## 12. Assumptions
- **The third segment of `io.github.*` / `com.github.*` is a GitHub login.** Logins are alphanumeric with hyphens and survive a `groupId` segment unchanged, so extraction is exact, not heuristic. Compared case-insensitively.
- **An owner mismatch alone is not evidence.** Renames and repository transfers produce the same mismatch, which is why the fork lookup authorises the ban rather than the mismatch.
- **A candidate and the coordinate it copied share a project**, since a project corresponds to one `scm_repo`. This is what lets `(project_id, artifact_id)` stand in for "same repository".
- **Banning is reversible in effect.** Deleting the `banned_packages` row makes the still-pending index request claimable, and the package re-indexes unaided.
- **The measured figures rest on partial coverage.** Only 69 of the 119 candidates appear in the labelled set; the other 50 have never had their fork status checked, so a first run's ban count is not known in advance.

## 13. References
- KTL-4790 — candidate collection, which produces this feature's input. FR-008 narrows its FR-010: stale `PENDING` rows are now deleted.
- KTL-4618 — 617 labelled pairs, each with a verdict and a fork determination, 69 of which this funnel produces. Both came from the same pass, so the fork flag is not an independently validated judgement; the recall figure above measures it against owner matching, which is computed independently of it.
- `BlacklistService` in `core/project` — the ban path, including package deletion and project recompute.
- `IndexingRequestRepository.findFirstForIndexing` in `core/package` — where `banned_packages` is enforced against the indexing queue.
