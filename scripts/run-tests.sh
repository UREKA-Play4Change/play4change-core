#!/usr/bin/env bash
# run-tests.sh — run all tests with the correct profile
#
# Usage:
#   ./scripts/run-tests.sh                          # all modules
#   ./scripts/run-tests.sh server                   # server only
#   ./scripts/run-tests.sh ai                       # ai-agent only
#   ./scripts/run-tests.sh server --info            # verbose output
#   ./scripts/run-tests.sh server MagicLinkServiceTest  # specific class

set -euo pipefail
cd "$(dirname "$0")/.."

TARGET="${1:-all}"
shift || true
EXTRA_ARGS=("$@")

run_tests() {
  local task="$1"
  echo "### Running: $task"
  ./gradlew "$task" \
    -Dspring.profiles.active=test \
    --continue \
    "${EXTRA_ARGS[@]}"
}

case "$TARGET" in
  server)
    if [[ "${EXTRA_ARGS[0]:-}" =~ ^[A-Z] ]]; then
      CLASS="${EXTRA_ARGS[0]}"
      EXTRA_ARGS=("${EXTRA_ARGS[@]:1}")
      ./gradlew :server:test --tests "*.$CLASS" \
        -Dspring.profiles.active=test \
        "${EXTRA_ARGS[@]}"
    else
      run_tests ":server:test"
    fi
    ;;
  ai)
    run_tests ":ai-agent:langchain:test"
    ;;
  all)
    run_tests ":server:test :ai-agent:langchain:test"
    ;;
  *)
    echo "Usage: $0 [server|ai|all] [extra gradle args or test class name]"
    echo "  Examples:"
    echo "    $0 server"
    echo "    $0 server MagicLinkServiceTest"
    echo "    $0 ai"
    echo "    $0 all --info"
    exit 1
    ;;
esac

echo ""
echo "=== Test reports ==="
echo "  Server: server/build/reports/tests/test/index.html"
echo "  AI:     ai-agent/langchain/build/reports/tests/test/index.html"
