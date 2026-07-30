# Current Task: P3 Localization, Onboarding, AI Performance and UI Redesign

Date: 2026-07-29

## Scope

Implement the P3 master specification in dependency order. P3.0-P3.7 and the final evidence audit are complete, with the representative benchmark input gap documented separately.

## Baseline

- Base commit: `4ccf9be2277c4cceba3043951d671970a77164a0`.
- Worktree was clean before P3 edits.
- A physical OPPO CPH2339 and a Pixel 8 emulator became available after baseline capture; see `docs/P3_BASELINE.md` for their evidence limitations.
- Prior P2 device evidence must not be represented as a new P3 measurement.

## Completed

- Added stable AndroidX AppCompat `1.7.1`, Android 12 locale auto-storage metadata, and generated per-app locale config with English as the unqualified fallback.
- Added typed `SupportedAppLanguage` and `TranscriptLanguagePreference` preferences.
- Preserved the legacy `language_code` key as a migration source; `en`, `vi`, and `vi-VN` map to supported values and unknown values fall back to English.
- Added independent app-language, transcript-language, and onboarding-version storage APIs.
- Updated Settings to write the typed app-language preference.
- Updated the root UI state with language, onboarding version, and explicit loading state.
- Changed the root activity to apply locales through `AppCompatDelegate` only when needed.
- Added two-way synchronization so Android 13+ system language changes update DataStore instead of being overwritten; the in-app picker persists before applying the locale.
- Deferred NavHost creation until DataStore emits real preferences; a loading screen is shown meanwhile.
- Added unit coverage for defaults and legacy language migration.
- Moved Settings copy into `feature:settings` English/Vietnamese resources and replaced model-state/capability literals with localized enum mappings.
- Replaced free-form model import failure text with a typed UI error.
- Rebuilt first-run onboarding as a five-page `HorizontalPager` flow with language selection, recording guidance, Offline AI expectations, privacy, and optional microphone permission.
- Moved onboarding page and permission state into `OnboardingViewModel`, added completion guarding, and persisted onboarding version 2.
- Added a separate tutorial route from Settings that reuses the guided flow without resetting onboarding completion or automatically requesting microphone permission.
- Added structured `OfflineAiStageTimings` persisted as safe JSON on analysis jobs through Room migration 7→8.
- Split decode/resample and speech detection timings in the audio preprocessor.
- Removed duplicate source hashing by passing the coordinator fingerprint into preprocessing; regression coverage verifies one fingerprint operation per job.
- Published a base transcript revision and `AVAILABLE` status before diarization and insights, allowing incremental UI results.
- Preserved an available transcript when later analysis is cancelled or fails.
- Stopped unloading cached speech engines before lightweight heuristic insights.
- Added shared shape, spacing, and dimension tokens and made the shared top app bar title/back action localization-safe.
- Rebuilt Home around one `LazyColumn` with a recording hero, 2x2 recording modes, active AI work, compact trial status, recent recordings, and working detail navigation.
- Preserved quick-mode information through navigation and visibly preselected it in Recorder.
- Added Overview, Transcript, and Insights navigation to Recording Detail; transcript rows now seek by timestamp and highlight the active playback segment.
- Moved Recording Detail copy into English/Vietnamese feature resources and replaced free-form processing labels with typed `AiProcessingStage` values localized in UI.
- Added persisted transcript language, default analysis mode, and Offline AI performance mode preferences with safe defaults.
- Replaced the hard-coded Settings version with package metadata and exposed the new Offline AI preferences in Settings.
- Added a model-keyed, language-aware and thread-profile-aware Sherpa recognizer cache with deterministic resource release and unit coverage.
- Made transcription consume detected VAD speech windows, with full-audio fallback only when no usable windows exist.
- Persisted and honored runtime processing options for transcript language, performance profile, diarization and insight mode.
- Added localized foreground WorkManager notification metadata and analysis cancellation from Recording Detail.
- Split Recording Detail route and dialogs out of the main screen file.
- Localized Home, Recorder, Editor, Library and Subscription into feature-owned English/Vietnamese resources.
- Added resource parity and common unaccented-Vietnamese regression tests.
- Expanded model cards with runtime, version, size and verified checksum metadata.
- Added useful transcript/insight/processing state to recent Home recording cards.
- Fixed the runtime theme to inherit from AppCompat, preventing launch crashes when applying per-app locales.
- Made compact bottom-navigation labels stable and localized Recording Detail date/time formatting through the active locale.
- Added a verified automatic download/install path for the recommended multilingual transcription model in onboarding and Settings, with progress and manual-import fallback.
- Renamed the recording action to Offline analysis and added immediate missing/corrupt-model blocking before queue creation.
- Fixed device analysis jobs remaining indefinitely in QUEUED when WorkManager could not instantiate the Hilt worker by adding the AndroidX Hilt worker compiler.
- Reconciled pending Room jobs with terminal WorkManager states, made retry replace failed work, and clarified the localized queued message.
- Verified on the physical OPPO that retry progressed through local preparation and completed both the transcript and extracted insights.
- Added automatic installation of the Sherpa-compatible Pyannote segmentation and 3D-Speaker embedding bundle; stale READY catalog records now require a verified upgrade.
- Added real full-analysis model gating, deterministic speech-engine release between stages, Android 12 foreground-promotion fallback, and rerunnable terminal jobs.
- Fixed reanalysis to bypass cached transcript, diarization, and insight revisions when explicitly requested while preserving idempotency for active/recovered work.
- Added sentence/long-text segmentation, invalid Whisper timestamp fallback, contiguous speaker labels, and non-duplicating transcript-to-speaker fusion that skips long silent gaps.
- Added short-transcript insight quality guards so sparse recordings do not invent key points, decisions, or chapters.
- Exposed local user editing for insight title/summary and persisted edits as USER_EDITED revisions.
- Added an optional Whisper Base int8 catalog entry with verified encoder, decoder, and token metadata.
- Added persisted transcription-model selection in Settings; Tiny remains the stable default and Base can be selected explicitly.
- Made queued analysis honor the exact selected model id and reject unavailable selected models instead of silently substituting another model.
- Validated every catalog model file, preserved complete manifest metadata, and fixed total model-size display.
- Added cross-segment repeated-phrase filtering that preserves raw model text and runs before speaker fusion.

