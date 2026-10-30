# Ultraviolette Navigator

Android cluster UI that replicates the UV X47 / F77 speedometer layout.

## First build

All telemetry is placeholder-driven via `ClusterPlaceholders`. Landscape-locked, edge-to-edge Compose screen.

## Open in Android Studio

1. Open this folder in Android Studio
2. Sync Gradle
3. Run on a landscape emulator or device (API 26+)

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

## Agentic harness (deploy → design facts → verify)

With a device connected (`adb devices`):

```bash
./scripts/harness.sh
```

This builds, installs, launches, settles, smoke-checks focus, and writes `docs/harness/latest-report.md` plus `docs/harness/design-facts.md` (colors, layout constants, placeholders scraped from Compose). Agents verify against **source + reference intent**, not screencaps — captures are too inaccurate for color/spacing judgment.

| Script | Purpose |
|--------|---------|
| `./scripts/harness.sh` | Deploy + smoke + design-facts report |
| `./scripts/deploy.sh` | Build / install / launch |
| `./scripts/probe-design.sh` | Scrape design tokens into `docs/harness/` |
| `./scripts/screengrab.sh` | Optional human PNG archive only |
| `./scripts/build-apk.sh` | APK assemble |
| `./scripts/install-apk.sh` | Install existing APK |

## Layout map

| Area | Placeholder fields |
|------|--------------------|
| Speed | `speedKmh`, `speedUnit` |
| Odo / trip | `odometerKm`, `tripKm` |
| Energy / range | `energyWhPerKm`, `energyRecommended`, `rangeKm` |
| Status pill | `timeText`, `batteryPercent`, `signalBars` |
| Lean / weather | `leanAngleDeg`; `ambientTempC` + condition from Open-Meteo (GPS) |
| Mode bar | `rideMode`, `regenLevel`, `efficiencyLabel` |
| Icons | `tractionControlOn`, `absOn` |

Reference screenshot: `docs/uv-x47-reference.png`
