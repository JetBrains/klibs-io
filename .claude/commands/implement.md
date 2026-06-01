---
description: Implement one milestone from an approved klibs.io plan; stops after the milestone per CLAUDE.md.
---

# /implement

Implement exactly one milestone from an approved plan. This command enforces CLAUDE.md's "Execute step" rule: one milestone, then STOP.

## Input
$ARGUMENTS

`$ARGUMENTS` should identify the milestone (e.g. `1`, `2`, or `next`). If empty or `next`, find the first milestone in the plan that hasn't been implemented yet (use git log and file state to determine).

## Process
1. **Read the plan** under the most recently modified `docs/specs/*/plan.md`. Confirm the milestone exists and resolve `next` if used.
2. **Read the spec** in the same directory for context.
3. **Implement only the chosen milestone.** Constraints:
    - Touch only the files listed for that milestone, plus tests for those files. If you must touch additional files, STOP first and explain why.
    - Match existing patterns (JPA vs JDBC, annotation style, test base class) — grep before inventing.
    - Run the milestone's validation command before declaring done.
4. **STOP after the milestone.** Do not start the next milestone, even if it looks small. Report:
    - Per-file change summary (one line each)
    - Validation: command run + result
    - Risks / follow-ups
    - Any deviation from the plan, and why

Do not commit or push. The human reviews and commits.
