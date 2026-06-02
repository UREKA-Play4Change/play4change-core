#!/usr/bin/env bash
# promote-admin.sh — promote a user to ADMIN role by email
#
# Usage:
#   ./scripts/promote-admin.sh example@email.com

set -euo pipefail
cd "$(dirname "$0")/.."

EMAIL="${1:-}"

if [[ -z "$EMAIL" ]]; then
  echo "Usage: $0 <email>"
  echo "  Example: $0 example@email.com"
  exit 1
fi

psql() {
  docker compose exec -T postgres \
    psql -U play4change -d play4change -t -A "$@"
}

# Check the user exists before promoting
EXISTING=$(psql -v email="$EMAIL" -c "SELECT id, email, role FROM users WHERE email = :'email';")

if [[ -z "$EXISTING" ]]; then
  echo "ERROR: No user found with email '$EMAIL'."
  echo "The user must sign in at least once before being promoted."
  exit 1
fi

CURRENT_ROLE=$(echo "$EXISTING" | cut -d'|' -f3)

if [[ "$CURRENT_ROLE" == "ADMIN" ]]; then
  echo "INFO: '$EMAIL' is already ADMIN — nothing to do."
  exit 0
fi

# Promote
psql -v email="$EMAIL" -c "UPDATE users SET role = 'ADMIN' WHERE email = :'email';"

# Confirm
UPDATED=$(psql -v email="$EMAIL" -c "SELECT role FROM users WHERE email = :'email';")

if [[ "$UPDATED" == "ADMIN" ]]; then
  echo "OK: '$EMAIL' promoted to ADMIN."
  echo "They must log out and log in again for the new role to take effect."
else
  echo "ERROR: Promotion may have failed. Current role: '$UPDATED'"
  exit 1
fi
