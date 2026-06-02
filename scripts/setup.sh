#!/usr/bin/env bash
# setup.sh — destroy all containers/volumes, rebuild from scratch, start tunnel
#
# Usage:
#   ./scripts/setup.sh
#
# WARNING: wipes all volumes (postgres, minio, redis). Use restart-server.sh for hot-fix cycles.

set -euo pipefail
cd "$(dirname "$0")/.."

if [ -f .env ]; then
  set -a; source .env; set +a
fi

# echo "### Cleaning containers and volumes..."
# docker compose down -v

echo "### Building and starting all services..."
docker compose up --build -d

echo "### Starting Cloudflare tunnel..."
TUNNEL_ID="${CLOUDFLARE_TUNNEL_ID:-}"
CREDS_FILE="$HOME/.cloudflared/$TUNNEL_ID.json"
TOKEN_FILE="$HOME/.cloudflared/${TUNNEL_ID}.token"

if [ -n "${CLOUDFLARE_TUNNEL_TOKEN:-}" ]; then
  cloudflared tunnel run --protocol http --token "$CLOUDFLARE_TUNNEL_TOKEN"
elif [ -f "$TOKEN_FILE" ]; then
  cloudflared tunnel run --protocol http --token "$(cat "$TOKEN_FILE")"
elif [ -f "$CREDS_FILE" ]; then
  cloudflared tunnel run --protocol http "$TUNNEL_ID"
else
  echo ""
  echo "ERROR: Cloudflare tunnel credentials not found. Provide one of:"
  echo "  1. Set CLOUDFLARE_TUNNEL_TOKEN env var with your tunnel token"
  echo "  2. Save the token to: $TOKEN_FILE"
  echo "     cloudflared tunnel token $TUNNEL_ID > $TOKEN_FILE"
  echo "  3. Place the credentials JSON at: $CREDS_FILE"
  exit 1
fi
