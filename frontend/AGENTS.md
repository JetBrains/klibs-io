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
- `*.visual.test.tsx` — layout and styling regressions only. Runs as a separate Vitest project, so `test:component` skips it. Import the global CSS the component needs, pin an explicit viewport, and disable animations/transitions so frames are deterministic.
- `e2e-tests/*` — reserve for full journeys crossing routing or backend boundaries.

## Guardrails

- Never push; never delete branches.
- Keep diffs minimal and localized. No drive-by refactors, renames, or reformatting.
