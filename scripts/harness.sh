#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "$SCRIPT_DIR/lib/common.sh"

BUILD_TYPE="debug"
SERIAL=""
SKIP_BUILD=0
SETTLE_MS=1200

usage() {
  cat <<EOF
Usage: $0 [options]

Deploy the cluster app and write a source-based harness report.
Does **not** take screencaps — captures are too inaccurate for agent verify.

Options:
  --debug | --release   Build type (default: debug)
  --serial <id>         Target adb device serial
  --skip-build          Reuse existing APK
  --settle-ms <n>       Wait after launch before smoke (default: 1200)
  -h, --help            Show this help

Outputs:
  docs/harness/latest-report.md
  docs/harness/design-facts.md
  docs/harness/design-facts.json

Legacy PNG grabs (humans only, not for agent verify):
  ./scripts/screengrab.sh
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --debug) BUILD_TYPE="debug"; shift ;;
    --release) BUILD_TYPE="release"; shift ;;
    --serial) SERIAL="${2:-}"; shift 2 ;;
    --skip-build) SKIP_BUILD=1; shift ;;
    --settle-ms) SETTLE_MS="${2:-}"; shift 2 ;;
    --screengrab|--grab)
      die "Captures removed from harness (too inaccurate). Use ./scripts/screengrab.sh only if a human needs a PNG archive — agents must not verify from it."
      ;;
    -h|--help) usage; exit 0 ;;
    *) die "Unknown option: $1" ;;
  esac
done

cd "$ROOT_DIR"
TARGET="$(resolve_device "$SERIAL")"
ensure_harness_dir

deploy_args=(--"$BUILD_TYPE" --serial "$TARGET")
[[ "$SKIP_BUILD" -eq 1 ]] && deploy_args+=(--skip-build)

echo "======== HARNESS: deploy ========"
"$ROOT_DIR/scripts/deploy.sh" "${deploy_args[@]}"

echo
echo "======== HARNESS: settle ${SETTLE_MS}ms ========"
python3 - "$SETTLE_MS" <<'PY'
import sys, time
time.sleep(int(sys.argv[1]) / 1000.0)
PY

echo
echo "======== HARNESS: design probe (source) ========"
"$ROOT_DIR/scripts/probe-design.sh"

echo
echo "======== HARNESS: smoke ========"
FOCUS="$(current_focus "$TARGET" || true)"
echo "focus: ${FOCUS:-unknown}"
FOCUS_RESULT="warn"
if app_is_focused "$TARGET"; then
  FOCUS_RESULT="pass"
else
  echo "WARN: $PACKAGE not focused after settle." >&2
fi

WM_SIZE="$(device_wm_size "$TARGET" || true)"
WM_DENSITY="$(device_wm_density "$TARGET" || true)"
echo "wm size: ${WM_SIZE:-unknown}"
echo "wm density: ${WM_DENSITY:-unknown}"

ORIENT_HINT="unknown"
if [[ "$WM_SIZE" =~ ([0-9]+)x([0-9]+) ]]; then
  w="${BASH_REMATCH[1]}"
  h="${BASH_REMATCH[2]}"
  # Physical size is fixed; landscape app still reports the same wm size.
  # Prefer wider dimension as landscape expectation for phones in landscape.
  if (( w >= h )); then
    ORIENT_HINT="landscape-capable (${w}x${h})"
  else
    ORIENT_HINT="portrait-native (${w}x${h}) — rotate device if cluster looks wrong"
  fi
fi

stamp="$(date +%Y%m%d-%H%M%S)"
REPORT_PATH="${HARNESS_DIR}/latest-report.md"

refs=(
  "docs/uv-x47-reference.png"
  "docs/screen-reference.jpg"
  "docs/screen-reference-2.jpg"
  "docs/screen-reference-3.jpg"
)

{
  cat <<EOF
# Harness report — ${stamp}

| Field | Value |
|-------|-------|
| Serial | \`${TARGET}\` |
| Package | \`${PACKAGE}\` |
| Build | \`${BUILD_TYPE}\` |
| Focus | \`${FOCUS:-n/a}\` |
| WM size | \`${WM_SIZE:-n/a}\` |
| WM density | \`${WM_DENSITY:-n/a}\` |
| Orientation hint | ${ORIENT_HINT} |
| Design facts | \`docs/harness/design-facts.md\` |
| Generated | $(date -u +%Y-%m-%dT%H:%M:%SZ) |

## Verify policy

**Do not** use \`docs/captures/\` or adb screencap for pass/fail.
Colors, type sizes, and spacing come from Compose source (\`design-facts.md\`).
References are for design intent; the physical device (or user feedback) is visual truth.

## Agent verify steps

1. Read \`docs/harness/design-facts.md\` (tokens for the region you changed)
2. Read Compose sources for that region
3. Read a reference only for intent (prefer \`docs/screen-reference-2.jpg\` for COMBAT)
4. Walk \`.cursor/skills/cluster-harness/checklist.md\` against **source + references**, not PNGs
5. If something still looks wrong on-device, adjust code → re-run \`./scripts/harness.sh\`

## Smoke checks

| Check | Result |
|-------|--------|
| APK installed / launched | pass |
| App focus contains package | ${FOCUS_RESULT} |
| Design facts written | pass |

## References

EOF
  for ref in "${refs[@]}"; do
    if [[ -f "$ROOT_DIR/$ref" ]]; then
      echo "- \`$ref\`"
    fi
  done
} > "$REPORT_PATH"

echo
echo "======== HARNESS: done ========"
echo "REPORT_OK path=$REPORT_PATH"
echo "FACTS_OK path=${HARNESS_DIR}/design-facts.md"
echo
echo "Next for agent: Read docs/harness/design-facts.md + Compose sources + docs/screen-reference-2.jpg (intent). Do not verify from captures."
