#!/usr/bin/env bash
# backfill-user-names.sh — set name for every user whose name is NULL
#
# Derives the display name from the email local part:
#   - takes the part before '@'
#   - replaces '.' and '_' with spaces
#   - applies initcap (capitalises first letter of each word)
#
# Examples:
#   radesh.govind@gmail.com  →  Radesh Govind
#   alice@example.com        →  Alice
#   john_doe@test.com        →  John Doe
#
# Usage:
#   ./scripts/backfill-user-names.sh          # dry-run: shows what would change
#   ./scripts/backfill-user-names.sh --apply  # applies the update

set -euo pipefail
cd "$(dirname "$0")/.."

APPLY="${1:-}"

psql() {
  docker compose exec -T postgres \
    psql -U play4change -d play4change -t -A "$@"
}

COUNT=$(psql -c "SELECT count(*) FROM users WHERE name IS NULL;")

if [[ "$COUNT" == "0" ]]; then
  echo "OK: No users with a missing name. Nothing to do."
  exit 0
fi

echo "Found $COUNT user(s) with no name set:"
echo ""
psql -c "SELECT email FROM users WHERE name IS NULL ORDER BY created_at;" \
  | while IFS= read -r email; do
      LOCAL="${email%%@*}"
      DERIVED=$(echo "$LOCAL" | tr '._' '  ' | awk '{for(i=1;i<=NF;i++) $i=toupper(substr($i,1,1)) tolower(substr($i,2)); print}')
      printf "  %-40s  →  %s\n" "$email" "$DERIVED"
    done

echo ""

if [[ "$APPLY" != "--apply" ]]; then
  echo "Dry-run complete. Pass --apply to apply the changes."
  exit 0
fi

psql -c "
UPDATE users
SET name = initcap(replace(replace(split_part(email, '@', 1), '.', ' '), '_', ' '))
WHERE name IS NULL;
"

REMAINING=$(psql -c "SELECT count(*) FROM users WHERE name IS NULL;")

if [[ "$REMAINING" == "0" ]]; then
  echo "OK: All $COUNT user(s) updated successfully."
else
  echo "ERROR: $REMAINING user(s) still have no name after update."
  exit 1
fi
