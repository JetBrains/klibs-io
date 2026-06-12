#!/usr/bin/env python3
"""Claude Code headless eval runner for the klibs.io MCP.

Drives the 17 scenarios (scenarios.py) through the Claude Agent SDK and judges each
answer with a web-verifying LLM, to compare three variants:

  baseline    no MCP, no skill   (agent answers from its own knowledge; filesystem only)
  mcp         MCP, no skill      (klibs tools on the main thread, base instruction only)
  mcp_skill   MCP + inline skill (klibs tools + skill text injected into the system prompt)

Pass = TOOL gate AND JUDGE gate:
  - TOOL gate: the klibs tool the scenario `requires` was actually called. Skipped for the
    baseline variant (it has no klibs tools to call).
  - JUDGE gate: web-verified judge score >= the scenario's min_score.

Per scenario we capture judge score, the fresh/cache token split, a billed-weighted token
estimate, duration, cost, num_turns and tool calls.

Run from mcp-eval/cc-runner with local klibs up on :8080 (not needed for baseline):
    . .venv/bin/activate
    python runner.py --variant mcp_skill              # all 17, writes reports/mcp_skill.json
    python runner.py --variant mcp --only s6 i2        # subset, no report file
"""
from __future__ import annotations

import argparse
import asyncio
import json
import re
import time
from collections import defaultdict
from pathlib import Path

from claude_agent_sdk import (
    AssistantMessage,
    ClaudeAgentOptions,
    ResultMessage,
    TextBlock,
    ToolUseBlock,
    query,
)

from scenarios import SCENARIOS, full_prompt

HERE = Path(__file__).resolve().parent
EVAL_ROOT = HERE.parent                          # mcp-eval/
SKILLS = HERE / "skills"
REPORTS = HERE / "reports"

KLIBS_URL = "http://localhost:8080/mcp"
MODEL = "claude-sonnet-4-6"

VARIANTS = ("baseline", "mcp", "mcp_skill")

KLIBS_TOOLS = ["mcp__klibs__searchProjects", "mcp__klibs__getLatestVersion"]
FS_TOOLS = ["mcp__filesystem__read_text_file", "mcp__filesystem__list_directory",
            "mcp__filesystem__directory_tree", "mcp__filesystem__search_files"]

KLIBS_SERVER = {"type": "http", "url": KLIBS_URL}


def fs_server(variant: str) -> dict:
    """Per-variant working copy so variants can run in parallel without edit conflicts."""
    root = EVAL_ROOT / ".work" / f"notes-app-{variant}"
    return {"type": "stdio", "command": "npx",
            "args": ["-y", "@modelcontextprotocol/server-filesystem", str(root)]}

BASE_INSTRUCTION = (
    "You are a Kotlin Multiplatform coding assistant working inside a KMP project. "
    "When a task needs a third-party capability, find a Kotlin Multiplatform library for "
    "it rather than guessing from memory. Never invent version numbers or coordinates."
)


def skill_text(name: str) -> str:
    return (SKILLS / f"{name}.md").read_text()


def make_options(variant: str) -> ClaudeAgentOptions:
    common = dict(model=MODEL, permission_mode="bypassPermissions",
                  cwd=str(HERE), max_turns=40)
    if variant == "baseline":   # no klibs MCP, no skill — answers from own knowledge
        return ClaudeAgentOptions(
            system_prompt=BASE_INSTRUCTION,
            mcp_servers={"filesystem": fs_server(variant)},
            allowed_tools=FS_TOOLS, **common)
    if variant == "mcp":
        sys = BASE_INSTRUCTION
    elif variant == "mcp_skill":
        sys = BASE_INSTRUCTION + "\n\n" + skill_text("lib-expert-inline")
    else:
        raise ValueError(f"unknown variant {variant}")
    return ClaudeAgentOptions(
        system_prompt=sys,
        mcp_servers={"klibs": KLIBS_SERVER, "filesystem": fs_server(variant)},
        allowed_tools=KLIBS_TOOLS + FS_TOOLS, **common)


def tool_ok(variant: str, requires: str, r: dict) -> bool:
    if variant == "baseline":           # no klibs tools available → gate N/A
        return True
    if requires == "getLatestVersion":
        return bool(r["called_getLatestVersion"])
    if requires == "searchProjects":
        return bool(r["called_searchProjects"])
    return True


# --- LLM judge: fresh query with WebSearch/WebFetch, returns {score, reason} ----
JUDGE_SYS = (
    "You are a strict grader for an AI coding assistant's answer. You have WebSearch and "
    "WebFetch tools — USE THEM to verify factual claims before scoring; do not rely on your "
    "training memory for versions or library names (it is stale).\n"
    "Procedure: (1) extract the concrete claims in RESPONSE (library names, coordinates, "
    "version numbers); (2) verify them via web search against authoritative sources "
    "(Maven Central / search.maven.org, the library's GitHub/docs); (3) score how well "
    "RESPONSE satisfies RUBRIC given what you verified.\n"
    "Scoring: a version that you CONFIRM exists on Maven Central is correct even if it looks "
    "newer than you expected. Penalize claims you confirm are fabricated (no such version / "
    "no such artifact). If a fact is genuinely unverifiable after searching, do not penalize "
    "it as fabricated — note it.\n"
    "Output ONLY a JSON object on the final line: "
    "{\"score\": <float 0..1>, \"reason\": \"<short, cite what you verified>\"}."
)


