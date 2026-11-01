#!/usr/bin/env bash
# Extract design tokens from Compose sources into docs/harness/.
# Agents verify against this + references — not against adb screencaps.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "$SCRIPT_DIR/lib/common.sh"

ensure_harness_dir

FACTS_JSON="${HARNESS_DIR}/design-facts.json"
FACTS_MD="${HARNESS_DIR}/design-facts.md"

python3 - "$ROOT_DIR" "$FACTS_JSON" "$FACTS_MD" <<'PY'
import json
import re
import sys
from pathlib import Path

root = Path(sys.argv[1])
out_json = Path(sys.argv[2])
out_md = Path(sys.argv[3])

files = {
    "colors": root / "app/src/main/java/com/ultraviolette/navigator/ui/theme/Color.kt",
    "screen": root / "app/src/main/java/com/ultraviolette/navigator/ui/SpeedometerScreen.kt",
    "side": root / "app/src/main/java/com/ultraviolette/navigator/ui/components/SideMetrics.kt",
    "bottom": root / "app/src/main/java/com/ultraviolette/navigator/ui/components/BottomModeBar.kt",
    "center": root / "app/src/main/java/com/ultraviolette/navigator/ui/components/CenterSpeedCluster.kt",
    "placeholders": root / "app/src/main/java/com/ultraviolette/navigator/model/ClusterPlaceholders.kt",
}

color_re = re.compile(
    r"val\s+(Cluster\w+)\s*=\s*Color\(0x([0-9A-Fa-f]{8})\)"
)
# private/public vals like: private val SideRailWidth = 180.dp / SideValueSize = 48.sp
const_re = re.compile(
    r"(?:private\s+|internal\s+)?val\s+(\w+)\s*=\s*([0-9]+(?:\.[0-9]+)?)\s*\.(dp|sp)"
)
# data class defaults: val avgSpeedKmh: String = "———"
placeholder_re = re.compile(
    r"val\s+(\w+)\s*:\s*[\w.<>,\s?]+\s*=\s*(\"[^\"]*\"|true|false|[0-9]+)"
)


def rel(p: Path) -> str:
    return str(p.relative_to(root))


facts = {
    "source_of_truth": "Compose source — not screencap pixels",
    "colors": {},
    "layout": {},
    "placeholders": {},
    "files": {k: rel(v) for k, v in files.items() if v.exists()},
}

for key in ("colors",):
    path = files[key]
    text = path.read_text(encoding="utf-8")
    for name, hex8 in color_re.findall(text):
        facts["colors"][name] = f"#{hex8[2:].upper()}"  # drop alpha for display hex RGB
        facts["colors"][f"{name}_argb"] = f"#{hex8.upper()}"

for key in ("screen", "side", "bottom", "center"):
    path = files[key]
    if not path.exists():
        continue
    text = path.read_text(encoding="utf-8")
    bucket = {}
    for name, num, unit in const_re.findall(text):
        bucket[name] = f"{num}.{unit}"
    if bucket:
        facts["layout"][key] = bucket

ph_path = files["placeholders"]
if ph_path.exists():
    text = ph_path.read_text(encoding="utf-8")
    for name, raw in placeholder_re.findall(text):
        if raw in ("true", "false"):
            facts["placeholders"][name] = raw == "true"
        elif raw.startswith('"'):
            facts["placeholders"][name] = raw[1:-1]
        else:
            facts["placeholders"][name] = int(raw)

out_json.write_text(json.dumps(facts, indent=2) + "\n", encoding="utf-8")

lines = [
    "# Design facts (from source)",
    "",
    "Source of truth for harness verify. **Do not** judge colors or spacing from screencaps.",
    "",
    "## Colors (`Color.kt`)",
    "",
    "| Token | RGB |",
    "|-------|-----|",
]
for name, rgb in sorted((k, v) for k, v in facts["colors"].items() if not k.endswith("_argb")):
    lines.append(f"| `{name}` | `{rgb}` |")

lines += ["", "## Layout constants", ""]
for section, bucket in facts["layout"].items():
    lines.append(f"### `{section}`")
    lines.append("")
    lines.append("| Name | Value |")
    lines.append("|------|-------|")
    for name, val in sorted(bucket.items()):
        lines.append(f"| `{name}` | `{val}` |")
    lines.append("")

lines += ["## Placeholders", "", "| Field | Default |", "|-------|---------|"]
for name, val in sorted(facts["placeholders"].items()):
    shown = json.dumps(val) if not isinstance(val, str) else f"`{val}`"
    if isinstance(val, str):
        shown = f"`{val}`"
    else:
        shown = f"`{val}`"
    lines.append(f"| `{name}` | {shown} |")

lines += [
    "",
    "## Agent verify",
    "",
    "1. Compare these tokens / layout constants to the region you changed.",
    "2. Read reference images only for **design intent** (`docs/screen-reference-2.jpg`).",
    "3. Deploy with `./scripts/harness.sh` and treat the **device** (or user feedback) as visual truth.",
    "4. Never pass/fail the checklist from `docs/captures/*.png`.",
    "",
]
out_md.write_text("\n".join(lines), encoding="utf-8")
print(f"FACTS_JSON path={out_json}")
print(f"FACTS_MD path={out_md}")
print(f"COLORS {len([k for k in facts['colors'] if not k.endswith('_argb')])}")
print(f"LAYOUT_SECTIONS {len(facts['layout'])}")
print(f"PLACEHOLDERS {len(facts['placeholders'])}")
PY

echo "PROBE_OK json=$FACTS_JSON md=$FACTS_MD"
