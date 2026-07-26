# Changelog

All notable changes to Loomora are recorded here.

## [M13-M17] - 2026-07-26
### Added
- Implemented Loomora responsive Marketing Website (`web/index.html`).
- Completed UI/UX polish audit, Material 3 spacing, and accessible micro-animations (M15).
- Completed performance profiling, cold start optimization, and memory efficiency checks for long recording sessions (M16).
- Generated `FINAL_AUDIT_REPORT.md` (M17) with formal **GO RECOMMENDATION** for commercial beta release.

## [M12] - 2026-07-25
### Added
- Successfully generated production release candidate APK (`app-release-unsigned.apk`).
- Configured R8 / ProGuard obfuscation rules in `:app` for Room Database, Media3 ExoPlayer, and Hilt.
- Published `RELEASE_REPORT.md` documenting store readiness and build verification evidence.

## [M11] - 2026-07-25
### Added
- Completed test traceability matrix and published `TEST_REPORT.md` verifying P0/P1 scenarios across all 17 modules with 100% test pass rate.

## [M10] - 2026-07-25
### Added
- Completed security hardening audit for exported manifest components (`AudioRecorderService` `exported="false"`).
- Configured `network_security_config.xml` disallowing cleartext HTTP traffic and enforcing strict HTTPS.
- Added `android:allowBackup="false"` to preserve recording data privacy on device.
- Implemented `SecurityPrivacyTest` in `:core:common` validating path traversal protection.

## [M9] - 2026-07-25
### Added
- Completed dual-language localization audit for English (`values/strings.xml`) and Vietnamese (`values-vi/strings.xml`).
- Verified 100% PASS ratings across screen-by-screen accessibility audit for TalkBack descriptions, 48dp minimum touch target boundaries, and non-color state indicators.
- Created `AccessibilityAuditTest` in `:core:designsystem`.

## [M8] - 2026-07-25
### Added
- Implemented capability-based entitlement model (`Capability`, `EntitlementStatus`, `EntitlementManager`) in `:core:model` and `:core:datastore`.
- Guaranteed 100% Free & Unlimited local recording and playback access.
- Implemented `SubscriptionScreen` & `SubscriptionViewModel` in `:feature:subscription` with visible trial counters, 100% free local guarantee, license activation input, website handoff, and non-blocking exit path.

## [M7] - 2026-07-25
### Added
- Implemented provider-neutral AI data models (`TranscriptSegment`, `AiInsights`, `ActionItem`, `Chapter`, `AiJobStatus`) in `:core:model`.
- Implemented `AiTranscriptionProvider`, `AiInsightsProvider`, `DefaultAiProvider`, and `AiPipelineEngine` in `:core:network` with explicit consent enforcement and transcript failure isolation.
- Added explicit Cloud AI Data Disclosure modal and AI status rendering in `RecordingDetailScreen`.

## [M6] - 2026-07-25
### Added
- Implemented non-destructive edit model (`EditRecipe`, `EditOperation` for Trim, Delete Range, Split) in `:core:model`.
- Implemented `AudioEditExporter` in `:core:audio` for non-destructive export of edited audio files to `_edited.m4a` without modifying original files.
- Implemented `EditorScreen` & `EditorViewModel` in `:feature:editor` with full Undo/Redo stack, timeline waveform representation, and accessible numerical start/end range inputs.
- Implemented Speech Clarity enhancement toggle and export progress workflow.

## [M5] - 2026-07-25
### Added
- Implemented Media3 ExoPlayer playback engine (`AudioPlayerEngine`) in `:core:audio`.
- Implemented full playback controls component (`PlaybackControls`) in `:core:designsystem` with play/pause, seek slider, ±10s skip, and speed selection (0.5x - 2.0x).
- Implemented `RecordingDetailScreen` & `RecordingDetailViewModel` in `:feature:recordingdetail` with in-place title editing, technical metadata card, and tap-to-seek marker list.
- Implemented `MiniPlayer` floating component in `:core:designsystem` for persistent shell audio controls.
- Added missing/corrupt audio file error handling (`ErrorState`).

## [M4] - 2026-07-25
### Added
- Implemented real microphone audio recording pipeline in `:core:audio` with AAC/M4A MediaRecorder (44.1kHz, 128kbps stereo).
- Implemented `AudioRecorderService` foreground service (`foregroundServiceType="microphone"`) with persistent notification and pause/resume/stop actions.
- Implemented authoritative `RecorderState` machine and real peak/RMS amplitude sampling (`maxAmplitude`).
- Implemented point-of-use microphone permission request with rationale UI (`RECORD_AUDIO`).
- Implemented live `AudioWaveform` visualizer, authoritative timestamp timer, and marker persistence in `RecorderScreen`.
- Finalized audio recordings are saved directly to Room database (`RecordingEntity`) and immediately accessible in Library playback.

## [M3] - 2026-07-25
### Added
- Implemented Room Entities (`RecordingEntity`, `AudioSegmentEntity`, `MarkerEntity`, `TagEntity`, `RecordingTagCrossRef`, `BackgroundJobEntity`, `EntitlementEntity`, `TrialUsageEntity`).
- Implemented Room DAOs (`RecordingDao`, `AudioSegmentDao`, `MarkerDao`, `TagDao`, `BackgroundJobDao`).
- Configured Room Database v1 with schema export (`1.json`).
- Implemented `RecordingRepository` & `RecordingRepositoryImpl` with Flow-based reactive queries and path traversal safe file deletion.
- Connected `LibraryViewModel` & `HomeScreen` to Room database with real empty states, search, sort, filter chips, and favorite toggle.
- Added Robolectric Room in-memory database test suite (`RecordingDaoTest.kt`).

## [M2] - 2026-07-25
### Added
- Created production Loomora Design System in `:core:designsystem`: Indigo/Teal seed color palette with semantic light & dark color schemes, `LoomoraTheme`, and `LocalLoomoraExtraColors`.
- Created reusable components: `LoomoraTopAppBar`, `PrimaryRecordButton`, `RecorderStatusPill`, `EmptyState`, `ErrorState`, `OfflineBanner`, `TrialUsageChip`, `ProBadge`, `SettingRow`, `RecordingListItem`.
- Created modular English (`values/strings.xml`) and Vietnamese (`values-vi/strings.xml`) string resources.
- Implemented multi-step `OnboardingScreen` with DataStore persistence.
- Implemented `HomeScreen`, `LibraryScreen`, `SettingsScreen`, and `SubscriptionScreen` shells with real empty states and zero fake data.
- Integrated `LoomoraBottomBar` navigation shell into single-activity `LoomoraNavHost`.
- Wired dynamic System/Light/Dark theme and English/Vietnamese language preferences to DataStore.

## [M1] - 2026-07-25
### Added
- Project foundation initialized with Gradle Kotlin DSL and `libs.versions.toml` version catalog.
- Scaffolded 17 Android modules (`:app`, `:core:*`, `:feature:*`).
- Configured Hilt Dependency Injection, Room Database foundation, and DataStore preferences.
- Added Jetpack Compose navigation shell and Material 3 design system tokens.
- Added English and Vietnamese string resources.
- Added GitHub Actions CI workflow.

## [M0] - 2026-07-25
### Added
- Initial repository audit and technical implementation plan.

## Unreleased

### Added
- Product and engineering specification kit.

### Changed
- None.

### Fixed
- None.

### Security
- None.
