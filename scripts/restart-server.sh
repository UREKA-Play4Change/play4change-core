#!/usr/bin/env bash
# restart-server.sh — rebuild the server JAR and restart only the server container
#
# Use this instead of start-dev.sh when iterating on server code.
# Does NOT wipe volumes — postgres, minio, and redis data are preserved.
#
# Usage:
#   ./scripts/restart-server.sh
#   ./scripts/restart-server.sh --skip-tests   (default: tests are skipped anyway)

set -euo pipefail
cd "$(dirname "$0")/.."

echo "### Building server JAR..."
./gradlew :server:bootJar -x test -x detekt --parallel

echo "### Stopping server container..."
docker compose stop server
docker compose rm -f server

echo "### Starting server container..."
docker compose up -d --build server

echo "### Tailing server logs (Ctrl+C to stop)..."
docker compose logs -f server
