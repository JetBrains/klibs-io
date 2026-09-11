# search-eval JVM E2E benchmark

Two tiers, two bounds:

| tier | bound | corpus | engine | runs | expectation |
|------|-------|--------|--------|------|-------------|
| **Regression** | lower — "search can't get worse than this" | **frozen prod snapshot** (Testcontainers) | OpenSearch (Testcontainers) | manual today; the tier meant for CI | floor cases **all green** |
| **Eval** | upper — "we strive for 100%" | **live prod-copy** (external DB) | OpenSearch (Testcontainers) | manual, by whoever changes search | some **red** = the signal |

Both are excluded from the regular `./kotlin test` run — each needs Docker and a corpus, and is enabled
explicitly by the commands below.

Both drive the production search path through OpenSearch, on the same image pinned in code, so their
headlines are comparable on a shared corpus. Neither uses the `docker compose` instance by default: its
data volume persists and its version is pinned elsewhere, so leftover indices and image drift would move
the numbers silently. The eval tier can opt into it for debugging — see below.

## Who can run this

Both tiers run against a copy of the production database, so they are JetBrains-internal: the corpus
lives in klibs.io's private storage and the scripts that move it around live in the private
[klibs-io-infrastructure](https://github.com/JetBrains/klibs-io-infrastructure) repository, under
`database/search-eval/`.

If you are an external contributor, you cannot run these tests — and you are not expected to. Open your
pull request as usual; a klibs.io maintainer runs the tiers for you and reports back on the PR, with the
failing cases if there are any.

If you are on the klibs.io team, clone the infrastructure repository next to this one. The commands below
assume that layout and are run from the root of this repository:

```
<parent>/klibs-io/                    # this repository
<parent>/klibs-io-infrastructure/     # the private one
```

## Regression tier — the deterministic floor

Needs Docker and the snapshot file at `e2e/build/search-eval/frozen.pgdump`.

Two containers, both started by the run: Postgres restored from the snapshot, and an empty OpenSearch
filled from it into `project-regression` / `package-regression`. The floor gates the production search
path, so the OpenSearch instance is pinned in code with no override — a floor measured on someone's
local cluster would not be a floor.

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
SEARCH_EVAL_SNAPSHOT_KEY=search-eval/frozen-<date>.pgdump.gz \
  ../klibs-io-infrastructure/database/search-eval/search-eval-fetch.sh
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
../klibs-io-infrastructure/database/search-eval/search-eval-freeze.sh   # prints the new SEARCH_EVAL_SNAPSHOT_KEY
```

## Eval tier — the aspirational target

Drives the production search path through **OpenSearch**, against eval-only indices (`project-eval`,
`package-eval`) that the run wipes and refills itself — the `@BeforeAll` calls `OpenSearchIndexer.sync`
for each index spec, rebuilding both from the DB. So the corpus is always whatever the DB holds right
now; no manual indexing step. Unlike the regression tier it does not refresh the Postgres mat views, so
the DB must already have them populated.

The DB is the only external dependency; OpenSearch is provided either way below.

### Seed the corpus DB

Once, and again whenever the corpus should move to fresher data. Needs VPN and cluster access; the
script lives in the infrastructure repository:

```bash
../klibs-io-infrastructure/database/copy_prod_db_to_local.sh -K klibs-prod -C klibs-postgres -L klibs -D klibs
```

Override the connection with `SEARCH_EVAL_DB_URL` / `_USER` / `_PASSWORD` (defaults:
`jdbc:postgresql://localhost:5432/klibs`, user and password `klibs`).

If the DB is empty the run fails fast — the test asserts the project index is non-empty before scoring
anything.

### Run with OpenSearch in a test container — the default

Nothing to start; the run brings up its own OpenSearch, pinned to the same image as the regression tier,
and throws it away at the end.

```bash
./kotlin test -m e2e --include-classes '*SearchEvalE2ETest' --jvm-args '-Dsearch.eval.tier=eval'
```

Use this for any number you intend to quote or compare — against an earlier eval run, or against the
regression tier's headline on the same corpus.

### Run against the persistent OpenSearch — for debugging a case

The container dies with the JVM, which is awkward mid-tuning: `_analyze` and `_explain` are how you find
out *why* a case ranks where it does. `SEARCH_EVAL_OS_URI` points the tier at the `docker compose`
instance instead, whose indices outlive the run.

```bash
docker compose up -d opensearch
SEARCH_EVAL_OS_URI=https://localhost:9200 \
  ./kotlin test -m e2e --include-classes '*SearchEvalE2ETest' --jvm-args '-Dsearch.eval.tier=eval'
```

```bash
# afterwards, against the alias the run logged.
# Credentials are the local demo pair in docker-compose.yml.
curl -sk -u '<user>:<password>' -H 'Content-Type: application/json' \
  'https://localhost:9200/<project-eval-alias>/_analyze' -d '{"analyzer":"tool_alias","text":"hilt"}'
```

### Reading the result

Compare runs only against each other on the **same** corpus — the headline is a function of the DB
contents, so a re-copied prod DB moves it independently of any search change.

**Progress readout**  
Every run records itself to `e2e/build/search-eval/last-run.json`
(local scratch, not committed) and diffs against the previous one, printing the headline delta plus
which cases gained or lost ground:

```bash
./kotlin test -m e2e --include-classes '*SearchEvalE2ETest' --jvm-args '-Dsearch.eval.tier=eval'   # before
# … make the search change …
./kotlin test -m e2e --include-classes '*SearchEvalE2ETest' --jvm-args '-Dsearch.eval.tier=eval'   # after — prints the delta
```
