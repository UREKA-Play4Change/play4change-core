#!/usr/bin/env bash
# promote-admin.sh — promote a user to ADMIN role by email
#
# Usage:
#   ./promote-admin.sh example@email.com

set -euo pipefail

EMAIL="${1:-}"

if [[ -z "$EMAIL" ]]; then
  echo "Usage: $0 <email>"
  echo "  Example: $0 example@email.com"
  exit 1
fi

# Check the user exists before promoting
EXISTING=$(docker compose exec -T postgres \
  psql -U play4change -d play4change -t -A \
  -c "SELECT id, email, role FROM users WHERE email = '${EMAIL}';")

if [[ -z "$EXISTING" ]]; then
  echo "ERROR: No user found with email '${EMAIL}'."
  echo "The user must sign in at least once before being promoted."
  exit 1
fi

CURRENT_ROLE=$(echo "$EXISTING" | cut -d'|' -f3)

if [[ "$CURRENT_ROLE" == "ADMIN" ]]; then
  echo "INFO: '${EMAIL}' is already ADMIN — nothing to do."
  exit 0
fi

# Promote
docker compose exec -T postgres \
  psql -U play4change -d play4change -t -A \
  -c "UPDATE users SET role = 'ADMIN' WHERE email = '${EMAIL}';"

# Confirm
UPDATED=$(docker compose exec -T postgres \
  psql -U play4change -d play4change -t -A \
  -c "SELECT role FROM users WHERE email = '${EMAIL}';")

if [[ "$UPDATED" == "ADMIN" ]]; then
  echo "OK: '${EMAIL}' promoted to ADMIN."
  echo "They must log out and log in again for the new role to take effect."
else
  echo "ERROR: Promotion may have failed. Current role: '${UPDATED}'"
  exit 1
fi
