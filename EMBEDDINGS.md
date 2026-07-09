# Embeddings research & evaluation

This document collects everything done for the klibs.io **embeddings / semantic search** research:
what the code does today, what was added to measure it, what was changed in the indexing job, and how
to run it end to end.

For the deep, harness-level reference (config knobs, metric definitions, results template) see
[`scripts/eval/README.md`](scripts/eval/README.md). This file is the higher-level index of the whole effort.

---

## 1. Goal

Answer, with numbers, whether **semantic (embedding) search** beats the current
**PostgreSQL full-text search (FTS)** on klibs.io — using real user queries and standard IR metrics —
and lay the groundwork (a reusable harness) for the follow-up experiments (hybrid, ANN, composite input).

## 2. What already existed (the baseline)

- **Production search = FTS** (`core/search`): materialized view `project_index`, weighted `tsvector`,
  ranked as `ts_rank_cd(exact)*0.7 + ts_rank_cd(wildcard)*0.3 + log(stars+1)*0.7`.
- **Embeddings scaffolding** (`integrations/ai`): a pluggable `Embedder` + `EmbedderRegistry`, with one
  `pgvector` column per technique on the `project` table:
  - `openai-3-small` → `readme_embedding vector(1536)` (default)
  - `openai-3-large` → `readme_embedding_openai_large vector(3072)`
  - `openai-ada-002` → `readme_embedding_openai_ada vector(1536)`
- **Semantic endpoint**: `POST /search/projects/similar` → `SearchService.searchSimilarProjects`
  → `ORDER BY <column> <=> query_vector` (exact cosine distance, no ANN index yet).
- **Indexing job**: `ProjectEmbeddingService` filled every embedding column from the project README.

## 3. What was changed / added

### 3.1 Indexing: embed the full README instead of the minimized one

`app/src/main/kotlin/io/klibs/app/indexing/ProjectEmbeddingService.kt`

- `addReadmeEmbedding()` now resolves the owner login and reads the **full README** from S3/localstack
  via `readmeService.readReadmeMd(...)`, falling back to `project.minimizedReadme` only when the full
  README is missing or blank.
- Added a `MAX_README_LENGTH = 25_000` truncation guard (mirrors `ProjectIndexingService`) to stay
  within the OpenAI embedding token limit. Every registered embedder column is still filled unchanged.
- Note: the job only selects projects with **empty** embedding columns. To re-embed existing projects,
  `NULL` the `readme_embedding*` columns first, then run the app with localstack populated and an
  OpenAI key set.

### 3.2 Evaluation harness (E0) + embedding-model comparison (E1)

New code under `app/src/main/kotlin/io/klibs/app/eval/`:

| File | Role |
|---|---|
| `RankingMetrics.kt` | Pure nDCG@k / MRR / Recall@k functions. |
| `EvaluationModel.kt` | `EvaluationQuery` (text + graded judgments) and `QuerySet`. |
| `EvaluationQueryLoader.kt` | Loads the concept-query resource, top-N by frequency. |
| `Rankers.kt` | `FtsRanker` (baseline) + `SemanticRanker` (one per embedding column). |
| `RelevanceJudge.kt` | LLM-as-judge: grades a whole pooled candidate set 0–3 in one structured-JSON call. |
| `JudgmentStore.kt` | File cache of judgments; edit by hand to spot-correct, reused across runs. |
| `EvaluationReport.kt` | Aggregates per-query metrics → per-engine Markdown table. |
| `EmbeddingEvaluationRunner.kt` | Orchestrator; a property-gated `ApplicationRunner`. |

Tests (`app/src/test/kotlin/io/klibs/app/eval/`): `RankingMetricsTest`, `EvaluationReportTest`,
`EvaluationQueryLoaderTest` — 13 tests total, all passing.

### 3.3 A strong, fully-offline local embedder (`bge-large-local`)

`integrations/ai/src/main/kotlin/io/klibs/integration/ai/BgeLargeLocalEmbedder.kt`

To answer *"do we actually need OpenAI, or is a local model enough?"* a genuinely strong, fully
offline neural local embedder is provided:

