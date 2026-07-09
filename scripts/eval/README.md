# Embeddings search evaluation (E0 + E1)

A reproducible harness that measures whether **semantic (embedding) search** beats the current
**PostgreSQL full-text search (FTS)** on klibs.io, using real user queries and quantitative IR metrics.

- **E0** — an evaluation set + metrics (this is the credibility anchor).
- **E1** — an embedding-model comparison across the embedding columns in the schema
  (`openai-3-small`, `openai-3-large`, `openai-ada-002`, and the strong offline
  `bge-large-local`) plus the FTS baseline.

## What already exists in the repo (the baseline)

- **Production search = FTS** (`core/search`): materialized view `project_index`, weighted `tsvector`,
  ranked as `ts_rank_cd(exact)*0.7 + ts_rank_cd(wildcard)*0.3 + log(stars+1)*0.7`.
- **Embeddings scaffolding** (`integrations/ai`): pluggable `Embedder` + `EmbedderRegistry`, one
  `pgvector` column per technique on the `project` table. Indexed by `ProjectEmbeddingService`
  from `project.minimizedReadme`.
- **Semantic endpoint**: `POST /search/projects/similar` → `SearchService.searchSimilarProjects`
  → `ORDER BY <column> <=> query_vector` (exact cosine, no ANN index yet).

## Pipeline

```
raw_search_queries.txt                      (real query log, provided)
        │  extract_concept_queries.py        (normalize, dedup, drop library names + noise)
        ▼
app/src/main/resources/eval/concept-queries.txt   (concept queries ranked by frequency)
        │  EmbeddingEvaluationRunner (per query):
        │    1. rank with FTS + every embedding column
        │    2. pool top-k results across engines
        │    3. grade the pool with the LLM judge (cached)
        │    4. score each engine: nDCG@10, MRR, Recall@10/20, latency
        ▼
eval-output/embedding-eval-report.md         (results table)
eval-output/judgments.json                   (graded relevance, editable + reused)
```

## Files

| File | Role |
|---|---|
| `scripts/eval/raw_search_queries.txt` | Raw real search log (input). |
| `scripts/eval/extract_concept_queries.py` | Extracts concept queries; drops exact library/brand names and typo fragments. |
| `app/src/main/resources/eval/concept-queries.txt` | Query set: `query<TAB>frequency`, frequency-sorted. |
| `io/klibs/app/eval/RankingMetrics.kt` | Pure nDCG@k / MRR / Recall@k. |
| `io/klibs/app/eval/Rankers.kt` | `FtsRanker` + `SemanticRanker` (one per embedding column). |
| `io/klibs/app/eval/RelevanceJudge.kt` | LLM-as-judge, grades a whole pool 0–3 in one structured-JSON call. |
| `io/klibs/app/eval/JudgmentStore.kt` | File cache of judgments; edit by hand to spot-correct. |
| `io/klibs/app/eval/EvaluationReport.kt` | Aggregates per-query metrics → per-engine table. |
| `io/klibs/app/eval/EmbeddingEvaluationRunner.kt` | Orchestrator; a property-gated `ApplicationRunner`. |

## Regenerate the query set (optional)

Only needed when the raw log changes or you tune the denylists in the script:

```bash
python3 scripts/eval/extract_concept_queries.py \
    scripts/eval/raw_search_queries.txt \
    app/src/main/resources/eval/concept-queries.txt
```

The script prints how many rows were kept as concepts vs dropped as library names / fragments / noise.

## Run the evaluation

Prerequisites:
- A **populated database** (use `scripts/copy_prod_db_to_local.sh` for a prod-like local DB).
- The embedding columns you want to compare must be **populated** (the `bge-large-local`
  column works offline; the OpenAI columns are filled by the README-embedding
  job and need an OpenAI key).
- An **OpenAI key** configured (used to embed the query for OpenAI engines and to run the LLM judge).
- To include the strong local model, enable it with `klibs.embeddings.bge-large.enabled=true` (this
  makes both the indexing job fill `readme_embedding_bge_large` and the harness score it). Its model
  (`BAAI/bge-large-en-v1.5`, ~0.8 GB) auto-downloads once via DJL, then runs fully offline.

```bash
./gradlew :app:bootRun --args='--spring.profiles.active=local --klibs.eval.enabled=true --klibs.embeddings.bge-large.enabled=true'
```

The runner writes `eval-output/embedding-eval-report.md` and caches judgments in
`eval-output/judgments.json`. Judgments are **reused** on the next run, so you can:
1. run once (LLM grades every pooled candidate),
2. open `judgments.json` and correct any wrong grades by hand,
3. re-run — cached grades are used, no new LLM calls, metrics reflect your corrections.

### Configuration

| Property | Default | Meaning |
|---|---|---|
| `klibs.eval.enabled` | `false` | Master switch; the runner only exists when `true`. |
| `klibs.eval.query-limit` | `150` | Top-N concept queries by frequency to evaluate. |
| `klibs.eval.pool-k` | `10` | Top-k per engine pooled for judging. |
| `klibs.eval.judgments-file` | `eval-output/judgments.json` | Judgment cache path. |
| `klibs.eval.report-file` | `eval-output/embedding-eval-report.md` | Report output path. |
| `klibs.embeddings.bge-large.enabled` | `false` | Enables the offline `bge-large-local` engine (indexing + harness). |
| `klibs.embeddings.bge-large.model-url` | `djl://…/BAAI/bge-large-en-v1.5` | Local model to load; override to try another. |

## Metrics

- **nDCG@10** — ranking quality with graded relevance (position-discounted); the headline metric.
- **MRR** — how high the first relevant result sits.
- **Recall@10 / @20** — coverage of all relevant projects in the top-k.
- **p50 / p95 latency (ms)** — per-query wall time of the engine call.
- **cost** — LLM judge calls + per-engine query embeddings; token-level $ is in the existing
  `klibs.*` OpenAI metrics.

## Results template (paste into the research doc, fill after a run)

> Corpus: <N projects>. Query set: top-150 concept queries from real logs. Judge: `gpt-5-mini`,
> graded 0–3 over pooled top-10. Date: <date>.

| engine | nDCG@10 | MRR | Recall@10 | Recall@20 | p95 ms | cost |
|---|---:|---:|---:|---:|---:|---|
| fts (baseline) | | | | | | free |
| bge-large-local | | | | | | free (offline) |
| openai-ada-002 | | | | | | $ |
| openai-3-small | | | | | | $ |
| openai-3-large | | | | | | $ |

Interpretation to write up:
- Does any embedding model beat FTS on nDCG@10 for these short, keyword-like queries?
- Is `3-large` worth ~6.5× the price of `3-small`?
- **Does the free, offline `bge-large-local` match the OpenAI columns — i.e. do we actually need
  OpenAI, or is a strong local model enough?**

## Known limitations / natural next experiments

- **README-only embeddings (E2)** — only `project.minimizedReadme` is embedded; name / AI description
  / tags (which FTS weights highest) are ignored. Test a composite input next.
- **No hybrid (E3)** — lexical and vector are separate; RRF / weighted fusion usually wins.
- **No ANN index (E4)** — every semantic search is an exact sequential scan; add HNSW/IVFFlat and
  measure recall/latency. Note: the `3072`-dim `openai-3-large` column exceeds pgvector's ~2000-dim
  HNSW limit and needs `halfvec` or dimensionality reduction to be indexable.
- **Judge bias** — LLM-as-judge bootstraps labels; spot-correct `judgments.json` for headline claims.
