---
description: Generate an implementation plan from an existing klibs.io spec; produces 2–4 milestones for review.
---

# /plan

Generate an implementation plan from the spec referenced below. The plan must follow `.claude/templates/plan.md` and respect CLAUDE.md's "Align → Execute" milestone gating.

## Input
$ARGUMENTS

If `$ARGUMENTS` is empty or doesn't reference a spec file, auto-discover the most recently modified file matching `docs/specs/*/spec.md` and use that.

## Process
1. **Read the spec.** Do not generate a plan if the spec still contains unresolved `[NEEDS CLARIFICATION]` markers — instead, list them and ask the human to resolve them in the spec first.
2. **The feature name is the spec's directory name** (under `docs/specs/`).
3. **Read `.claude/templates/plan.md`** for structure.
4. **Propose 2–4 milestones.** Constraints:
    - Each milestone is independently mergeable and testable.
    - Each milestone touches the minimum files needed.
    - Validation must be a runnable command or a concrete observation — not "manually verify it works".
    - If a milestone needs more than 4 lines of description, split it.
5. **Respect klibs.io module-by-feature boundaries.** A single milestone may touch multiple modules only if they're logically inseparable (e.g., a migration changeset + the JPA entity that maps it).
6. **Write the result to `docs/specs/<feature-name>/plan.md`.**
7. **STOP.** Report:
    - Path of the file written
    - Any spec ambiguity that forced a planning assumption
    - Any milestone you considered splitting further but chose not to, and why

Do not implement anything. The plan is the deliverable.
