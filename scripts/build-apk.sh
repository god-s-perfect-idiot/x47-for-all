#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

BUILD_TYPE="${1:-debug}"

case "$BUILD_TYPE" in
  debug)
    TASK=":app:assembleDebug"
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    ;;
  release)
    TASK=":app:assembleRelease"
    APK_PATH="app/build/outputs/apk/release/app-release-unsigned.apk"
    ;;
  *)
    echo "Usage: $0 [debug|release]"
    exit 1
    ;;
esac

echo "Building $BUILD_TYPE APK..."
./gradlew "$TASK"

if [[ ! -f "$APK_PATH" ]]; then
  echo "Build finished but APK not found at: $APK_PATH" >&2
  exit 1
fi

ABS_APK="$(cd "$(dirname "$APK_PATH")" && pwd)/$(basename "$APK_PATH")"
SIZE="$(du -h "$ABS_APK" | awk '{print $1}')"

echo
echo "APK ready ($SIZE)"
echo "$ABS_APK"
