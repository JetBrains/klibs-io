# search-eval JVM E2E benchmark (KTL-4710)

Two tiers, two bounds:

| tier | bound | corpus | engine | runs | expectation |
|------|-------|--------|--------|------|-------------|
| **Regression** | lower — "search can't get worse than this" | **frozen prod snapshot** (Testcontainers) | Postgres FTS | manual today; the tier meant for CI | floor cases **all green** |
| **Eval** | upper — "we strive for 100%" | **live prod-copy** (external DB) | OpenSearch (KTL-4711) | manual, by whoever changes search | some **red** = the signal |

Both are excluded from the regular `./kotlin test` run — each needs Docker and a corpus, and is enabled
explicitly by the commands below.

## Regression tier — the deterministic floor

Stays on Postgres FTS — it sets no `klibs.search.opensearch.*` properties, so the OS gate is off and
no OpenSearch instance is needed.

```bash
./scripts/search-eval-freeze.sh                                   # one-time/rare: pin the weekly backup
SEARCH_EVAL_SNAPSHOT_KEY=search-eval/frozen-<date>.pgdump.gz \
  ./scripts/search-eval-fetch.sh                                  # download + gunzip -> app/build/search-eval/frozen.pgdump
./kotlin test -m app --include-classes '*SearchRegressionTest' --jvm-args '-Dsearch.eval.tier=regression'
```

**Updating the floor** — when search improves so a new case passes, or the snapshot is refreshed:

```bash
./kotlin test -m app --include-classes '*SearchRegressionTest' \
  --jvm-args '-Dsearch.eval.tier=regression -Dsearch.floor.overwrite=true'   # rewrites app/src/test/resources/search-eval/floor.json
```

## Eval tier — the aspirational target

Drives the production search path through **OpenSearch**, against eval-only indices (`project-eval`,
`package-eval`) that the run wipes and refills itself — the `@BeforeAll` calls `refreshSearchViews()`,
which rebuilds both the Postgres mat views and the OS indices from the DB via `OpenSearchTempPopulator`.
So the corpus is always whatever the DB holds right now; no manual indexing step.

```bash
docker compose up -d opensearch                                                       # must be up on :9200
./scripts/copy_prod_db_to_local.sh -K klibs-prod -C klibs-postgres -L klibs -D klibs   # seed prod-copy
./kotlin test -m app --include-classes '*SearchEvalE2ETest' --jvm-args '-Dsearch.eval.tier=eval'
```

If OpenSearch is down or the DB is empty the run fails fast: `refreshSearchViews()` swallows exceptions,
so the test asserts the project index is non-empty before scoring anything.

Overridable via env: `SEARCH_EVAL_OS_URI` (default `http://localhost:9200`),
`SEARCH_EVAL_OS_PROJECT_INDEX` (`project-eval`), `SEARCH_EVAL_OS_PACKAGE_INDEX` (`package-eval`),
plus `SEARCH_EVAL_DB_URL` / `_USER` / `_PASSWORD` for the corpus DB.

Compare runs only against each other on the **same** corpus — the headline is a function of the DB
contents, so a re-copied prod DB moves it independently of any search change.

**Progress readout**  
Every run records itself to `app/build/search-eval/last-run.json`
(local scratch, not committed) and diffs against the previous one, printing the headline delta plus
which cases gained or lost ground:

```bash
./kotlin test -m app --include-classes '*SearchEvalE2ETest' --jvm-args '-Dsearch.eval.tier=eval'   # before
# … make the search change …
./kotlin test -m app --include-classes '*SearchEvalE2ETest' --jvm-args '-Dsearch.eval.tier=eval'   # after — prints the delta
```