- Runs a local sentence-transformers model (default `BAAI/bge-large-en-v1.5`, **1024 dims**) via
  [DJL](https://djl.ai/); after a one-time model download (~0.8 GB, cached) it needs **no API key,
  no network, and has no per-query cost**.
- New pgvector column `readme_embedding_bge_large vector(1024)` (migration
  `2026-Q3/2026-07-08_add_bge_large_embedding_to_project.yml`). It auto-plugs into indexing, the
  `/search/projects/similar` endpoint, and the evaluation harness via `EmbedderRegistry` — no changes
  to those layers.
- **Disabled by default.** Enable with `klibs.embeddings.bge-large.enabled=true` (the model is heavy);
  the model then loads **lazily** on first use, so enabling it downloads nothing at startup. The model
  can be swapped via `klibs.embeddings.bge-large.model-url`.
- Output is L2-normalized so cosine distance is comparable to the other embedders.

### 3.4 Query set from the real search log

- `scripts/eval/raw_search_queries.txt` — the raw real search log (input).
- `scripts/eval/extract_concept_queries.py` — normalizes, deduplicates, and drops exact library/brand
  names (e.g. `ktor`) and typo/noise fragments, keeping only **concept** queries.
- `app/src/main/resources/eval/concept-queries.txt` — the resulting query set (`query<TAB>frequency`,
  frequency-sorted). 486 concept queries extracted from 1840 raw rows.

## 4. How the harness works

```
raw_search_queries.txt                         (real query log, provided)
        │  extract_concept_queries.py           (normalize, dedup, drop library names + noise)
        ▼
app/src/main/resources/eval/concept-queries.txt  (concept queries ranked by frequency)
        │  EmbeddingEvaluationRunner (per query):
        │    1. rank with FTS + every embedding column
        │    2. pool the top-k results across engines
        │    3. grade the pool with the cached LLM judge (0–3)
        │    4. score each engine: nDCG@10, MRR, Recall@10/20, latency
        ▼
eval-output/embedding-eval-report.md            (results table)
eval-output/judgments.json                      (graded relevance, editable + reused)
```

## 5. How to run

### 5.1 Regenerate the query set (only if the raw log or denylists change)

```bash
python3 scripts/eval/extract_concept_queries.py \
    scripts/eval/raw_search_queries.txt \
    app/src/main/resources/eval/concept-queries.txt
```

### 5.2 Run the evaluation

Prerequisites:
- A **populated database** (prod-like local DB).
- The embedding columns you want to compare must be **populated** — the `bge-large-local`
  column works offline; the OpenAI columns are filled by the README-embedding job (section 3.1) and
  need an OpenAI key.
- An **OpenAI key** configured (used to embed queries for the OpenAI engines and to run the LLM judge).
- To include the strong local model, add `--klibs.embeddings.bge-large.enabled=true` (fills
  `readme_embedding_bge_large` during indexing and scores it in the harness).

```bash
./gradlew :app:bootRun --args='--spring.profiles.active=local --klibs.eval.enabled=true --klibs.embeddings.bge-large.enabled=true'
```

The runner writes `eval-output/embedding-eval-report.md` and caches judgments in
`eval-output/judgments.json`. Judgments are **reused** on the next run, so you can:
1. run once (LLM grades every pooled candidate),
2. open `judgments.json` and correct any wrong grades by hand,
3. re-run — cached grades are used (no new LLM calls) and metrics reflect your corrections.

### 5.3 Configuration

| Property | Default | Meaning |
|---|---|---|
| `klibs.eval.enabled` | `false` | Master switch; the runner bean only exists when `true`. |
| `klibs.eval.query-limit` | `150` | Top-N concept queries by frequency to evaluate. |
| `klibs.eval.pool-k` | `10` | Top-k per engine pooled for judging. |
| `klibs.eval.judgments-file` | `eval-output/judgments.json` | Judgment cache path. |
| `klibs.eval.report-file` | `eval-output/embedding-eval-report.md` | Report output path. |
| `klibs.embeddings.bge-large.enabled` | `false` | Enable the offline `bge-large-local` engine. |
| `klibs.embeddings.bge-large.model-url` | `djl://…/BAAI/bge-large-en-v1.5` | Local model to load. |

## 6. Metrics

- **nDCG@10** — ranking quality with graded relevance (position-discounted); the headline metric.
- **MRR** — how high the first relevant result sits.
- **Recall@10 / @20** — coverage of all relevant projects in the top-k.
- **p50 / p95 latency (ms)** — per-query wall time of the engine call.
- **cost** — LLM judge calls + per-engine query embeddings; token-level $ is in the existing
  `klibs.*` OpenAI metrics.

## 7. Results template (fill after a run)

> Corpus: `<N projects>`. Query set: top-150 concept queries from real logs. Judge: `gpt-5-mini`,
> graded 0–3 over pooled top-10. Date: `<date>`.

| engine | nDCG@10 | MRR | Recall@10 | Recall@20 | p95 ms | cost |
|---|---:|---:|---:|---:|---:|---|
| fts (baseline) | | | | | | free |
| bge-large-local | | | | | | free (offline) |
| openai-ada-002 | | | | | | $ |
| openai-3-small | | | | | | $ |
| openai-3-large | | | | | | $ |

Questions to answer in the writeup:
- Does any embedding model beat FTS on nDCG@10 for these short, keyword-like queries?
- Is `3-large` worth ~6.5× the price of `3-small`?
- **Does the free, offline `bge-large-local` match the OpenAI columns — do we actually need OpenAI,
  or is a strong local model enough?**

## 8. Verification done

- `./gradlew :app:compileKotlin` succeeds.
- `./gradlew :app:test --tests "io.klibs.app.eval.*"` passes 13/13.
- The extraction script re-runs cleanly (1840 raw → 486 concepts).

## 9. Known limitations / natural next experiments

- **E2 — composite input**: name / AI description / tags (which FTS weights highest) are still not
  embedded; test a composite document next.
- **E3 — hybrid**: lexical and vector are separate; RRF / weighted fusion usually wins.
- **E4 — ANN index**: every semantic search is an exact sequential scan; add HNSW/IVFFlat and measure
  recall/latency. The `3072`-dim `openai-3-large` column exceeds pgvector's ~2000-dim HNSW limit and
  needs `halfvec` or dimensionality reduction to be indexable.
- **E5 — filtered semantic search**: `searchSimilarProjects` applies no facets yet.
- **Judge bias**: LLM-as-judge bootstraps labels; spot-correct `judgments.json` before headline claims.
