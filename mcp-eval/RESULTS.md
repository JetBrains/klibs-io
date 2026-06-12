# klibs.io MCP benchmark — results

Date: 2026-06-13 · Model: `claude-sonnet-4-6` · 17 scenarios (S1–S10, I1–I7).
Runner: `cc-runner` (Claude Agent SDK, real Claude Code, full tool access).

## Variants

All three run under the **same realistic Claude Code env** — the agent has the full tool
set (Bash, Read, WebSearch, …); the only difference is whether the klibs MCP and/or the
skill are present. This mirrors how a real user runs Claude Code.

| variant     | klibs MCP | skill        |
|-------------|-----------|--------------|
| `baseline`  | —         | —            |
| `mcp`       | ✅        | —            |
| `mcp_skill` | ✅        | inline skill |

## Headline

| metric | baseline | mcp | mcp_skill |
|---|---|---|---|
| pass rate | 13/17 | **14/17** | **14/17** |
| cost (USD) | **$8.09** | $2.70 | **$2.05** |
| duration | 1750s | 941s | **728s** |
| ctx tokens | 10.2M | 7.79M | **5.82M** |
| output tokens | 9,045 | 4,830 | **2,901** |

> `ctx tokens` = raw context read per turn, summed (cache-replay heavy — a "how big did
> context get" indicator, not money). **`cost` is the price-correct number** to compare.
> Single run per variant — treat as directional; for a publishable figure run N≥3.

## What it shows

- **klibs MCP makes the agent both more correct *and* far cheaper.** Without klibs the
  agent flails — it web-searches and bashes around to find libraries, burning **~3–4× the
  cost and ~2.4× the wall-clock** of the klibs-equipped runs, and still scores lower
  (13 vs 14). Grounding discovery in the index short-circuits all that wandering.
- **The skill adds no correctness over raw MCP (14 = 14) but trims cost a further ~24% and
  time ~23%** — it makes the agent decisive instead of exploratory. Sharpest on the
  implicit tier (e.g. i2 markdown: `mcp` 33 turns / 1.5M ctx → `mcp_skill` 19 turns / 0.8M).

## Per-scenario (web-verified judge)

| id | tier | baseline | mcp | mcp_skill | note |
|----|------|:--------:|:---:|:---------:|------|
| s1 | getLatestVersion | ✅ | ✅ | ✅ | |
| s2 | getLatestVersion | ✅ | ✅ | ✅ | |
| s3 | getLatestVersion | ❌ | ✅ | ✅ | baseline stated wrong okio version |
| s4 | getLatestVersion (neg) | ✅ | ✅ | ✅ | not-found handled |
| s5 | getLatestVersion | ✅ | ✅ | ✅ | |
| s6 | searchProjects | ❌ | ✅ | ✅ | baseline: borderline, 8 web searches, $1.35 |
| s7 | searchProjects | ✅ | ✅ | ✅ | |
| s8 | searchProjects | ✅ | ❌ | ❌ | mcp/skill: some named JS libs don't actually support JS |
| s9 | searchProjects | ✅ | ✅ | ✅ | |
| s10| searchProjects | ✅ | ✅ | ✅ | baseline outlier: $2.22 |
| i1 | persistence | ✅ | ✅ | ✅ | |
| i2 | markdown | ✅ | ✅ | ✅ | |
| i3 | QR | ❌ | ✅ | ✅ | baseline never inferred QR |
| i4 | PDF | ❌ | ❌ | ✅ | **skill win** — mcp punted to HTML, skill found a real PDF lib |
| i5 | encryption | ✅ | ✅ | ✅ | baseline outlier: $1.54 |
| i6 | charts | ✅ | ✅ | ❌ | **skill regression** — gave up after an empty search (TOOL-MISS) |
| i7 | image loading | ✅ | ❌ | ❌ | mcp/skill assumed Coil present, never called searchProjects (TOOL-MISS) |

(`mcp`/`mcp_skill` pass = required klibs tool was called AND judge ≥ min_score; `baseline`
has no klibs tool so it passes on judge alone.)

## Method notes

- **Web-verifying judge.** Scores against WebSearch/WebFetch checks (Maven Central, GitHub),
  not training memory — kills the "stale-cutoff" artifact that earlier produced a false
  2/17. Caveat: ground truth is the *live klibs index* but the judge checks *the web*; when
  they diverge a faithful answer can lose points (read judge reasons on version scenarios).
- **Tool gate** (`mcp`/`mcp_skill` only): the scenario's required klibs tool must actually
  be called — a TOOL-MISS fails regardless of answer quality (see i6/i7).
- **High variance, single run.** baseline's total is dominated by 3 outliers (s6 $1.35,
  s10 $2.22, i5 $1.54 ≈ $5 of $8) where the tool-rich agent went on long web/bash hunts.
  i4/i6 are single-scenario flips that may be noise. Run N≥3 before quoting hard numbers.
- **Env caveat:** the agent's `Skill` tool exposed the operator's personal `~/.claude`
  skills (not a real klibs user's set). Identical across variants, so it does not bias the
  comparison, but inflates the "realistic env" claim slightly.

## Reproduce

See `README.md`. All three variants are produced by `cc-runner/runner.py`
(`--variant baseline|mcp|mcp_skill`) with its inline web-verifying judge.
