# Visual verify checklist (UV X47 cluster)

Score against **Compose source** + `docs/harness/design-facts.md` + reference **intent**.
Primary COMBAT reference (intent): `docs/screen-reference-2.jpg`.
Hero product reference (intent): `docs/uv-x47-reference.png`.

**Do not** pass/fail from `docs/captures/*.png` or other screencaps.

Mark each row **pass** / **fail** / **n/a**. Failures need a one-line fix hypothesis pointing at a source file/token.

## Global

| # | Check |
|---|--------|
| G1 | Landscape cluster fills the usable area (`sensorLandscape`; bezels via `MinEdgeBezel` / cutout) |
| G2 | Face fill is `ClusterBackground`; short-edge bars are `ClusterBezel` |
| G3 | Short-edge black bezels present; no accidental system-bar chrome in layout |
| G4 | No DEMO / placeholder font watermarks |

## Top status bar

| # | Check |
|---|--------|
| T1 | Left: hill-hold / TPMS-style icons present when placeholders expect them |
| T2 | Center: time + battery % from `ClusterPlaceholders` |
| T3 | Right: signal / wifi / TC / ABS icons aligned in `TopStatusBar` |
| T4 | Bar sits flush under the top edge (no large empty gap in layout) |

## Side metrics

| # | Check |
|---|--------|
| L1 | Left rail: avg stack uses shared `SideValueSize` / panel tokens |
| R1 | Right rail: trip time stack mirrors left; lean / temp as designed |
| S1 | `SideRailWidth` + edge pads keep content off bezels |

## Center speed cluster

| # | Check |
|---|--------|
| C1 | Large speed glyphs dominate the center (`CenterSpeedCluster`) |
| C2 | `km` / `h` unit split flanking speed |
| C3 | Accent from `rememberClusterAccent` matches ride mode (COMBAT red / BALLISTIC cyan) |
| C4 | Control-centre affordance present where designed |
| C5 | Tapping Control Centre opens modular grid; top bar stays; back/system-back dismisses |
| C6 | Control Centre keeps phone-relevant tiles (battery/trips/ride mode/sensors/EFF/SPD/Google Maps); no ABS/TPMS/dashcam/radar |
| C7 | TRIPS tile opens saved-ride history; back returns to Control Centre |

## Bottom / mode

| # | Check |
|---|--------|
| B1 | Gray bottom bridge / wings span as in `BottomModeBar` / wing comps |
| B2 | COMBAT / mode notch centered |
| B3 | SPD + EFF 10-bar stripes visible; footer pads avoid clip |
| B4 | No floating cards / accidental Compose preview chrome |

## Placeholders (first-build data)

Defaults live in `ClusterPlaceholders` / `design-facts.md`. Unless the task changed data, expect COMBAT-like values (speed `---`, avg `---`, trip time `0:00`, battery `72`, EFF/SPD bars off while idle, etc.).
