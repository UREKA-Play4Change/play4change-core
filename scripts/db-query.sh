#!/usr/bin/env bash
# db-query.sh — run a single SQL statement against the postgres container
#
# Usage:
#   ./scripts/db-query.sh "SELECT id, email, role FROM users;"
#   ./scripts/db-query.sh "SELECT count(*) FROM topics WHERE status = 'ACTIVE';"

set -euo pipefail
cd "$(dirname "$0")/.."

SQL="${1:-}"
if [[ -z "$SQL" ]]; then
  echo "Usage: $0 '<sql>'"
  echo "  Example: $0 \"SELECT id, email, role FROM users;\""
  exit 1
fi

docker compose exec -T postgres \
  psql -U play4change -d play4change -c "$SQL"
