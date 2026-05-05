# APsystems EZ1 Android Monitor

[![Build & Test](https://github.com/Paaaddy/apsystems-ez1-android/actions/workflows/build.yml/badge.svg)](https://github.com/Paaaddy/apsystems-ez1-android/actions/workflows/build.yml)

Android app for monitoring APsystems EZ1 microinverters on your local network. Requires no cloud — communicates directly over LAN.

## Features

- Real-time power output monitoring
- Daily/total energy production
- Inverter status and alerts
- Local network communication (no cloud required)

## Requirements

- Android 8.0+ (API 26+)
- APsystems EZ1 microinverter on local network

## Building

```bash
./gradlew assembleDebug          # debug APK
./gradlew test                   # unit tests
./gradlew lint                   # static analysis
./gradlew assembleRelease        # signed release APK (requires signing env vars)
```

## Releases

Download signed APKs from [GitHub Releases](https://github.com/Paaaddy/apsystems-ez1-android/releases).

Each release includes:
- Signed APK (`app-release.apk`)
- SHA256 checksum (`app-release.apk.sha256`)

Verify the checksum before installing:
```bash
sha256sum -c app-release.apk.sha256
```

## Contributing

### Making a release

1. Create a branch named `release/vX.Y.Z` (e.g. `release/v1.0.1`)
2. Bump `versionCode` and `versionName` in `app/build.gradle.kts`
   - `versionName`: semver string, e.g. `"1.0.1"`
   - `versionCode`: monotonic integer, e.g. `101` for `1.0.1`
3. Open a PR from `release/vX.Y.Z` to `master`
4. After approval and merge, the release workflow automatically:
   - Builds a signed APK
   - Verifies the APK signature with `apksigner`
   - Creates a git tag `vX.Y.Z`
   - Publishes a GitHub Release with the APK and SHA256 checksum

### Signing setup (one time)

Generate a keystore and add the following GitHub Secrets:

| Secret | Value |
|--------|-------|
| `KEYSTORE_BASE64` | `base64 -w0 ez1-release.jks` |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |

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

Without these env vars, `assembleRelease` falls back to debug signing (for local dev only).

### Repository settings (required for Dependabot auto-merge)

Enable auto-merge in GitHub repo settings:
**Settings → General → Allow auto-merge**
