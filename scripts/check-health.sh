#!/usr/bin/env bash
# check-health.sh — wait for all services to be ready and report status
#
# Usage:
#   ./scripts/check-health.sh
#   ./scripts/check-health.sh --timeout 120

set -euo pipefail
cd "$(dirname "$0")/.."

TIMEOUT=90
if [[ "${1:-}" == "--timeout" && -n "${2:-}" ]]; then
  TIMEOUT="$2"
fi

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

wait_for_container() {
  local service="$1"
  local label="$2"
  echo -n "  $label..."
  for i in $(seq 1 "$TIMEOUT"); do
    STATUS=$(docker compose ps --format json "$service" 2>/dev/null | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('Health',''))" 2>/dev/null || echo "")
    if [[ "$STATUS" == "healthy" ]]; then
      echo -e " ${GREEN}healthy${NC}"
      return 0
    fi
    if [[ "$STATUS" == "unhealthy" ]]; then
      echo -e " ${RED}unhealthy${NC}"
      docker compose logs --tail=20 "$service"
      return 1
    fi
    sleep 1
  done
  echo -e " ${RED}timed out after ${TIMEOUT}s${NC}"
  return 1
}

wait_for_http() {
  local url="$1"
  local label="$2"
  echo -n "  $label..."
  for i in $(seq 1 "$TIMEOUT"); do
    if curl -sf "$url" | grep -q '"status":"UP"' 2>/dev/null; then
      echo -e " ${GREEN}UP${NC}"
      return 0
    fi
    sleep 1
  done
  echo -e " ${RED}timed out after ${TIMEOUT}s${NC}"
  return 1
}

echo ""
echo "=== Play4Change Health Check ==="
echo ""
echo "Infrastructure:"
wait_for_container postgres  "PostgreSQL"
wait_for_container redis     "Redis"
wait_for_container minio     "MinIO"

echo ""
echo "Application:"
wait_for_http "http://localhost:8080/actuator/health" "Server (8080)"

echo ""
echo "Observability:"
echo -n "  Prometheus..."
curl -sf "http://localhost:9090/-/healthy" &>/dev/null && echo -e " ${GREEN}UP${NC}" || echo -e " ${YELLOW}not reachable${NC}"
echo -n "  Grafana..."
curl -sf "http://localhost:3000/api/health" &>/dev/null && echo -e " ${GREEN}UP${NC}" || echo -e " ${YELLOW}not reachable${NC}"

echo ""
echo "Endpoints:"
echo "  Swagger UI  → http://localhost:8080/swagger-ui.html"
echo "  Grafana     → http://localhost:3000  (admin / \$GRAFANA_ADMIN_PASSWORD)"
echo "  Prometheus  → http://localhost:9090"
echo "  MinIO UI    → http://localhost:9001"
echo ""
