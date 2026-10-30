#!/usr/bin/env bash
# Shared helpers for deploy / screengrab / harness scripts.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PACKAGE="com.ultraviolette.navigator"
ACTIVITY="${PACKAGE}/.MainActivity"
HARNESS_DIR="${ROOT_DIR}/docs/harness"
CAPTURE_DIR="${ROOT_DIR}/docs/captures"

die() {
  echo "$*" >&2
  exit 1
}

require_adb() {
  command -v adb >/dev/null 2>&1 || die "adb not found. Install Android platform-tools and ensure adb is on PATH."
}

list_devices() {
  adb devices | awk 'NR>1 && $2=="device" {print $1}'
}

resolve_device() {
  local serial="${1:-}"
  local devices=()
  local line

  require_adb

  while IFS= read -r line; do
    [[ -n "$line" ]] && devices+=("$line")
  done < <(list_devices)

  if [[ ${#devices[@]} -eq 0 ]]; then
    echo "No adb devices found (authorized + online)." >&2
    adb devices -l >&2 || true
    exit 1
  fi

  if [[ -n "$serial" ]]; then
    printf '%s\n' "${devices[@]}" | grep -qx "$serial" \
      || die "Device serial '$serial' is not online. Online: ${devices[*]}"
    echo "$serial"
    return
  fi

  if [[ ${#devices[@]} -eq 1 ]]; then
    echo "${devices[0]}"
    return
  fi

  die "Multiple devices connected (${devices[*]}). Pass --serial <id>."
}

apk_path_for() {
  case "${1:-debug}" in
    debug) echo "app/build/outputs/apk/debug/app-debug.apk" ;;
    release) echo "app/build/outputs/apk/release/app-release-unsigned.apk" ;;
    *) die "Unknown build type: $1 (use debug|release)" ;;
  esac
}

ensure_harness_dir() {
  mkdir -p "$HARNESS_DIR"
}

ensure_capture_dir() {
  mkdir -p "$CAPTURE_DIR"
}

# Display / orientation smoke helpers (no screencap).
device_wm_size() {
  local serial="$1"
  adb -s "$serial" shell wm size 2>/dev/null | tr -d '\r' | tail -1
}

device_wm_density() {
  local serial="$1"
  adb -s "$serial" shell wm density 2>/dev/null | tr -d '\r' | tail -1
}

current_focus() {
  local serial="$1"
  adb -s "$serial" shell dumpsys window 2>/dev/null \
    | awk '/mCurrentFocus|mFocusedApp/ {print; exit}'
}

app_is_focused() {
  local serial="$1"
  current_focus "$serial" | grep -q "$PACKAGE"
}

# Launch and poll until our activity is focused (or timeout).
launch_app() {
  local serial="$1"
  local attempts="${2:-8}"
  local i

  adb -s "$serial" shell am force-stop "$PACKAGE" >/dev/null 2>&1 || true
  sleep 0.4
  adb -s "$serial" shell am start -W -n "$ACTIVITY" >/dev/null
  # Wake / dismiss keyguard noise on some OEM skins.
  adb -s "$serial" shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true

  for ((i = 1; i <= attempts; i++)); do
    if app_is_focused "$serial"; then
      return 0
    fi
    # Retry start if a launcher stole focus.
    if (( i == 3 || i == 6 )); then
      adb -s "$serial" shell am start -n "$ACTIVITY" >/dev/null 2>&1 || true
    fi
    sleep 0.35
  done
  return 1
}
