#!/usr/bin/env bash
# setup.sh — rebuild from scratch and start all services including the Cloudflare tunnel.
#
# The tunnel runs as a Docker Compose service (cloudflare-tunnel) so it stays up
# automatically — no separate terminal process needed.
# Prerequisite: ~/.cloudflared/<CLOUDFLARE_TUNNEL_ID>.json must exist on this machine.
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

#echo "### Cleaning containers and volumes..."
#docker compose down -v

echo "### Building and starting all services (including Cloudflare tunnel)..."
docker compose up --build -d
