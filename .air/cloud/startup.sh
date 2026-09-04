#!/usr/bin/env bash
# Workspace setup for air.jetbrains.cloud, run by Air as FLEET_WORKSPACE_SETUP_SCRIPT.
#
# Workspace variables:
#   ENV               frontend environment profile: test (default) | features | production | local
#   UIVERIFY_API_KEY  UI Verify project key, read by .mcp.json and by `uiverify check`/`upload`
#
# Best-effort by design: only a broken checkout or a failed `npm ci` aborts the setup, so a
# workspace still opens when the image or the network blocks a step. The final summary says what
# is usable and what is not.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
FRONTEND="$REPO_ROOT/frontend"
ENV_NAME="${ENV:-test}"
SKILLS_CLI="skills@1.5.23"
UIVERIFY_CLI="uiverify@1.4.0"
BROWSER_OK=0

log() { printf '[air-setup] %s\n' "$*"; }
warn() { printf '[air-setup] WARN: %s\n' "$*" >&2; }
fail() {
    printf '[air-setup] ERROR: %s\n' "$*" >&2
    exit 1
}

preflight() {
    [ -f "$FRONTEND/package-lock.json" ] || fail "no frontend/package-lock.json in $REPO_ROOT"
    if [ -n "${UIVERIFY_API_KEY:-}" ]; then
        log "UIVERIFY_API_KEY is set"
    else
        warn "UIVERIFY_API_KEY is not set: the uiverify MCP server and 'uiverify check' cannot authenticate"
    fi
}

# ENV picks one of the committed frontend/.env.* profiles; .env.local is git-ignored.
setup_env_file() {
    local src
    case "$ENV_NAME" in
        test | features | production) src="$FRONTEND/.env.$ENV_NAME" ;;
        local | example) src="$FRONTEND/.env.example" ;;
        *)
            warn "unknown ENV='$ENV_NAME', falling back to 'test'"
            ENV_NAME=test
            src="$FRONTEND/.env.test"
            ;;
    esac
    [ -f "$src" ] || fail "missing ${src#"$REPO_ROOT/"}"
    cp "$src" "$FRONTEND/.env.local"
    log "ENV=$ENV_NAME -> frontend/.env.local ($(grep -m1 '^NEXT_PUBLIC_API_URL=' "$src" || echo 'no NEXT_PUBLIC_API_URL'))"
}

# The marker keeps repeated runs cheap; it lives inside the git-ignored node_modules.
install_node_deps() {
    local hash marker
    hash="$(md5sum "$FRONTEND/package-lock.json" | cut -d' ' -f1)"
    marker="$FRONTEND/node_modules/.air-npm-ci"
    if [ -f "$marker" ] && [ "$(cat "$marker")" = "$hash" ]; then
        log "npm dependencies already match package-lock.json, skipping npm ci"
        return
    fi
    log "installing npm dependencies (npm ci)"
    (cd "$FRONTEND" && npm ci) || fail "npm ci failed"
    printf '%s\n' "$hash" >"$marker"
}

install_browser() {
    local -a install=(npx playwright install chromium)
    if [ "$(id -u)" -eq 0 ] || sudo -n true 2>/dev/null; then
        install=(npx playwright install --with-deps chromium)
    else
        warn "no root access: installing the Chromium binary only, its OS packages (libnss3, libgbm1, libasound2, ...) cannot be added here"
    fi
    log "installing Playwright Chromium"
    if ! (cd "$FRONTEND" && "${install[@]}"); then
        warn "Chromium install failed, cdn.playwright.dev must be reachable"
        return
    fi
    if (cd "$FRONTEND" && node -e 'require("playwright").chromium.launch({args:["--no-sandbox"]}).then(b => b.close())') >/dev/null 2>&1; then
        BROWSER_OK=1
        log "Chromium launches"
    else
        warn "Chromium is installed but does not launch, its OS packages are probably missing"
    fi
}

