---
description: Formalize a completed klibs.io spike into a feature spec — treat the spike's findings (not its code) as the source of truth.
---

# /spec-from-spike

Formalize what a completed spike *taught* into a real spec ready for human review. The spike code is **not** the source of truth — the *learnings* are. Do not transcribe spike implementation choices into functional requirements; question them.

The spec must follow the template at `.claude/templates/spec.md` and respect the rules in `CLAUDE.md` (module-by-feature boundaries, minimal diff, no unsolicited refactors).

If you're starting fresh from a vague task description (no spike), use `/specify` instead.

## Input
$ARGUMENTS

`$ARGUMENTS` should be the spike's name (the directory name under `docs/spikes/`). If empty, ask which spike to formalize.

## Process
1. **Read `docs/spikes/<spike-name>/findings.md`** to understand the goal, verdict, and implications.
2. **Read the files listed under "Files touched"** in findings.md to see what the spike actually did — as secondary evidence only.
3. **Pick a fresh name for the production spec** — often different from the spike's name, since the spec is a deliberate redesign. Lowercase with hyphens, e.g. `oss-health-metric`.
4. **Read `.claude/templates/spec.md`** for the full structure.
5. **Fill in every applicable section**, with these extra rules:
    - Cite the spike in §12 References: `Spike: docs/spikes/<spike-name>/findings.md`.
    - In §8 Design options, capture what the spike ruled out (Option A = the approach the spike tried but found problematic; Decision = the refined approach). If the spike's approach was obviously right, skip §8.
    - In §11 Assumptions, include any constraint the spike discovered (rate-limit ceilings, schema gotchas, surprise API behaviors).
    - Spike code does not match production patterns (JPA vs JDBC, error handling, etc.). §7 must explicitly state which production patterns the real implementation will follow — don't inherit the spike's choices by default.
    - For anything you can't answer from the spike's findings, insert `[NEEDS CLARIFICATION: <specific question>]` inline. Do not invent details to fill silence.
    - Don't include why-now / priority-rationale / project-management context.
6. **Write to `docs/specs/<feature-name>/spec.md`** — never overwrite the spike. Both artifacts stay.
7. **STOP.** Do not generate a plan, tasks, or code. Report back:
    - Path of the spec written
    - Which spike findings shaped which spec sections
    - Anything the spike *did* that you're deliberately *not* carrying into the spec, and why
    - `[NEEDS CLARIFICATION]` markers and top-3 assumptions

The spec is the deliverable. Plan and implementation come later under separate commands.
