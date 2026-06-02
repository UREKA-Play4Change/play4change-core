#!/usr/bin/env bash
# minio-init.sh — create required MinIO buckets
#
# Requires: mc (MinIO client) — brew install minio/stable/mc
#
# Usage:
#   ./scripts/minio-init.sh

set -euo pipefail
cd "$(dirname "$0")/.."

if [ -f .env ]; then
  set -a; source .env; set +a
fi

MINIO_USER="${MINIO_ROOT_USER:-minioadmin}"
MINIO_PASS="${MINIO_ROOT_PASSWORD:-minioadmin}"
MINIO_URL="http://localhost:9000"
ALIAS="play4change-local"
BUCKET="${MINIO_BUCKET:-play4change}"

if ! command -v mc &>/dev/null; then
  echo "ERROR: MinIO Client (mc) is not installed."
  echo "Install on Mac: brew install minio/stable/mc"
  echo "Install on Linux: https://min.io/docs/minio/linux/reference/minio-mc.html"
  exit 1
fi

echo "### Configuring mc alias '$ALIAS' → $MINIO_URL"
mc alias set "$ALIAS" "$MINIO_URL" "$MINIO_USER" "$MINIO_PASS" --quiet

echo "### Creating bucket: $BUCKET"
mc mb --ignore-existing "$ALIAS/$BUCKET"

echo "### Current buckets:"
mc ls "$ALIAS"

echo ""
echo "OK: MinIO is ready. Bucket '$BUCKET' exists."
echo "    MinIO console → http://localhost:9001 ($MINIO_USER / $MINIO_PASS)"
