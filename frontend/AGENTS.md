# AGENTS.md

Next.js 14 (App Router) frontend for klibs.io. React 18 + TypeScript, JetBrains `@rescui/*` design system.

## Setup

```bash
npm install
cp .env.example .env.local   # NEXT_PUBLIC_API_URL points at the backend
```

## Commands

```bash
npm run dev                  # dev server on :3000
npm run build                # production build — run before claiming a change is done
npm run lint                 # ESLint (next/core-web-vitals + next/typescript); also a pre-commit hook
npm run test:component       # Vitest + React Testing Library (jsdom)
npm run test:visual          # UI Verify visual tests in real Chromium (needs npm run pw:install once)
npm run test:e2e             # Playwright user journeys (needs a running frontend)
```

## Structure

- `src/app/` — App Router routes; each route folder holds `page.tsx` and a `*-page-content.tsx` client part.
- `src/app/ui/<kebab-case>/` — one component per folder: `index.tsx` + `styles.module.css` + colocated tests.
- `src/app/api.ts` — all backend `fetch` calls, typed by `src/app/types.ts`. Add new calls here, not in components.
- `src/app/{types,helpers,constants,hooks,media,analytics}.ts` — shared primitives.
- `src/test/` — Vitest setup and fixtures. `e2e-tests/` — Playwright specs.

## Conventions

- Import via the `@/*` alias (`@/app/ui/container`), never deep relative paths.
- Server Components by default; add `'use client'` only for state, effects, or event handlers.
- Styling: CSS Modules (`styles.module.css`) composed with `classnames`; typography via `textCn` from `@rescui/typography`. No inline styles, no new CSS frameworks.
- Prefer existing `@rescui/*` and `@jetbrains/kotlin-web-site-ui` components over hand-rolled UI. Avoid adding dependencies.
- 4-space indent, 120-char lines, default export for components.
- Client-visible env vars must be prefixed `NEXT_PUBLIC_` and added to `.env.example`.

## Testing

Pick the cheapest level that proves the behavior:

- `*.test.tsx` — rendering, links, keyboard, filters. Assert user-visible outcomes via Testing Library queries; don't assert implementation details or call order.
- `*.visual.test.tsx` — layout and styling regressions only. Runs as a separate Vitest project, so `test:component` skips it. `src/test/visual-setup.ts` already loads the global CSS in layout order, stops animations, and swaps `next/image`, `next/link` and `next/navigation` for the doubles in `src/test/`; don't repeat any of that in a test file. Pin an explicit viewport. Test data (projects, packages, authors, API responses) lives in `src/test/fixtures.ts`, not in test files; add a factory there and import it. Answer API calls from `src/test/api.ts`, and use the helpers in `src/test/visual.ts` (`renderSingleAnimationFrame`, `waitForLayoutToSettle`) for anything else that varies between runs.
- `e2e-tests/*` — reserve for full journeys crossing routing or backend boundaries.

### Visual coverage

Every page and route state (`page`, `loading`, `error`, `not-found`) needs a visual test that renders it or its content component; `src/app/visual-coverage.test.ts` fails otherwise. That test can't see states inside a page, so when you add one (empty, error, a different layout), add a visual test for it too.

### When the UI Verify check reports changes on a PR

Triage the latest build on the PR with the `triage-visual-changes` skill (UI Verify MCP from `.mcp.json`), then
clear what doesn't need a person and summarize the rest:

1. **Rendering-only changes** (same text and layout; only glyph rendering, anti-aliasing or a few px of shift, e.g.
   a font rendering change after a dependency bump). For each story: `post_comment` one line on why it's safe
   ("font rendering changed after the dependency bump; same text and layout"), `resolve_comment` on it, then
   `review_diff` with `accept`. An open comment blocks the accept, so resolve first.
2. **Everything else** (content, color, size or layout changed, or you're not sure). `post_comment` one line on what
   changed and where ("card title wraps to two lines at 1024px"), leave it open and don't accept. Never accept a
   story someone else commented on.
3. **One PR comment** with `gh pr comment` (`--edit-last` if you posted one before): how many you accepted and why,
   then each story left for review as one line with its dashboard link and what changed. Lead with those; if none
   are left, say so.

## Guardrails

- Never delete branches.
- Never push to the main branch.
- Keep diffs minimal and localized. No drive-by refactors, renames, or reformatting.