async def judge(prompt: str, response: str, rubric: str) -> dict:
    q = (f"PROMPT:\n{prompt}\n\nRESPONSE:\n{response}\n\nRUBRIC:\n{rubric}\n\n"
         "Verify the claims via web search, then return the JSON verdict.")
    opts = ClaudeAgentOptions(model=MODEL, system_prompt=JUDGE_SYS,
                              allowed_tools=["WebSearch", "WebFetch"], max_turns=12,
                              permission_mode="bypassPermissions", cwd=str(HERE))
    text = ""
    async for msg in query(prompt=q, options=opts):
        if isinstance(msg, AssistantMessage):
            for b in msg.content:
                if isinstance(b, TextBlock):
                    text = b.text
        elif isinstance(msg, ResultMessage) and msg.result:
            text = msg.result
    m = re.search(r"\{.*\}", text, re.S)
    if not m:
        return {"score": 0.0, "reason": f"unparseable judge output: {text[:120]}"}
    try:
        return json.loads(m.group(0))
    except json.JSONDecodeError:
        return {"score": 0.0, "reason": f"bad json: {text[:120]}"}


# --- run one scenario -------------------------------------------------------
async def run_scenario(variant: str, key: str) -> dict:
    sc = SCENARIOS[key]
    prompt = full_prompt(key)
    opts = make_options(variant)
    t0 = time.time()
    final: ResultMessage | None = None
    buckets = {"main": defaultdict(int), "sub": defaultdict(int)}
    tool_calls: list[str] = []

    async for msg in query(prompt=prompt, options=opts):
        if isinstance(msg, AssistantMessage):
            who = "sub" if msg.parent_tool_use_id else "main"
            u = msg.usage or {}
            for k in ("input_tokens", "output_tokens",
                      "cache_read_input_tokens", "cache_creation_input_tokens"):
                buckets[who][k] += u.get(k, 0) or 0
            for block in msg.content:
                if isinstance(block, ToolUseBlock):
                    tool_calls.append(block.name)
        elif isinstance(msg, ResultMessage):
            final = msg

    dur = time.time() - t0
    resp = final.result if final else ""
    verdict = await judge(prompt, resp or "", sc["rubric"])
    score = float(verdict.get("score", 0.0))

    # token accounting, summed across main + subagent buckets
    agg = defaultdict(int)
    for b in buckets.values():
        for k, v in b.items():
            agg[k] += v
    # ctx = raw context the model read each turn (cache-replay heavy, double-counts cache);
    # billed = price-weighted estimate (cache_read at 0.1x, cache_creation at 1.25x).
    ctx = agg["input_tokens"] + agg["cache_read_input_tokens"] + agg["cache_creation_input_tokens"]
    billed = round(agg["input_tokens"] + 1.25 * agg["cache_creation_input_tokens"]
                   + 0.10 * agg["cache_read_input_tokens"] + agg["output_tokens"])

    r = {
        "key": key, "variant": variant, "tier": sc["tier"], "requires": sc["requires"],
        "score": score, "min_score": sc["min_score"], "judge_reason": verdict.get("reason", ""),
        "no_error": bool(final and not final.is_error),
        "num_turns": (final.num_turns if final else None),
        "duration_s": round(dur, 1),
        "cost_usd": (final.total_cost_usd if final else None),
        "ctx_tokens": ctx, "billed_tokens": billed,
        "out_tokens": agg["output_tokens"],
        "tokens": dict(agg),
        "called_searchProjects": "mcp__klibs__searchProjects" in tool_calls,
        "called_getLatestVersion": "mcp__klibs__getLatestVersion" in tool_calls,
        "klibs_calls": sum(1 for t in tool_calls if t.startswith("mcp__klibs__")),
        "tool_calls": tool_calls,
        "result": resp,
    }
    r["tool_ok"] = tool_ok(variant, sc["requires"], r)
    r["passed"] = r["tool_ok"] and score >= sc["min_score"]
    return r


async def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--variant", default="mcp_skill", choices=VARIANTS)
    ap.add_argument("--only", nargs="*", help="scenario keys; default all 17")
    args = ap.parse_args()

    keys = args.only or list(SCENARIOS.keys())
    print(f"# variant={args.variant}  scenarios={len(keys)}  model={MODEL}\n")

    results = []
    for key in keys:
        r = await run_scenario(args.variant, key)
        results.append(r)
        mark = "PASS" if r["passed"] else "FAIL"
        gate = "" if r["tool_ok"] else " [TOOL-MISS]"
        print(f"{key:4} {mark} score={r['score']:.2f}/{r['min_score']:.2f}{gate}  "
              f"turns={r['num_turns']} dur={r['duration_s']}s "
              f"billed={r['billed_tokens']:,} cost=${r['cost_usd']:.4f} klibs={r['klibs_calls']}")
        if not r["passed"]:
            print(f"      judge: {r['judge_reason'][:140]}")

    passed = sum(1 for r in results if r["passed"])
    tot = lambda f: sum(r[f] or 0 for r in results)
    print(f"\n# {args.variant}: {passed}/{len(results)} pass  "
          f"billed={tot('billed_tokens'):,} ctx={tot('ctx_tokens'):,} "
          f"out={tot('out_tokens'):,} cost=${tot('cost_usd'):.3f} dur={tot('duration_s'):.0f}s")

    if not args.only:  # full run → persist
        REPORTS.mkdir(exist_ok=True)
        out = REPORTS / f"{args.variant}.json"
        out.write_text(json.dumps({
            "variant": args.variant, "model": MODEL,
            "summary": {"passed": passed, "total": len(results),
                        "billed_tokens": tot("billed_tokens"), "ctx_tokens": tot("ctx_tokens"),
                        "out_tokens": tot("out_tokens"), "cost_usd": round(tot("cost_usd"), 4),
                        "duration_s": round(tot("duration_s"), 1)},
            "results": results,
        }, indent=2))
        print(f"# wrote {out}")


if __name__ == "__main__":
    asyncio.run(main())
