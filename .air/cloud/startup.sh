#!/usr/bin/env bash
# Workspace setup for air.jetbrains.cloud (FLEET_WORKSPACE_SETUP_SCRIPT).
#   ENV               frontend env profile: test (default) | features | production | local
#   UIVERIFY_API_KEY  UI Verify project key, read by .mcp.json and by `uiverify check`
# Only a broken checkout or a failed `npm ci` aborts; every other step warns and continues.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
FRONTEND="$ROOT/frontend"
ENV_NAME="${ENV:-test}"
SKILLS_CLI="skills@1.5.23"
BROWSER_OK=0

log() { printf '[air-setup] %s\n' "$*"; }
warn() { printf '[air-setup] WARN: %s\n' "$*" >&2; }
fail() {
    printf '[air-setup] ERROR: %s\n' "$*" >&2
    exit 1
}

# ENV picks one of the committed frontend/.env.* profiles; .env.local is git-ignored.
setup_env_file() {
    local src="$FRONTEND/.env.$ENV_NAME"
    case "$ENV_NAME" in
        test | features | production) ;;
        local | example) src="$FRONTEND/.env.example" ;;
        *)
            warn "unknown ENV='$ENV_NAME', using 'test'"
            ENV_NAME=test src="$FRONTEND/.env.test"
            ;;
    esac
    [ -f "$src" ] || fail "missing ${src#"$ROOT/"}"
    cp "$src" "$FRONTEND/.env.local"
    log "ENV=$ENV_NAME -> frontend/.env.local ($(grep -m1 NEXT_PUBLIC_API_URL "$src"))"
}

# The marker keeps repeat runs cheap; it lives in the git-ignored node_modules.
install_node_deps() {
    local hash marker
    hash="$(md5sum "$FRONTEND/package-lock.json" | cut -d' ' -f1)" || fail "no frontend/package-lock.json"
    marker="$FRONTEND/node_modules/.air-npm-ci"
    if [ -f "$marker" ] && [ "$(cat "$marker")" = "$hash" ]; then
        log "npm dependencies up to date"
        return
    fi
    log "installing npm dependencies"
    (cd "$FRONTEND" && npm ci) || fail "npm ci failed"
    echo "$hash" >"$marker"
}

install_browser() {
    local -a cmd=(npx playwright install --with-deps chromium)
    if [ "$(id -u)" -ne 0 ] && ! sudo -n true 2>/dev/null; then
        cmd=(npx playwright install chromium)
        warn "no root: installing the Chromium binary only, its OS packages (libnss3, libgbm1, ...) cannot be added"
    fi
    log "installing Playwright Chromium"
    (cd "$FRONTEND" && "${cmd[@]}") || {
        warn "Chromium install failed, cdn.playwright.dev must be reachable"
        return
    }
    if (cd "$FRONTEND" && node -e 'require("playwright").chromium.launch({args:["--no-sandbox"]}).then(b=>b.close())') 2>/dev/null; then
        BROWSER_OK=1
    else
        warn "Chromium does not launch, its OS packages are missing"
    fi
}

# skills-lock.json is the source of truth for which skills a workspace gets.
install_skills() {
    local lock="$ROOT/skills-lock.json" name
    local -a entry args expected=() missing=()
    [ -f "$lock" ] || return 0
    while read -r -a entry; do
        args=()
        for name in "${entry[@]:1}"; do args+=(--skill "$name"); done
        expected+=("${entry[@]:1}")
        log "installing skills from ${entry[0]}: ${entry[*]:1}"
        (cd "$ROOT" && npx -y "$SKILLS_CLI" add "${entry[0]}" "${args[@]}" -a claude-code -a junie -y) ||
            warn "could not install skills from ${entry[0]}"
    done < <(node -e '
const skills = JSON.parse(require("node:fs").readFileSync(process.argv[1], "utf8")).skills || {};
const bySource = {};
for (const [name, e] of Object.entries(skills)) (bySource[e.sourceUrl || e.source] ??= []).push(name);
for (const [source, names] of Object.entries(bySource)) console.log(source, ...names);
' "$lock")

    # The CLI skips unknown names and still exits 0, so check what landed.
    for name in ${expected[@]+"${expected[@]}"}; do
        [ -d "$ROOT/.agents/skills/$name" ] || missing+=("$name")
    done
    [ "${#missing[@]}" -eq 0 ] || warn "not installed, renamed upstream? ${missing[*]} - update skills-lock.json"
}

# Only a full unshallow clears --is-shallow-repository, which 'uiverify check' needs to confirm
# a baseline commit. Costs ~1s and ~1.5MB here.
unshallow_clone() {
    [ "$(git -C "$ROOT" rev-parse --is-shallow-repository 2>/dev/null)" = true ] || return 0
    log "unshallowing the clone"
    git -C "$ROOT" fetch --quiet --unshallow || warn "could not unshallow, 'uiverify check' will confirm 0 baselines"
}

report() {
    local key=MISSING visual="unavailable, Chromium cannot run here"
    [ -z "${UIVERIFY_API_KEY:-}" ] || key=set
    [ "$BROWSER_OK" -eq 0 ] || visual=ready
    log "ENV=$ENV_NAME | UIVERIFY_API_KEY=$key | visual tests: $visual"
    log "run: cd frontend && npm run test:component | npm run test:visual | npm run dev"
    [ "$(curl -s -m 8 -o /dev/null -w '%{http_code}' https://uiverify.ai/api/mcp || true)" != 000 ] ||
        warn "uiverify.ai unreachable, allowlist it in the workspace egress policy or UI Verify only works in CI"
}

log "preparing $ROOT for air.jetbrains.cloud"
setup_env_file
install_node_deps
install_browser
install_skills
unshallow_clone
report
