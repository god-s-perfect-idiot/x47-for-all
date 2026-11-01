#!/usr/bin/env bash
# Helpers for /publish-release. Prefer the skill for drafting notes + gh create.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

VERSION=""
BUILD_ONLY=0
NOTES_ONLY=0
SKIP_BUILD=0

usage() {
  cat <<EOF
Usage: $0 --version X.Y.Z [options]

Options:
  --version X.Y.Z   Semver without leading v (required)
  --build-only      Assemble debug APK and copy to dist/
  --notes-only      Print git log since previous tag (for drafting notes)
  --skip-build      Reuse existing debug APK when copying to dist/
  -h, --help        Show help
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --version) VERSION="${2:-}"; shift 2 ;;
    --build-only) BUILD_ONLY=1; shift ;;
    --notes-only) NOTES_ONLY=1; shift ;;
    --skip-build) SKIP_BUILD=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage >&2; exit 1 ;;
  esac
done

if [[ -z "$VERSION" ]]; then
  echo "--version is required" >&2
  usage >&2
  exit 1
fi

if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Version must look like X.Y.Z (got: $VERSION)" >&2
  exit 1
fi

prev_tag() {
  git describe --tags --abbrev=0 2>/dev/null || true
}

if [[ "$NOTES_ONLY" -eq 1 ]]; then
  PREV="$(prev_tag)"
  echo "VERSION=$VERSION"
  echo "PREV_TAG=${PREV:-"(none)"}"
  echo "----"
  if [[ -n "$PREV" ]]; then
    git log --pretty=format:'- %s (%h)' "${PREV}..HEAD"
  else
    git log --pretty=format:'- %s (%h)'
  fi
  echo
  exit 0
fi

DIST_DIR="$ROOT_DIR/dist"
ASSET="$DIST_DIR/ultraviolette-navigator-${VERSION}.apk"
DEBUG_APK="$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"

mkdir -p "$DIST_DIR"

if [[ "$SKIP_BUILD" -eq 0 ]]; then
  "$ROOT_DIR/scripts/build-apk.sh" debug
fi

if [[ ! -f "$DEBUG_APK" ]]; then
  echo "Missing debug APK at $DEBUG_APK" >&2
  exit 1
fi

cp "$DEBUG_APK" "$ASSET"
SIZE="$(du -h "$ASSET" | awk '{print $1}')"
echo
echo "RELEASE_APK_OK ($SIZE)"
echo "$ASSET"

if [[ "$BUILD_ONLY" -eq 1 ]]; then
  exit 0
fi

echo
echo "Next (via /publish-release skill):"
echo "  1. Bump versionCode/versionName in app/build.gradle.kts"
echo "  2. Write docs/releases/v${VERSION}.md"
echo "  3. Commit + push"
echo "  4. gh release create v${VERSION} \"$ASSET\" --title \"v${VERSION}\" --notes-file docs/releases/v${VERSION}.md"
