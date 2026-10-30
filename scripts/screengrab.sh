#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "$SCRIPT_DIR/lib/common.sh"

SERIAL=""
NAME=""
SETTLE_MS=1800
OUT_PATH=""

usage() {
  cat <<EOF
Usage: $0 [options]

OPTIONAL human archive only. Agents must NOT verify UI from these PNGs —
screencaps are inaccurate for color/spacing judgment. Use ./scripts/harness.sh
+ docs/harness/design-facts.md instead.

Options:
  --serial <id>         Target adb device serial
  --name <label>        Capture basename label (default: timestamp)
  --settle-ms <n>       Wait before capture (default: 1800)
  --out <path>          Explicit output path (skips docs/captures naming)
  -h, --help            Show this help
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --serial) SERIAL="${2:-}"; shift 2 ;;
    --name) NAME="${2:-}"; shift 2 ;;
    --settle-ms) SETTLE_MS="${2:-}"; shift 2 ;;
    --out) OUT_PATH="${2:-}"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) die "Unknown option: $1" ;;
  esac
done

cd "$ROOT_DIR"
TARGET="$(resolve_device "$SERIAL")"
ensure_capture_dir

if [[ -z "$OUT_PATH" ]]; then
  stamp="$(date +%Y%m%d-%H%M%S)"
  label="${NAME:-capture}"
  OUT_PATH="${CAPTURE_DIR}/${stamp}-${label}.png"
fi

mkdir -p "$(dirname "$OUT_PATH")"

echo "==> Settling ${SETTLE_MS}ms on $TARGET"
python3 - "$SETTLE_MS" <<'PY'
import sys, time
time.sleep(int(sys.argv[1]) / 1000.0)
PY

echo "==> Screencap -> $OUT_PATH"
adb -s "$TARGET" exec-out screencap -p > "$OUT_PATH"

bytes="$(wc -c < "$OUT_PATH" | tr -d ' ')"
if [[ "$bytes" -lt 1000 ]]; then
  die "Screencap too small (${bytes} bytes) — device may be locked or capture failed."
fi

# Stable pointer for agents / CI.
cp "$OUT_PATH" "${CAPTURE_DIR}/latest.png"

# Also keep a convenience copy next to historical phone-capture-*.png
latest_num=0
for f in "$ROOT_DIR"/docs/phone-capture-*.png; do
  [[ -e "$f" ]] || continue
  base="$(basename "$f")"
  n="${base#phone-capture-}"
  n="${n%.png}"
  if [[ "$n" =~ ^[0-9]+$ ]] && (( n > latest_num )); then
    latest_num=$n
  fi
done
next=$((latest_num + 1))
PHONE_COPY="${ROOT_DIR}/docs/phone-capture-${next}.png"
cp "$OUT_PATH" "$PHONE_COPY"

echo
echo "CAPTURE_OK path=$OUT_PATH"
echo "LATEST_OK path=${CAPTURE_DIR}/latest.png"
echo "PHONE_COPY path=$PHONE_COPY bytes=$bytes"
