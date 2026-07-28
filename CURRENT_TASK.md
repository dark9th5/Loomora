# Current Task: P2.7 Release Hardening, Device Tiers and Website Truthfulness

Date: 2026-07-28

## Scope

Implement P2.7 only:
- run and record release gates with actual results;
- audit signing, migrations, R8/minify, APK contents, dependency pins, privacy/logging, model distribution, and website claims;
- create release hardening docs and source-of-truth feature/device tables;
- update website wording so only evidence-backed features are advertised as Available/Beta;
- keep LiteRT-LM/deep generative summaries as Experimental/Coming Soon, not Available.

Do not add a new LLM runtime, do not enable R8 without release regression evidence, and do not call the app READY if gates or evidence are incomplete.

## Pre-Edit Audit

- Worktree is already dirty with P2.2-P2.6 changes and local prompt-file rename/delete artifacts. These will not be reverted.
- P2.4 production insights path is `HeuristicMeetingInsightEngine` through `FallbackMeetingInsightEngine`; optional llama.cpp boundary is unavailable by default and LiteRT-LM is not accepted as stable production meeting insight output.
- P2.5 persistent WorkManager queue is implemented and debug/app tests passed; foreground notification and process-kill/reboot smoke remain follow-up.
- P2.6 signed offline license and durable trial core is implemented; QR/file SAF import and Keystore hardening remain follow-up.
- Release minify/R8 is currently `isMinifyEnabled = false` in `app/build.gradle.kts`.
- Release signing does not fall back to debug signing. `assembleRelease` can compile unsigned when production signing inputs are absent; `validateReleaseSigning` fails when production signing inputs are missing.
- Version catalog pins exact versions; there is no obvious `latest.release`.
- Website exists in `web/` and currently contains over-strong/outdated claims:
  - “Cloud AI Transcriptions” and “Cloud AI Summary Extractions”;
  - “SMART INSIGHTS” marked `READY`;
  - FAQ says audio is transmitted over HTTPS when tapping Transcribe;
  - Pro plan promises cloud AI transcripts and speaker/action items;
  - “Foreground Protection” wording implies full protection while P2.5 foreground progress notification remains follow-up;
  - “Speech Clarity Filter” appears enabled in mockup though P1.4 rejects speech clarity export.
- Existing docs include release signing and audio editor limitations, but not final audit, supported devices, model manifest, privacy, third-party notices, release checklist, or website feature matrix.

## Plan

1. Update website copy to truthful current state:
   - local/offline AI after model import;
   - smart extractive insights, not deep generative LLM;
   - diarization Beta;
   - generative enhancement Coming Soon/Experimental;
   - license wording aligned to P2.6;
   - no cloud AI claims.
2. Create/update release docs:
   - `FINAL_AUDIT_REPORT.md`;
   - `SUPPORTED_DEVICES.md`;
   - `MODEL_MANIFEST.md`;
   - `PRIVACY.md`;
   - `THIRD_PARTY_NOTICES.md`;
   - `RELEASE_CHECKLIST.md`;
   - `docs/WEBSITE_FEATURE_MATRIX.md`;
   - `KNOWN_ISSUES.md`.
3. Run required release gates and record exact command outcomes:
   - `.\gradlew.bat clean check`;
   - `.\gradlew.bat testDebugUnitTest`;
   - `.\gradlew.bat assembleDebug`;
   - `.\gradlew.bat assembleRelease`;
   - `connectedDebugAndroidTest` only if practical with current USB/device state.
4. Audit APK/release artifacts:
   - debug/private signing material;
   - private license signing key/test token strings;
   - bundled large model files;
   - native libraries/ABI support;
   - APK sizes.
5. Generate feature truth table with feature status, requirement, evidence, and website wording.
6. Define device tiers from actual evidence:
   - Core;
   - Speech AI;
   - Lightweight Insights;
   - Experimental Generative Insights.
7. Keep R8 deferred unless release build/regression evidence is enough.
8. Update tracking docs with actual command/device evidence and final recommendation.

## Acceptance Criteria

- Release build gates pass or `FINAL_AUDIT_REPORT.md` honestly reports NOT READY/INTERNAL TEST with blockers.
- Website has Available/Beta/Coming Soon status matrix.
- Heuristic insights are described as extractive/evidence-based.
- LiteRT-LM/deep generative summary is not advertised as Available.
- Device/model limits are backed by actual evidence.
- License wording matches P2.6 and does not promise immediate offline revocation.
- Privacy and third-party/model attribution docs exist.
- No debug signing/private key/secret is found in release artifacts by audit commands.
- R8 remains disabled with documented rationale unless fully verified.

