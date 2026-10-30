---
name: cluster-harness
description: >-
  Deploy and smoke-test the Ultraviolette Navigator Android cluster UI on a
  connected device. Verify from Compose source + design facts + reference
  intent — not from screencaps. Use when changing Compose UI, after
  layout/theme/font edits, when the user asks to deploy, verify, or run the
  harness.
---

# Cluster harness

## When to run

After any change under `app/src/main/java/**/ui/**`, themes, fonts, or resources that affect the cluster screen — or when the user asks to deploy / verify.

## Commands

```bash
# Deploy + settle + smoke + design-facts report (no screencap)
./scripts/harness.sh

# Reuse APK
./scripts/harness.sh --skip-build
```

Useful flags: `--skip-build`, `--serial <id>`, `--settle-ms 1200`.

Pieces:

| Script | Job |
|--------|-----|
| `./scripts/deploy.sh` | build → install → launch |
| `./scripts/probe-design.sh` | scrape Color.kt / layout consts / placeholders → `docs/harness/` |
| `./scripts/harness.sh` | deploy + settle + smoke + probe + `docs/harness/latest-report.md` |
| `./scripts/screengrab.sh` | **Human archive only** — never for agent pass/fail |

Shell needs `required_permissions: ["all"]` (adb USB + device I/O).

## Capture policy (hard)

- **Do not** take or read screencaps to verify UI.
- **Do not** run `screengrab.sh` / `harness.sh --screengrab` in the agent loop.
- Captures mislead on color, bezel, and spacing. Source tokens + the physical device (or user feedback) are truth.
- If the user wants a PNG for themselves, they can run `./scripts/screengrab.sh` — still do not score the checklist from it.

## Agent loop

1. Edit Compose / theme as needed.
2. Run `./scripts/harness.sh`.
3. Read `docs/harness/latest-report.md` and `docs/harness/design-facts.md`.
4. Read the Compose files for the region you changed.
5. Read `docs/screen-reference-2.jpg` (COMBAT) and/or `docs/uv-x47-reference.png` for **design intent only**.
6. Score [checklist.md](checklist.md) against **source + references**, not PNGs.
7. If the user reports on-device issues, patch → re-deploy with harness. Cap at 3 deploy iterations unless they ask to continue.

## Hard rules

- Never claim a visual match from a capture image.
- Prefer the harness over ad-hoc `gradlew` / `adb` one-liners so smoke + design facts land in `docs/harness/`.
- Package / activity: `com.ultraviolette.navigator/.MainActivity`.
- Landscape only (`sensorLandscape`). If the user says it looks portrait, fix orientation / relaunch before judging layout.
- Placeholder telemetry: `ClusterPlaceholders` — do not invent live CAN/telemetry wiring during UI verify.

## Smoke vs design verify

| Layer | Source of truth |
|-------|-----------------|
| Smoke | Deploy focus / install / `wm size` in `latest-report.md` |
| Design | `docs/harness/design-facts.md` + Compose sources vs reference intent |
| Pixels on glass | User / device — not agent vision on screencaps |
