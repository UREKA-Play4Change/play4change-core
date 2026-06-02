#!/usr/bin/env bash
# build-android.sh — build the Android APK
#
# Usage:
#   ./scripts/build-android.sh           # debug (default)
#   ./scripts/build-android.sh release   # release

set -euo pipefail
cd "$(dirname "$0")/.."

VARIANT="${1:-debug}"

case "$VARIANT" in
  debug)
    echo "### Building debug APK..."
    ./gradlew :composeApp:assembleDebug --parallel
    APK=$(find composeApp/build/outputs/apk/debug -name "*.apk" | head -1)
    ;;
  release)
    echo "### Building release APK..."
    ./gradlew :composeApp:assembleRelease --parallel
    APK=$(find composeApp/build/outputs/apk/release -name "*.apk" | head -1)
    ;;
  *)
    echo "Usage: $0 [debug|release]"
    exit 1
    ;;
esac

if [[ -n "$APK" ]]; then
  SIZE=$(du -sh "$APK" | cut -f1)
  echo ""
  echo "OK: $APK ($SIZE)"
  echo ""
  echo "Install on connected device:"
  echo "  adb install -r \"$APK\""
else
  echo "ERROR: APK not found. Check build output above."
  exit 1
fi
