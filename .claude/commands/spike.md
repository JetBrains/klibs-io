---
description: Throwaway exploratory prototype to validate an idea — no spec, no plan, no tests. Output is a findings document.
---

# /spike

Run a throwaway exploration — to answer a specific question, test a hypothesis, or build a rough end-to-end version of something and see where it breaks. This command intentionally **suppresses** CLAUDE.md's Align → Execute milestone gating — that gate exists to keep production work disciplined, and it would defeat the purpose of a spike.

A spike is not production code. Its only deliverable is **learning**, captured in a findings document. The spike code itself may survive (as inspiration for the eventual real implementation) or be thrown away.

## Input
$ARGUMENTS

A specific question, hypothesis, or build target. Examples (small to large):
- "Can we extract dependents count from Maven Central without exceeding the solrsearch rate limit?"
- "Does the OssHealth score still make sense if we drop the issue-velocity component?"
- "Try PostgreSQL FTS `rank_cd` vs `ts_rank` — which gives better results for our queries?"
- "Build a rough end-to-end implementation of the OSS health metric per `<design doc link>` — find out where the design breaks down before we formalize."

If `$ARGUMENTS` is vague (e.g. "spike a thing"), STOP and ask for a specific question or build target.

## Process
1. **Pick a short name for the spike** — 3–6 words, lowercase with hyphens (e.g. `solrsearch-rate-limit-probe`). This becomes the directory under `docs/spikes/`.
2. **Stay in spike discipline, not spike size.** A spike can be tiny (one file probing one question) or large (a vibecoded end-to-end prototype touching many files). What makes it a spike is *throwaway intent* and *skipped production discipline* — not file count. If you find yourself wanting to add tests, match JPA/JDBC patterns, or polish error handling **because the code is going to ship**, you've stopped spiking — promote to `/specify`.
3. **Write code freely.** Skip tests, skip migrations, skip OpenAPI updates. Hardcode where useful. Use `println` / `logger.info` liberally. Hit real APIs when allowed.
4. **No production discipline.**
    - Do **not** match JPA vs JDBC patterns — pick whichever is faster to write.
    - Do **not** respect module-by-feature boundaries — put exploration code wherever is easiest.
    - Do **not** worry about backwards compatibility, error handling, or edge cases unless they're *the thing being explored*.
5. **Write findings to `docs/spikes/<spike-name>/findings.md`** with this shape:
    - **Goal:** the question, hypothesis, or build target this spike was exploring (the input)
    - **What I tried:** one bullet per major attempt
    - **What happened:** observations, numbers, errors
    - **Verdict:** Proceed / Abandon / Pivot — one line
    - **Implications:** what this means for the eventual real implementation (links to constraints, gotchas, surprise behaviors)
    - **Files touched:** path list (so `/specify --from-spike` can find them)
6. **STOP.** Report:
    - Path of the findings file
    - Verdict in one line
    - Whether you'd recommend running `/specify --from-spike <spike-name>` next, or that the spike's answer is "no, don't build this"

Spike code is **not meant to be merged.** Stay on a spike branch or revert before formalizing into a real spec.
