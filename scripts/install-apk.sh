#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

BUILD_TYPE="${1:-debug}"
SERIAL="${2:-}"

case "$BUILD_TYPE" in
  debug)
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    ;;
  release)
    APK_PATH="app/build/outputs/apk/release/app-release-unsigned.apk"
    ;;
  *)
    echo "Usage: $0 [debug|release] [device_serial]"
    exit 1
    ;;
esac

if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found. Install Android platform-tools and ensure adb is on PATH." >&2
  exit 1
fi

if [[ ! -f "$APK_PATH" ]]; then
  echo "APK not found at $APK_PATH"
  echo "Building $BUILD_TYPE APK first..."
  "$ROOT_DIR/scripts/build-apk.sh" "$BUILD_TYPE"
fi

DEVICES=()
while IFS= read -r line; do
  [[ -n "$line" ]] && DEVICES+=("$line")
done < <(adb devices | awk 'NR>1 && $2=="device" {print $1}')

if [[ ${#DEVICES[@]} -eq 0 ]]; then
  echo "No adb devices found (authorized + online)." >&2
  echo "Connect a device/emulator, enable USB debugging, then retry." >&2
  adb devices -l >&2 || true
  exit 1
fi

if [[ -n "$SERIAL" ]]; then
  TARGET="$SERIAL"
elif [[ ${#DEVICES[@]} -eq 1 ]]; then
  TARGET="${DEVICES[0]}"
else
  echo "Multiple devices connected. Pass a serial as the second argument:" >&2
  printf '  %s\n' "${DEVICES[@]}" >&2
  echo "Usage: $0 $BUILD_TYPE <device_serial>" >&2
  exit 1
fi

echo "Installing $APK_PATH onto ${TARGET}..."
adb -s "$TARGET" install -r "$APK_PATH"

echo
echo "Installed on ${TARGET}"
echo "Launch with:"
echo "  adb -s ${TARGET} shell am start -n com.ultraviolette.navigator/.MainActivity"
