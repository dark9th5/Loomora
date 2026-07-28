# Final Audit Report

Date: 2026-07-28

Recommendation: `INTERNAL TEST / RELEASE CANDIDATE CANDIDATE`, not production release-ready until production signing is configured with the real private key outside the repo.

## Summary

P2.7 hardened the release surface and website truthfulness. `clean check`, debug build, unsigned release APK build, website build, unit tests, and connected offline AI device smoke now pass. Release signing is correctly blocked when production signing inputs are missing, and the release APK audit did not find bundled model packs or private signing material.

This is not ready for production release only because no real production signing key is configured in this workspace and no signed production APK smoke test was performed. Do not replace this with a fake bundled key.

## Gate Results

| Gate | Result | Evidence |
| --- | --- | --- |
| `.\gradlew.bat testDebugUnitTest --console=plain --no-daemon --max-workers=1` | Pass | `BUILD SUCCESSFUL in 1m 56s`; 489 actionable tasks. |
| `.\gradlew.bat assembleDebug --console=plain --no-daemon --max-workers=1` | Pass | `BUILD SUCCESSFUL in 7m 7s`; `app-debug.apk` = 195,716,923 bytes. |
| `.\gradlew.bat assembleRelease --console=plain --no-daemon --max-workers=1` | Pass unsigned | `BUILD SUCCESSFUL in 11m 57s`; `app-release-unsigned.apk` = 187,953,411 bytes. |
| `.\gradlew.bat :app:validateReleaseSigning --console=plain --no-daemon --max-workers=1` | Fail expected | Missing production signing inputs; release publishing is blocked rather than falling back to debug signing. |
| `npm.cmd run build` in `web/` | Pass | Next.js production build completed and generated 20 pages. |
| `.\gradlew.bat clean check --console=plain --no-daemon --max-workers=1` | Pass | `BUILD SUCCESSFUL in 15m 20s`; 1442 actionable tasks. |
| `.\gradlew.bat :core:offlineai:connectedDebugAndroidTest --console=plain --no-daemon --max-workers=1` | Pass | OPPO `CPH2339` Android 12: 4 tests, 0 skipped, 0 failed; `BUILD SUCCESSFUL in 2m 30s`. |

## APK Audit

Release artifact: `app/build/outputs/apk/release/app-release-unsigned.apk`

- APK size: 187,953,411 bytes.
- Native libraries by ABI:
  - `arm64-v8a`: 7 entries, 52,054,672 bytes.
  - `armeabi-v7a`: 6 entries, 21,849,260 bytes.
  - `x86`: 6 entries, 36,617,508 bytes.
  - `x86_64`: 7 entries, 60,102,040 bytes.
- No `.onnx`, `.litertlm`, `.modelpack`, keystore, private-key, `.jks`, or `.keystore` entries were found in the release APK scan.
- R8 remains disabled: `release.isMinifyEnabled = false`.

## Website Truthfulness

- Removed stale remote-AI, over-strong privacy, deep-generative, unbounded remote processing, and enabled speech-enhancement claims from the website copy.
- Updated pricing/pro/license/privacy language to signed offline capabilities and on-device/local model processing.
- Added `docs/WEBSITE_FEATURE_MATRIX.md` as the source of truth for Available/Beta/Coming Soon wording.

## Release Blockers

- Production release signing inputs must be configured outside the repo and validated with `:app:validateReleaseSigning`.
- A signed release APK must be installed and smoked on a physical device.
- Process-kill/reboot queue recovery, low-storage behavior, and wider device tiers remain manual release-hardening follow-ups.
