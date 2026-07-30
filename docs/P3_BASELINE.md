# P3 Baseline

Date: 2026-07-29  
Base commit: `4ccf9be2277c4cceba3043951d671970a77164a0`

## Repository State

- Branch: `main` tracking `origin/main`.
- Worktree at baseline: clean.
- Existing architecture: Compose feature modules, DataStore preferences, Hilt, Room, WorkManager, Media3, and local Offline AI modules.
- Existing app-language preference: ambiguous free-form `language_code` string, defaulting to English.
- Existing root behavior: NavHost start destination was selected from the initial UI state before DataStore completed loading.

## Verification Baseline

P3 focused checks run after the first localization slice:

- `./gradlew :core:datastore:testDebugUnitTest`: pass in 44 seconds.
- `./gradlew :app:compileDebugKotlin`: pass in 270.7 seconds.

The first combined invocation timed out and temporarily left generated KSP files locked. The focused reruns completed after the stale process exited; no source failure remained.

## Screenshot Matrix

No device or emulator was connected on 2026-07-29. The required before-change screenshot matrix was therefore not captured:

- onboarding;
- Home in English and Vietnamese;
- Settings in English and Vietnamese;
- Recording Detail idle and processing;
- transcript result;
- insights result.

This is an open evidence gap. Prior P2 screenshots or device observations are not substituted as P3 baseline evidence.

## Device Evidence

`adb devices -l` returned no connected devices on 2026-07-29. Device-specific UI, accessibility, locale persistence, and Android 13 system language synchronization still require physical-device or emulator verification.

## Later P3 Device Evidence

After the baseline was recorded, an OPPO CPH2339 running Android 12 became available. Its 720 x 1612, 320 dpi screen corresponds to 360 dp width. The Offline AI device smoke suite passed 4/4 cases. The device was locked by a user pattern during screenshot work, so ADB could not perform visual navigation or capture.

A local Pixel 8 emulator was also attempted for deterministic screenshots. Its System UI repeatedly became unresponsive under memory pressure and the app process could not remain foregrounded, so those screenshots were rejected rather than treated as product evidence. The original before-change screenshot gap cannot be reconstructed after implementation.

The OPPO was later unlocked and used for post-change native-resolution acceptance. QA verified app launch, Vietnamese locale persistence across force-stop/reinstall, microphone recording and save, populated Library navigation, Recording Detail tabs, dark mode, and compact-label behavior. Evidence is stored under `docs/screenshots/p3`. The device was returned to its physical 720 x 1612 size, 320 dpi density, and 1.0 font scale after testing.
