#!/usr/bin/env bash
#
# Air cloud environment startup for frontend work.
#
# Air runs this after cloning the repository and before the agent starts, on
# every environment launch - including task resume - so every step here must be
# idempotent.
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

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# This script runs in its own process, so variables it exports never reach the
# agent session; Air's mechanism for that is ~/.bashrc. Replace any earlier
# definition rather than appending, which keeps resume from adding duplicates
# and from leaving a stale value behind after a secret is rotated.
persist_for_agent() {
    local name="$1" value="$2" bashrc="$HOME/.bashrc" line
    line="$(printf 'export %s=%q' "$name" "$value")"
    touch "$bashrc"
    if ! grep -qxF "$line" "$bashrc"; then
        sed -i "/^export ${name}=/d" "$bashrc"
        printf '%s\n' "$line" >>"$bashrc"
    fi
}

# Done before the install below, so a failing install can't cost the agent its
# UI Verify credentials.
if [[ -n "${UIVERIFY_API_KEY:-}" ]]; then
    # Never echo the value: this output lands in downloadable environment logs.
    persist_for_agent UIVERIFY_API_KEY "$UIVERIFY_API_KEY"
    echo "==> UIVERIFY_API_KEY exported for the agent; the 'uiverify' MCP server can authenticate"
else
    echo "==> WARNING: UIVERIFY_API_KEY is unset, so the 'uiverify' MCP server" >&2
    echo "    in .mcp.json cannot authenticate. Add it to the environment secrets." >&2
fi

# Keep Playwright's ~500MB browser download out of the environment, and keep
# husky from breaking npm install (it expects a .git in the package root, but
# frontend/ is a subdirectory). The agent inherits both so that an npm install
# it runs later behaves exactly like the one below.
persist_for_agent PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD 1
persist_for_agent HUSKY 0
export PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1 HUSKY=0

# No lock-file hash bookkeeping: npm is already a no-op when node_modules
# matches package-lock.json.
echo "==> Installing frontend dependencies"
cd "$REPO_ROOT/frontend"
npm install --no-audit --no-fund

echo "==> Component test runner: vitest $(npx --no-install vitest --version)"
echo "==> Frontend environment ready"