# skills-lock.json stays the single source of truth for which skills a workspace gets.
install_skills() {
    local lock="$REPO_ROOT/skills-lock.json"
    if [ ! -f "$lock" ]; then
        log "no skills-lock.json, skipping agent skills"
        return
    fi
    local name
    local -a entry args expected=() missing=()
    while read -r -a entry; do
        [ "${#entry[@]}" -gt 1 ] || continue
        args=()
        for name in "${entry[@]:1}"; do args+=(--skill "$name"); done
        expected+=("${entry[@]:1}")
        log "installing skills from ${entry[0]}: ${entry[*]:1}"
        (cd "$REPO_ROOT" && npx -y "$SKILLS_CLI" add "${entry[0]}" "${args[@]}" --agent claude-code --agent junie --yes) ||
            warn "could not install skills from ${entry[0]}"
    done < <(node -e '
const fs = require("node:fs");
const lock = JSON.parse(fs.readFileSync(process.argv[1], "utf8"));
const bySource = new Map();
for (const [name, entry] of Object.entries(lock.skills || {})) {
    const source = entry.sourceType && entry.sourceType !== "github" ? entry.sourceUrl : entry.source;
    if (!source) continue;
    if (!bySource.has(source)) bySource.set(source, []);
    bySource.get(source).push(name);
}
for (const [source, names] of bySource) console.log([source, ...names].join(" "));
' "$lock")

    # The CLI skips unknown skill names and still exits 0, so check what actually landed.
    for name in ${expected[@]+"${expected[@]}"}; do
        [ -d "$REPO_ROOT/.agents/skills/$name" ] || missing+=("$name")
    done
    if [ "${#missing[@]}" -gt 0 ]; then
        warn "not installed, renamed or removed upstream: ${missing[*]} (update skills-lock.json)"
    fi
}

# 'uiverify check' confirms no baseline commit on a shallow clone (it bails on
# --is-shallow-repository, so deepening does not help), and 'git diff master...' has no merge
# base at depth 1. Costs ~1s and ~1.5MB on this repo.
unshallow_git_clone() {
    if [ "$(git -C "$REPO_ROOT" rev-parse --is-shallow-repository 2>/dev/null)" = "true" ]; then
        log "unshallowing the clone"
        git -C "$REPO_ROOT" fetch --quiet --unshallow ||
            warn "could not unshallow, 'uiverify check' will confirm 0 baseline commits and diffs against master will fail"
    fi
    git -C "$REPO_ROOT" fetch --quiet origin master || warn "could not fetch origin/master"
}

warm_uiverify_cli() {
    log "pre-fetching $UIVERIFY_CLI into the npx cache"
    npx -y "$UIVERIFY_CLI" --help >/dev/null 2>&1 || warn "could not pre-fetch $UIVERIFY_CLI"
}

reachable() {
    local status
    status="$(curl -s -m 8 -o /dev/null -w '%{http_code}' "$1" 2>/dev/null || true)"
    case "$status" in
        "" | 000) return 1 ;;
        *) return 0 ;;
    esac
}

summary() {
    local -a blocked=()
    reachable https://uiverify.ai/api/mcp || blocked+=(uiverify.ai)
    reachable https://cdn.playwright.dev/ || blocked+=(cdn.playwright.dev)

    log "--- summary ---"
    log "ENV profile      : $ENV_NAME (frontend/.env.local)"
    log "npm dependencies : installed"
    log "UIVERIFY_API_KEY : $([ -n "${UIVERIFY_API_KEY:-}" ] && echo set || echo MISSING)"
    log "visual tests     : $([ "$BROWSER_OK" -eq 1 ] && echo 'ready' || echo 'unavailable, Chromium cannot run here')"
    log "ready to run     : cd frontend && npm run test:component | npm run test:visual | npm run dev"
    if [ "${#blocked[@]}" -gt 0 ]; then
        warn "blocked by the workspace egress policy: ${blocked[*]}"
        warn "ask the Air workspace admins to allowlist those hosts, otherwise visual snapshotting and UI Verify triage only work in CI"
    fi
}

log "setting up $REPO_ROOT for air.jetbrains.cloud"
preflight
setup_env_file
install_node_deps
install_browser
install_skills
unshallow_git_clone
warm_uiverify_cli
summary