## Results

Completed P2.7 release hardening and truthfulness pass with all available local gates passing. The remaining production-only blocker is real release signing: no production private key is configured in this workspace, and none was bundled or faked.

- Website copy was corrected across the Next app and legacy `web/index.html` so it no longer advertises cloud AI, deep generative summaries as Available, absolute privacy, enabled speech clarity export, or all-device support.
- Added release truth docs: `FINAL_AUDIT_REPORT.md`, `RELEASE_CHECKLIST.md`, `SUPPORTED_DEVICES.md`, `MODEL_MANIFEST.md`, `PRIVACY.md`, `THIRD_PARTY_NOTICES.md`, and `docs/WEBSITE_FEATURE_MATRIX.md`.
- Fixed release/build hardening issues discovered by gates:
  - moved the direct sherpa `.aar` package dependency from `:core:offlineai` implementation to app packaging plus compile-only library use, avoiding broken local-AAR library publication;
  - removed default WorkManager startup initializer from the manifest because the app provides Hilt WorkManager configuration;
  - removed the production `valid-pro-token` entitlement stub path;
  - fixed subscription test wiring for the P2.6 entitlement repository;
  - added AndroidX `UnstableApi` opt-in for Media3 transformer usage instead of suppressing lint.
- Release APK audit found no bundled `.onnx`, `.litertlm`, `.modelpack`, keystore, private-key, `.jks`, or `.keystore` entries.

## Commands

- `git status --short`: dirty tree confirmed; P2.2-P2.6 changes are uncommitted; prompt-file rename/delete artifacts are present and will not be touched.
- `rg "LiteRT|LLM|generative|summary|insight|speaker|offline|privacy|license|pricing|Pro|cloud|sync|API|all phones|mọi|every"`: found truthful P2.6 docs plus outdated/over-strong website and blueprint claims; website claims in `web/app/page.tsx` are in P2.7 scope.
- `Get-Content app/build.gradle.kts`: release minify is off, release signing uses env/local keystore only when complete, no debug fallback for release.
- `Get-Content gradle/libs.versions.toml`: dependencies are exact pinned versions.
- `Get-Content web/app/page.tsx`: confirmed cloud AI, READY insights, speech clarity, and over-strong protection claims need correction.
- `.\gradlew.bat testDebugUnitTest --console=plain --no-daemon --max-workers=1`: pass, `BUILD SUCCESSFUL in 1m 56s`.
- `.\gradlew.bat assembleDebug --console=plain --no-daemon --max-workers=1`: pass, `BUILD SUCCESSFUL in 7m 7s`; debug APK size 195,716,923 bytes.
- `.\gradlew.bat assembleRelease --console=plain --no-daemon --max-workers=1`: pass unsigned, `BUILD SUCCESSFUL in 11m 57s`; release unsigned APK size 187,953,411 bytes.
- `.\gradlew.bat :app:validateReleaseSigning --console=plain --no-daemon --max-workers=1`: fail expected; production signing env/local inputs are missing and release publishing is blocked.
- `npm.cmd run build` in `web/`: pass; Next.js production build completed.
- `.\gradlew.bat :core:audio:lintDebug --console=plain --no-daemon --max-workers=1`: pass after Media3 AndroidX opt-in.
- `.\gradlew.bat clean check --console=plain --no-daemon --max-workers=1`: pass after longer rerun, `BUILD SUCCESSFUL in 15m 20s`; 1442 actionable tasks.
- `& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices -l`: detected OPPO `CPH2339`.
- `.\gradlew.bat :core:offlineai:connectedDebugAndroidTest --console=plain --no-daemon --max-workers=1`: pass on OPPO `CPH2339` Android 12; 4 tests, 0 skipped, 0 failed; `BUILD SUCCESSFUL in 2m 30s`.

## Warnings / Follow-Up

- R8 should remain deferred unless release regression smoke covers Room, Hilt, WorkManager, serialization, sherpa JNI, license parsing, and insight schema on release.
- Current evidence is strongest on OPPO `CPH2339`; wider device matrix is not tested.
- Website pages should remain conservative until a deployable release candidate and model distribution policy are finalized.
- Production release remains blocked until a real production signing key is configured outside the repo, `:app:validateReleaseSigning` passes with that key, and the signed production APK is smoke-tested.
