# TODOs

## CI/CD

### Add concurrency group to release.yml
**What:** Add `concurrency:` block to `.github/workflows/release.yml` so re-triggered runs queue instead of racing.
**Why:** If release.yml is re-run (via GitHub UI, cherry-pick, or webhook replay), two runs can race to create the same tag/release — the second fails loudly or creates a duplicate.
**Context:** Identified by Codex outside voice during eng review. Risk is low for solo development but will bite when you re-run a failed release. Fix is 3 lines.
**How:** Add at job level: `concurrency: { group: release-${{ github.event.pull_request.number }}, cancel-in-progress: false }`
**Depends on:** release.yml exists (implement after this PR lands)
