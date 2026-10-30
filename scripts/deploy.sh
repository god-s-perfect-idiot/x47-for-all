#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "$SCRIPT_DIR/lib/common.sh"

BUILD_TYPE="debug"
SERIAL=""
SKIP_BUILD=0
NO_LAUNCH=0

usage() {
  cat <<EOF
Usage: $0 [options]

Build (optional), install, and launch the UV Navigator cluster on a device.

Options:
  --debug | --release   Build type (default: debug)
  --serial <id>         Target adb device serial
  --skip-build          Install existing APK only
  --no-launch           Install without launching
  -h, --help            Show this help
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --debug) BUILD_TYPE="debug"; shift ;;
    --release) BUILD_TYPE="release"; shift ;;
    --serial) SERIAL="${2:-}"; shift 2 ;;
    --skip-build) SKIP_BUILD=1; shift ;;
    --no-launch) NO_LAUNCH=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) die "Unknown option: $1" ;;
  esac
done

cd "$ROOT_DIR"
TARGET="$(resolve_device "$SERIAL")"
APK_PATH="$(apk_path_for "$BUILD_TYPE")"

if [[ "$SKIP_BUILD" -eq 0 ]]; then
  echo "==> Building $BUILD_TYPE"
  "$ROOT_DIR/scripts/build-apk.sh" "$BUILD_TYPE"
elif [[ ! -f "$APK_PATH" ]]; then
  die "APK missing at $APK_PATH. Re-run without --skip-build."
fi

echo "==> Installing on $TARGET"
adb -s "$TARGET" install -r "$APK_PATH"

if [[ "$NO_LAUNCH" -eq 0 ]]; then
  echo "==> Launching $ACTIVITY"
  if launch_app "$TARGET"; then
    echo "FOCUS_OK $(current_focus "$TARGET")"
  else
    echo "FOCUS_WARN $(current_focus "$TARGET")" >&2
    echo "App may not be focused — check the device." >&2
  fi
fi

echo
echo "DEPLOY_OK serial=$TARGET apk=$APK_PATH"
