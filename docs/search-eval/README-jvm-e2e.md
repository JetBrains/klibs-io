# search-eval JVM E2E benchmark (KTL-4710)

Two tiers, two bounds:

| tier | bound | corpus | runs | expectation |
|------|-------|--------|------|-------------|
| **Regression** | lower — "search can't get worse than this" | **frozen prod snapshot** (Testcontainers) | manual today; the tier meant for CI | floor cases **all green** |
| **Eval** | upper — "we strive for 100%" | **live prod-copy** (external DB) | manual, by whoever changes search | some **red** = the signal |

Both are excluded from the regular `./kotlin test` run — each needs Docker and a corpus, and is enabled
explicitly by the commands below.

## Regression tier — the deterministic floor

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

```bash
./scripts/copy_prod_db_to_local.sh -K klibs-prod -C klibs-postgres -L klibs -D klibs   # seed prod-copy
./kotlin test -m app --include-classes '*SearchEvalE2ETest' --jvm-args '-Dsearch.eval.tier=eval'
```

**Progress readout**  
Every run records itself to `build/search-eval/last-run.json`
(local scratch, not committed) and diffs against the previous one, printing the headline delta plus
which cases gained or lost ground:

```bash
./kotlin test -m app --include-classes '*SearchEvalE2ETest' --jvm-args '-Dsearch.eval.tier=eval'   # before
# … make the search change …
./kotlin test -m app --include-classes '*SearchEvalE2ETest' --jvm-args '-Dsearch.eval.tier=eval'   # after — prints the delta
```