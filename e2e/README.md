# search-eval JVM E2E benchmark

Two tiers, two bounds:

| tier | bound | corpus | runs | expectation |
|------|-------|--------|------|-------------|
| **Regression** | lower — "search can't get worse than this" | **frozen prod snapshot** (Testcontainers) | manual today; the tier meant for CI | floor cases **all green** |
| **Eval** | upper — "we strive for 100%" | **live prod-copy** (external DB) | manual, by whoever changes search | some **red** = the signal |

Both are excluded from the regular `./kotlin test` run — each needs Docker and a corpus, and is enabled
explicitly by the commands below.

## Regression tier — the deterministic floor

Needs Docker and the snapshot file at `e2e/build/search-eval/frozen.pgdump`.

### Run the tier

This is the only command you need day to day:

```bash
./kotlin test -m e2e --include-classes '*SearchRegressionTest' --jvm-args '-Dsearch.eval.tier=regression'
```

If the snapshot file is missing, the test still runs, but on an empty database — so every case fails.
Download the file first.

### Download the snapshot

Once per machine, and again after `build/` is deleted. Needs VPN and kubectl, or AWS keys in the
environment:

```bash
SEARCH_EVAL_SNAPSHOT_KEY=search-eval/frozen-<date>.pgdump.gz ./scripts/search-eval-fetch.sh
```

The key to use is the `snapshot` field in `floor.json`. The fetch script saves the key it downloaded
next to the dump, and the tier fails if the two disagree — a floor only means something against the
corpus it was recorded on.

### Update the floor

Do this when search improves and a new case passes, or after switching to a new snapshot:

```bash
./kotlin test -m e2e --include-classes '*SearchRegressionTest' \
  --jvm-args '-Dsearch.eval.tier=regression -Dsearch.floor.overwrite=true'
```

Rewrites `e2e/src/test/resources/search-eval/floor.json`, stamping the snapshot it ran on — commit it.

### Make a new snapshot (rare)

Only when the corpus should move to newer data. Copies the weekly prod backup to a new fixed key;
needs prod access (VPN and kubectl). Then download it and update the floor:

```bash
./scripts/search-eval-freeze.sh   # prints the new SEARCH_EVAL_SNAPSHOT_KEY
```

## Eval tier — the aspirational target

```bash
./scripts/copy_prod_db_to_local.sh -K klibs-prod -C klibs-postgres -L klibs -D klibs   # seed prod-copy
./kotlin test -m e2e --include-classes '*SearchEvalE2ETest' --jvm-args '-Dsearch.eval.tier=eval'
```

**Progress readout**  
Every run records itself to `e2e/build/search-eval/last-run.json`
(local scratch, not committed) and diffs against the previous one, printing the headline delta plus
which cases gained or lost ground:

```bash
./kotlin test -m e2e --include-classes '*SearchEvalE2ETest' --jvm-args '-Dsearch.eval.tier=eval'   # before
# … make the search change …
./kotlin test -m e2e --include-classes '*SearchEvalE2ETest' --jvm-args '-Dsearch.eval.tier=eval'   # after — prints the delta
```
