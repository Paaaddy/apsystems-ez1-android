# APsystems EZ1 Android Monitor

[![Build & Test](https://github.com/Paaaddy/apsystems-ez1-android/actions/workflows/build.yml/badge.svg)](https://github.com/Paaaddy/apsystems-ez1-android/actions/workflows/build.yml)
[![Release Please](https://github.com/Paaaddy/apsystems-ez1-android/actions/workflows/release-please.yml/badge.svg)](https://github.com/Paaaddy/apsystems-ez1-android/actions/workflows/release-please.yml)

Android app for monitoring APsystems EZ1 microinverters on your local network. Requires no cloud — communicates directly over LAN.

## Features

- Real-time power output monitoring
- Daily/total energy production
- Inverter status and alerts
- Local network communication (no cloud required)
- Home screen widget

## Requirements

- Android 8.0+ (API 26+)
- APsystems EZ1 microinverter on local network

## Building

<!-- AUTO-GENERATED from app/build.gradle.kts + .github/workflows -->
```bash
./gradlew test lint assembleDebug   # test + lint + debug APK (CI path)
./gradlew assembleDebug             # debug APK only
./gradlew assembleRelease           # signed release APK (requires signing env vars)
./gradlew test                      # unit tests only
./gradlew lint                      # static analysis only
```
<!-- /AUTO-GENERATED -->

## Getting updates

Download APKs from [GitHub Releases](https://github.com/Paaaddy/apsystems-ez1-android/releases).

Each release includes a signed APK named `ez1-monitor-vX.Y.Z.apk`.

### Obtainium (recommended for auto-updates)

[Obtainium](https://github.com/ImranR98/Obtainium) auto-updates sideloaded apps from GitHub releases.

**Quick setup:** Import this repo directly in Obtainium:
- Source URL: `https://github.com/Paaaddy/apsystems-ez1-android`
- Asset regex: `ez1-monitor-v.*\.apk`

Or import via the `obtainium.json` config file at the root of this repo.

### Manual updates

1. Download the latest APK from [Releases](https://github.com/Paaaddy/apsystems-ez1-android/releases)
2. Open the APK on your device — Android will offer to update the existing install
3. No uninstall needed (signing key is stable across releases)

> **Note:** If you installed a very early release (before v1.1.0) and the update fails with a signature error, uninstall first. After that, all future updates install over the top.

## Contributing

### Making a release

Releases are managed by [Release Please](https://github.com/googleapis/release-please).

1. Merge any feature/fix PRs to `master` using [conventional commits](https://www.conventionalcommits.org/):
   - `feat: ...` — bumps minor version
   - `fix: ...` — bumps patch version
   - `feat!: ...` or `BREAKING CHANGE:` footer — bumps major version
2. Release Please automatically opens a PR titled `chore(master): release ez1-monitor X.Y.Z` that updates `CHANGELOG.md`, `version.txt`, and `versionName` in `app/build.gradle.kts`
3. Review and merge the Release Please PR
4. On merge, the release workflow automatically:
   - Creates a GitHub Release with generated release notes
   - Builds the APK
   - Attaches `ez1-monitor-ez1-monitor-vX.Y.Z.apk` to the release

### Signing setup (one time)

Generate a keystore and add the following GitHub Secrets:

<!-- AUTO-GENERATED from .github/workflows/release.yml -->
| Secret | Value |
|--------|-------|
| `KEYSTORE_BASE64` | `base64 -w0 ez1-release.jks` |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |
<!-- /AUTO-GENERATED -->

```bash
# Generate keystore
keytool -genkey -v \
  -keystore ez1-release.jks \
  -alias ez1 \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# Encode for GitHub Secret
base64 -w0 ez1-release.jks
```

For local release builds, set the same env vars:

```bash
export KEYSTORE_FILE=/path/to/ez1-release.jks
export KEYSTORE_PASSWORD=your-password
export KEY_ALIAS=ez1
export KEY_PASSWORD=your-key-password
./gradlew assembleRelease
```

Without these env vars, `assembleRelease` falls back to debug signing (local dev only).

### Repository settings (required for Dependabot auto-merge)

Enable auto-merge in GitHub repo settings:
**Settings → General → Allow auto-merge**
