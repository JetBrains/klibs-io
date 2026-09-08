#!/usr/bin/env bash
#
# Air cloud environment startup for frontend work.
#
# Prepares the environment so an agent can:
#   - run the Vitest component suite: cd frontend && npm run test:component
#   - ask the UI Verify MCP server (see .mcp.json) what changed visually
#
# Deliberately out of scope:
#   - Playwright browsers. The visual (*.visual.test.tsx) and e2e suites run in
#     CI, so nothing here drives a real browser.
#   - Screenshot baselines. UI Verify holds them server-side; the agent reads
#     diffs over MCP instead of comparing locally.

set -euo pipefail

FRONTEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../frontend" && pwd)"
cd "$FRONTEND_DIR"

# Playwright arrives as a dev dependency of @vitest/browser-playwright and
# @playwright/test. Keep its ~500MB browser download out of the image.
export PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

# npm runs `prepare` (husky) on install, and husky expects a .git in the package
# root. frontend/ is a subdirectory of the repo, and git hooks are useless to an
# agent, so disable it rather than let install fail.
export HUSKY=0

# No lock-file hash bookkeeping: npm is already a no-op when node_modules
# matches package-lock.json.
echo "==> Installing frontend dependencies"
npm install --no-audit --no-fund

echo "==> Component test runner: vitest $(npx --no-install vitest --version)"

if [[ -z "${UIVERIFY_API_KEY:-}" ]]; then
    echo "==> WARNING: UIVERIFY_API_KEY is unset, so the 'uiverify' MCP server" >&2
    echo "    in .mcp.json cannot authenticate. Set it to inspect visual diffs." >&2
else
    echo "==> UIVERIFY_API_KEY is set; the 'uiverify' MCP server can authenticate"
fi

echo "==> Frontend environment ready"
