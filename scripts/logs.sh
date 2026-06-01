#!/usr/bin/env bash
# logs.sh — tail docker compose logs with optional service and grep filter
#
# Usage:
#   ./scripts/logs.sh                        # all services
#   ./scripts/logs.sh server                 # only server
#   ./scripts/logs.sh server ERROR           # server logs containing ERROR
#   ./scripts/logs.sh server "generation"    # AI generation logs
#
# Available services: server, postgres, redis, minio, nginx, prometheus, grafana

set -euo pipefail
cd "$(dirname "$0")/.."

SERVICE="${1:-}"
FILTER="${2:-}"

if [[ -n "$SERVICE" && -n "$FILTER" ]]; then
  docker compose logs -f --tail=100 "$SERVICE" | grep --line-buffered -i "$FILTER"
elif [[ -n "$SERVICE" ]]; then
  docker compose logs -f --tail=100 "$SERVICE"
else
  docker compose logs -f --tail=50
fi
