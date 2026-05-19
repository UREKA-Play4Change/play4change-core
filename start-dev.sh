#!/bin/bash

if [ -f .env ]; then
  export $(grep -v '^#' .env | xargs)
fi

echo "### Cleaning containers and volumes..."
docker compose down -v

echo "### Building a new project..."
docker compose up --build -d

echo "### Starting Cloudflare tunnel..."
TUNNEL_ID="${CLOUDFLARE_TUNNEL_ID:-}"
CREDS_FILE="$HOME/.cloudflared/$TUNNEL_ID.json"
TOKEN_FILE="$HOME/.cloudflared/${TUNNEL_ID}.token"

if [ -n "$CLOUDFLARE_TUNNEL_TOKEN" ]; then
  cloudflared tunnel run --protocol http2 --token "$CLOUDFLARE_TUNNEL_TOKEN"
elif [ -f "$TOKEN_FILE" ]; then
  cloudflared tunnel run --protocol http2 --token "$(cat "$TOKEN_FILE")"
elif [ -f "$CREDS_FILE" ]; then
  cloudflared tunnel run --protocol http2 "$TUNNEL_ID"
else
  echo ""
  echo "ERROR: Cloudflare tunnel credentials not found. Provide one of:"
  echo "  1. Set CLOUDFLARE_TUNNEL_TOKEN env var with your tunnel token"
  echo "  2. Save the token to: $TOKEN_FILE"
  echo "     cloudflared tunnel token $TUNNEL_ID > $TOKEN_FILE"
  echo "  3. Place the credentials JSON at: $CREDS_FILE"
  exit 1
fi
