# klibs.io MCP — Claude Code benchmark

Measures what the klibs.io MCP server (and the KMP-libraries skill) buys an agent, by
driving a fixed set of scenarios through **real Claude Code** (via the Claude Agent SDK):
MCP tools, an optional Skill, the filesystem, and an LLM judge that **web-searches to
verify** the answer.

Unlike a flat MCP eval, this runner exercises the agent end to end (multi-turn, real tool
use), so the metrics reflect actual agent behaviour.

## What it compares

Three **variants**, all on the same runner / model / prompts / judge, so they compare
directly:

| variant     | MCP     | skill        | notes |
|-------------|---------|--------------|-------|
| `baseline`  | —       | —            | no klibs tools; agent answers from its own knowledge |
| `mcp`       | klibs   | —            | klibs tools on the main thread, base instruction only |
| `mcp_skill` | klibs   | inline skill | klibs tools + `skills/lib-expert-inline.md` in the system prompt |

> **⚠️ Skill parity — check before each benchmark run.** `mcp_skill` injects
> `cc-runner/skills/lib-expert-inline.md`, a *standalone copy* of the production skill at
> the repo root (`skills/kmp-libraries-expert/SKILL.md`). They are **not** linked. Before
> running, confirm the inline copy still reflects the real skill — otherwise `mcp_skill`
> measures a stale paraphrase, not what ships. (We keep a copy rather than loading the real
> skill via the SDK plugin/`skills` mechanism so the skill text is always-on and guaranteed
> in context; the trade-off is you must keep the two in sync by hand.)

## Scenarios (17)

`cc-runner/scenarios.py` — ported from the original suite:

- **S1–S5** explicit `getLatestVersion` lookups (version / build metadata).
- **S6–S10** explicit `searchProjects` discovery (keyword + platform/target filters).
- **I1–I7** implicit/agentic: the prompt names a *feature* (persistence, markdown, QR,
  PDF, encryption, charts, image loading) but never a library or a tool. The agent must
  read the **Notes** KMP fixture (`fixtures/notes-app`) via the filesystem MCP, infer that
  a KMP library is needed, and search for it.

Each scenario has:
- `requires` — the klibs tool that **must** be called (deterministic gate, checked against
  captured tool calls, not the judge).
- `rubric` — a text-quality grade for the **web-verifying** LLM judge.

Pass = required tool was called **AND** judge score ≥ `min_score`. The tool gate is
**skipped for `baseline`** (it has no klibs tools to call) — baseline passes on judge alone.

## Metrics captured (per scenario, per variant)

- pass / judge score, judge reason, whether the required tool was called (`tool_ok`)
- **billed_tokens** — price-weighted estimate (`input + 1.25·cache_creation +
  0.10·cache_read + output`); the honest cost-shaped number to compare across variants
- **ctx_tokens** — raw context read per turn, summed (cache-replay heavy, double-counts
  cached context across turns; use for "how big did context get", not for cost)
- output tokens, full token breakdown, duration, cost (client estimate), num turns
- which klibs tools were called + call count

## Prerequisites

(`baseline` needs only steps 2–3 — no klibs server.)

1. **Local klibs.io with prod data, MCP on `:8080`** (see repo `CLAUDE.md` / `workflow.md`):
   ```bash
   docker compose up -d
   ./scripts/copy_prod_db_to_local.sh -K klibs-prod -C klibs-postgres -L klibs -D klibs
   export PERSONAL_GITHUB_TOKEN=ghp_dummy_local_eval PERSONAL_AI_TOKEN=FAKE \
          SPRING_DOCKER_COMPOSE_ENABLED=false
   ./gradlew :app:bootRun
   ```
   Confirm: `POST http://localhost:8080/mcp` with `initialize` → 200.
2. **Claude Code CLI** installed and authenticated (the SDK drives it; no raw API key).
   The web-verifying judge needs WebSearch/WebFetch, i.e. network access.
3. **Python 3.12 + the SDK**, and **npx** (for the filesystem MCP server):
   ```bash
   cd mcp-eval/cc-runner
   uv venv --python 3.12 .venv && . .venv/bin/activate
   uv pip install -r requirements.txt
   ```

## Run

From `mcp-eval/cc-runner` with the venv active (and klibs up on `:8080` for the `mcp*`
variants):

```bash
# 1. fresh per-variant working copies of the Notes fixture (the filesystem MCP edits these;
#    keep fixtures/ pristine)
rm -rf ../.work && mkdir -p ../.work
for v in baseline mcp mcp_skill; do cp -r ../fixtures/notes-app ../.work/notes-app-$v; done

# 2. run a variant — all 17 scenarios, writes reports/<variant>.json
python runner.py --variant baseline
python runner.py --variant mcp
python runner.py --variant mcp_skill

# subset (no report file written):
python runner.py --variant mcp_skill --only s6 i2
```

`runner.py` judges each answer inline (web-verified), so a fresh run is self-contained.
Reports land in `cc-runner/reports/` (git-ignored).

## The judge

`runner.py:judge()` runs a fresh, tool-less-except-web Claude session that **must verify
factual claims via WebSearch/WebFetch** (Maven Central, GitHub) before scoring — it does
not trust its training memory for versions. This kills the "stale-cutoff" artifact where a
real, recent version is wrongly called fabricated.

**Caveat:** ground truth here is the *live klibs index*, but the judge validates against
*the web*. When klibs lags Maven Central (or vice-versa), a faithful answer that echoes the
index can disagree with the web and lose points. Read judge reasons, not just the score,
for version-recency scenarios.

## Files

```
mcp-eval/
  README.md                       this file
  fixtures/notes-app/             KMP Notes app fixture (implicit tier I1–I7)
  cc-runner/
    runner.py                     SDK runner: variants, token split, inline web-verified judge
    scenarios.py                  17 scenarios (prompt + requires + rubric + min_score)
    skills/lib-expert-inline.md   the KMP-libraries skill text (mcp_skill variant)
    requirements.txt
    reports/                      generated (git-ignored)
  .work/                          generated per-variant fixture copies (git-ignored)
```
