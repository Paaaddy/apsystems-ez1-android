# TODOs

## In-app update checker (deferred from update mechanism PR)

After stable keystore is confirmed and at least one clean upgrade is verified, add:
- `UpdateChecker` (Application-level singleton, own OkHttpClient with 10s/30s timeouts)
- `UpdateState` sealed class: `Available(version, downloadUrl)`, `UpToDate`, `Unknown`
- `DashboardUiState.updateAvailable: UpdateState` (separate field — NOT snackbarMessage)
- `DashboardScreen`: show `Snackbar` when `updateAvailable is Available`, with "Download" action (`LocalUriHandler`)
- `UpdateCheckerTest` using MockWebServer (already in test deps)
- Cache result per process lifetime (MutableStateFlow in UpdateChecker initialized once)

Design decisions (from autoplan review, 2026-05-06):
- Affordance: `Snackbar` via existing `SnackbarHostState` (not custom banner)
- All failure states: silent (no UI)
- Download action: `LocalUriHandler.current.openUri(url)` — no-ops gracefully if no browser

## F-Droid listing (long-term)

F-Droid reaches ~1M Android users and auto-fetches from GitHub releases.
- Add `fdroid.yml` metadata to repo
- Submit to IzzyOnDroid (faster inclusion than main F-Droid repo)
- Effort: ~1 day for setup, 1–2 weeks for review
