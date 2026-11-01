---
name: publish-release
description: >-
  Build an installable Ultraviolette Navigator APK, write release notes, commit,
  push to git, and publish a GitHub release with the APK attached. Use when the
  user asks to /publish-release, publish a release, cut a release, ship an APK,
  or generate release notes and push.
disable-model-invocation: true
---

# Publish release

Ship a versioned APK + GitHub release for this repo (`x47-for-all`).

## Checklist

```
Publish progress:
- [ ] 1. Decide version (semver)
- [ ] 2. Bump versionCode / versionName
- [ ] 3. Build APK
- [ ] 4. Write release notes
- [ ] 5. Commit + push
- [ ] 6. Create GitHub release with APK
```

## 1. Version

- Read `versionCode` / `versionName` in `app/build.gradle.kts`.
- Default bump: patch (`1.0.0` → `1.0.1`). Use minor for user-visible features, major for breaking changes.
- Always increment `versionCode` by 1 (Android install requirement).
- Tag format: `v{versionName}` (example: `v1.1.0`).

Confirm the version with the user if they did not specify one.

## 2. Bump

Edit only:

```kotlin
versionCode = N
versionName = "X.Y.Z"
```

in `app/build.gradle.kts` `defaultConfig`.

## 3. Build APK

No release keystore is checked in. Prefer the **debug** APK for sideload distribution (installable). Unsigned release APKs are not useful for users.

```bash
./scripts/build-apk.sh debug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

Copy to a versioned name for the release asset (APKs are gitignored — do not commit):

```bash
mkdir -p dist
cp app/build/outputs/apk/debug/app-debug.apk \
  "dist/ultraviolette-navigator-${VERSION}.apk"
```

Shell needs `required_permissions: ["all"]` for Gradle/Android SDK.

Optional helper (does build + copy when args are passed through):

```bash
./scripts/publish-release.sh --version X.Y.Z --notes-only   # notes helpers
./scripts/publish-release.sh --version X.Y.Z --build-only   # APK only
```

Full cut (build + notes stub + gh release) is usually driven by this skill, not a single blind script — the agent must draft notes from commits.

## 4. Release notes

Create `docs/releases/vX.Y.Z.md` from `git log` since the previous tag (or since the last release commit if no tags).

Template:

```markdown
# vX.Y.Z — YYYY-MM-DD

## Highlights
- …

## Changes
- …

## Install
1. Download `ultraviolette-navigator-X.Y.Z.apk` from this GitHub release.
2. Allow install from the browser/file manager if prompted.
3. Open the APK and install.

Package: `com.ultraviolette.navigator`
```

Keep highlights user-facing; put build/harness-only work under a short “Internal” bullet if needed.

## 5. Commit + push

Commit **source** only: version bump, `docs/releases/vX.Y.Z.md`, and any skill/script updates. Never commit `*.apk` / `dist/`.

```bash
git add app/build.gradle.kts docs/releases/vX.Y.Z.md
# plus skill/script paths if changed
git commit -m "$(cat <<'EOF'
Release vX.Y.Z

EOF
)"
git push -u origin HEAD
```

Follow the repo commit / push rules (HEREDOC message, no force, no hook skip). Push needs `required_permissions: ["all"]`.

## 6. GitHub release

```bash
gh release create "vX.Y.Z" \
  "dist/ultraviolette-navigator-X.Y.Z.apk" \
  --title "vX.Y.Z" \
  --notes-file "docs/releases/vX.Y.Z.md"
```

If the tag already exists locally/remotely, stop and ask — do not delete tags unless the user requests it.

Return the release URL when done.

## Hard rules

- Do not commit APKs (`*.apk` is gitignored).
- Do not invent a release keystore; document if release-signed builds are requested later.
- Do not force-push or `--no-verify`.
- If working tree has unrelated dirty files, ask before bundling them into the release commit.
- After publish, summarize: version, APK path, release URL, and one-line highlight.