## Verification

- `:core:datastore:testDebugUnitTest`: pass, `BUILD SUCCESSFUL in 44s`.
- `:app:compileDebugKotlin`: pass after adding `app/src/main/res/resources.properties`; exit code 0 in 270.7 seconds.
- `:feature:settings:compileDebugKotlin`: pass after feature-owned localization resources were added.
- `:feature:onboarding:testDebugUnitTest :feature:settings:compileDebugKotlin :app:compileDebugKotlin`: pass, `BUILD SUCCESSFUL in 5m 17s`.
- `:core:offlineai:testDebugUnitTest`: pass after timing, fingerprint, incremental revision, and Room schema changes; `BUILD SUCCESSFUL in 5m 21s`.
- Final `:app:compileDebugKotlin` after Room 8 and P3.3 API changes: pass in 160.8 seconds.
- Home, Recorder, design system, and app compile gate: pass in 108.9 seconds.
- Recording Detail compile gates: pass after tabs, resources, and transcript UI changes.
- `:core:offlineai:testDebugUnitTest :feature:recordingdetail:testDebugUnitTest :feature:settings:compileDebugKotlin :app:compileDebugKotlin`: pass in 74.5 seconds.
- English/Vietnamese resource key parity audit: pass for every module that owns both resource sets.
- First combined gate was interrupted by timeout and left a KSP file lock; a later run reported that environmental lock. Focused reruns passed after the stale process exited.
- `git diff --check`: pass (line-ending conversion warnings only).
- Final full `testDebugUnitTest lintDebug assembleDebug` gate after all source changes: pass, `BUILD SUCCESSFUL in 4m22s` (incremental rerun).
- `:core:offlineai:testDebugUnitTest`: 49 tests passed after recognizer-cache coverage was added.
- `:app:connectedDebugAndroidTest`: pass after adding the missing AndroidX runner dependencies; the app module currently contains 0 instrumentation tests.
- Physical-device Offline AI smoke suite: 4/4 passed (Whisper, diarization, streaming AAC decode, extractive insights).
- Physical OPPO UI QA: onboarding, Home EN/VI, Settings EN/VI/dark, Recorder, populated Library, and all Recording Detail tabs captured at native 360 dp width; locale and recording data persisted across reinstall/restart.
- Latest full `testDebugUnitTest lintDebug assembleDebug`: pass, `BUILD SUCCESSFUL in 6m 7s`.
- Latest `:app:assembleRelease`: pass, `BUILD SUCCESSFUL in 1m 55s`.
- `git diff --check`: pass; only expected LF-to-CRLF working-copy warnings were emitted.
- Physical automatic model install: pass; download progressed from 8% to Ready, all package hashes verified, and Settings reported the Sherpa transcription model as ready.
- Physical end-to-end Offline analysis retry: pass; the previously stuck job completed and populated Transcript and Insights.
- Physical OPPO full-analysis rerun: pass in 41.4 seconds for the retained 69-second recording; 3 speech turns were aligned to 3 transcript segments with Speaker 1 labels and no text placed in silent gaps.
- Physical short-transcript insight guard: pass; output was `Bản ghi ngắn` with an evidence-backed summary and no fabricated structured sections.
- Physical insight edit persistence: pass; saving from the edit dialog created a USER_EDITED insight revision.
- Latest `testDebugUnitTest lintDebug assembleDebug`: pass in 6m 18s.
- Latest `:app:assembleRelease`: pass in 1m 52s.
- Selected-model and hallucination-filter module gates: pass in 98.6s and 106.6s.
- Latest full `testDebugUnitTest lintDebug assembleDebug`: pass in 1m 21s after the complete run produced fresh APK/lint artifacts.
- Physical Whisper Base install and SHA verification: pass; Settings reported 153.2 MB and Ready.
- Physical model switching: pass; Tiny/Base `Use this model` state persisted across reinstall and the queued worker used the selected model.
- Physical cross-segment hallucination filtering: pass; the repeated Base output was collapsed before diarization fusion.
- Final post-filter `testDebugUnitTest lintDebug assembleDebug`: pass in 52s, 1020 tasks.
- Final post-filter `:app:assembleRelease`: pass in 52s, 707 tasks.

## Next

1. Supply approved representative 30-second/5-minute EN/VI clips to complete the performance benchmark matrix.
