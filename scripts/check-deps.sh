#!/usr/bin/env bash
# check-deps.sh — run OWASP dependency-check and open the report
#
# This is slow (2-5 min on first run, downloads NVD data).
# CI runs this separately via .github/workflows/dependency-check.yml.
#
# Usage:
#   ./scripts/check-deps.sh

set -euo pipefail
cd "$(dirname "$0")/.."

echo "### Running OWASP dependency-check (this will take a few minutes)..."
./gradlew :server:dependencyCheckAnalyze

REPORT="server/build/reports/dependency-check-report.html"

if [[ -f "$REPORT" ]]; then
  echo ""
  echo "Report: $REPORT"
  if command -v open &>/dev/null; then
    open "$REPORT"
  elif command -v xdg-open &>/dev/null; then
    xdg-open "$REPORT"
  fi
else
  echo "ERROR: Report not found at $REPORT"
  exit 1
fi
