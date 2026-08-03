#!/usr/bin/env bash
# ------------------------------------------------------------------------------
# search-eval REGRESSION corpus — freeze (KTL-4710).
#
# Pins the current weekly prod backup as the frozen regression snapshot. The weekly
# cronjob `pg-create-dump` overwrites a fixed key, so this copies it once to an
# immutable, date-stamped key in the app storage bucket under `search-eval/`.
#
#   source: gs://<backup-bucket>/backups/mydatabase/klibs_prod_db_latest_version.pgdump.gz
#   dest:   gs://<app-bucket>/search-eval/frozen-<date>.pgdump.gz   (pinned, immutable)
#
# Pure bucket-to-bucket copy — no prod DB access. Run rarely (refresh the corpus),
# by a human with prod cluster access (VPN + kubectl).
#
# Deps: kubectl (klibs-prod secrets), aws CLI.
# ------------------------------------------------------------------------------
set -euo pipefail

# GCS's S3 API returns checksums the aws CLI rejects by default — disable validation (as the app does).
export AWS_RESPONSE_CHECKSUM_VALIDATION="${AWS_RESPONSE_CHECKSUM_VALIDATION:-when_required}"
export AWS_REQUEST_CHECKSUM_CALCULATION="${AWS_REQUEST_CHECKSUM_CALCULATION:-when_required}"

NS="${SEARCH_EVAL_NS:-klibs-prod}"
DATE="$(date +%Y-%m-%d)"
SRC_SECRET=db-backup-admin-secret
DST_SECRET=klibs-readme-user
SRC_KEY=backups/mydatabase/klibs_prod_db_latest_version.pgdump.gz
DST_KEY="search-eval/frozen-${DATE}.pgdump.gz"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

secret() { kubectl get secret "$1" -n "$NS" -o jsonpath="{.data.$2}" | base64 -d; }

echo "### Downloading weekly backup ($SRC_KEY)..."
AWS_ACCESS_KEY_ID="$(secret $SRC_SECRET AWS_ACCESS_KEY_ID)" \
AWS_SECRET_ACCESS_KEY="$(secret $SRC_SECRET AWS_SECRET_ACCESS_KEY)" \
AWS_DEFAULT_REGION=auto \
  aws s3 cp "s3://$(secret $SRC_SECRET BUCKET_NAME)/$SRC_KEY" "$TMP/frozen.pgdump.gz" \
    --endpoint-url "$(secret $SRC_SECRET GCS_ENDPOINT)"

echo "### Uploading frozen corpus ($DST_KEY)..."
DST_BUCKET="$(secret $DST_SECRET BUCKET_NAME)"
AWS_ACCESS_KEY_ID="$(secret $DST_SECRET AWS_ACCESS_KEY_ID)" \
AWS_SECRET_ACCESS_KEY="$(secret $DST_SECRET AWS_SECRET_ACCESS_KEY)" \
AWS_DEFAULT_REGION=auto \
  aws s3 cp "$TMP/frozen.pgdump.gz" "s3://$DST_BUCKET/$DST_KEY" \
    --endpoint-url "$(secret $DST_SECRET GCS_ENDPOINT)"

echo "### Frozen: s3://$DST_BUCKET/$DST_KEY"
echo "### Next: SEARCH_EVAL_SNAPSHOT_KEY=$DST_KEY ./scripts/search-eval-fetch.sh"
