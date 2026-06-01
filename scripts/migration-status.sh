#!/usr/bin/env bash
# migration-status.sh — show Flyway migration history from the running postgres container
#
# Usage:
#   ./scripts/migration-status.sh

set -euo pipefail
cd "$(dirname "$0")/.."

echo ""
echo "=== Flyway Migration History ==="
echo ""
docker compose exec -T postgres \
  psql -U play4change -d play4change \
  -c "SELECT installed_rank AS \"#\",
             version,
             description,
             CASE success WHEN true THEN 'OK' ELSE 'FAILED' END AS status,
             installed_on::timestamp(0) AS installed_on,
             execution_time AS ms
      FROM flyway_schema_history
      ORDER BY installed_rank;"
