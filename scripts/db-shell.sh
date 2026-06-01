#!/usr/bin/env bash
# db-shell.sh — open an interactive psql session inside the postgres container
#
# Usage:
#   ./scripts/db-shell.sh

set -euo pipefail
cd "$(dirname "$0")/.."

exec docker compose exec postgres psql -U play4change -d play4change
