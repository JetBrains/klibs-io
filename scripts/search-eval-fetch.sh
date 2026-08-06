#!/usr/bin/env bash
# ------------------------------------------------------------------------------
# search-eval REGRESSION corpus — fetch (KTL-4710).
#
# Downloads the pinned frozen snapshot to build/search-eval/frozen.pgdump, where
# FrozenSnapshotPostgresConfig restores it into Testcontainers. Run before
# `./kotlin test -m e2e --include-classes '*SearchRegressionTest' --jvm-args '-Dsearch.eval.tier=regression'` (dev + CI).
#
# Credentials come from the environment if set (CI: inject AWS_ACCESS_KEY_ID,
# AWS_SECRET_ACCESS_KEY, SEARCH_EVAL_BUCKET, GCS_ENDPOINT), else from the
# klibs-readme-user k8s secret (local: needs VPN + kubectl).
#
# Required: SEARCH_EVAL_SNAPSHOT_KEY (e.g. search-eval/frozen-2026-07-17.pgdump.gz).
# Deps: aws CLI, gunzip; kubectl only if creds not in env.
# ------------------------------------------------------------------------------
set -euo pipefail

# GCS's S3 API returns checksums the aws CLI rejects by default — disable validation (as the app does).
export AWS_RESPONSE_CHECKSUM_VALIDATION="${AWS_RESPONSE_CHECKSUM_VALIDATION:-when_required}"
export AWS_REQUEST_CHECKSUM_CALCULATION="${AWS_REQUEST_CHECKSUM_CALCULATION:-when_required}"

NS="${SEARCH_EVAL_NS:-klibs-prod}"
SECRET=klibs-readme-user
KEY="${SEARCH_EVAL_SNAPSHOT_KEY:?set SEARCH_EVAL_SNAPSHOT_KEY (e.g. search-eval/frozen-<date>.pgdump.gz)}"
# Default lands under e2e/build — the regression test runs with the :e2e module as its cwd.
OUT="${SEARCH_EVAL_SNAPSHOT:-e2e/build/search-eval/frozen.pgdump}"

secret() { kubectl get secret "$SECRET" -n "$NS" -o jsonpath="{.data.$1}" | base64 -d; }

if [[ -z "${AWS_ACCESS_KEY_ID:-}" ]]; then
  export AWS_ACCESS_KEY_ID="$(secret AWS_ACCESS_KEY_ID)"
  export AWS_SECRET_ACCESS_KEY="$(secret AWS_SECRET_ACCESS_KEY)"
fi
BUCKET="${SEARCH_EVAL_BUCKET:-$(secret BUCKET_NAME)}"
ENDPOINT="${GCS_ENDPOINT:-$(secret GCS_ENDPOINT)}"
export AWS_DEFAULT_REGION="${AWS_DEFAULT_REGION:-auto}"

mkdir -p "$(dirname "$OUT")"
echo "### Fetching s3://$BUCKET/$KEY ..."
aws s3 cp "s3://$BUCKET/$KEY" "$OUT.gz" --endpoint-url "$ENDPOINT"
gunzip -f "$OUT.gz"
echo "$KEY" > "$OUT.key"
echo "### Snapshot ready: $OUT"
