# Loomora AI Agent Master Context

This file combines the most important context for agents that cannot reliably read a folder.


---

<!-- FILE: AGENTS.md -->

# Loomora Agent Operating Contract

This file is mandatory for every AI coding agent working in this repository.

## 1. Mission

Build Loomora as a production-quality Android application, not a prototype.

Loomora records microphone conversations and meetings, stores and manages audio locally, supports editing and voice enhancement, and can produce transcripts, summaries, key points and actionable tasks. Core free functionality must work without login and without internet.

## 2. Non-negotiable product rules

- App name: **Loomora**
- Marketing line: **Smart Voice Recorder & AI Notes**
- Kotlin + Jetpack Compose.
- Local-first; no login wall.
- Free recording, playback and local management work offline.
- Default UI language is English; Vietnamese is fully supported.
- Light, dark and system themes.
- The app records through the microphone only in the initial product.
- Do not implement covert call recording.
- Never upload audio without an explicit user action and clear disclosure.
- Premium failure must never block access to the user's local recordings.
- Original audio is preserved; editing is non-destructive until export.
- Trial usage is consumed only after a successful premium result.
- AI output must link to transcript/audio evidence whenever possible.
- No feature may silently pretend to work.

## 3. Engineering rules

- Use Gradle Kotlin DSL and a version catalog.
- Resolve current stable dependency versions from official sources at implementation time; record choices in `docs/20_DECISIONS.md`.
- Prefer platform and Jetpack APIs over unnecessary dependencies.
- Keep dependency count intentional.
- UI follows unidirectional data flow.
- Compose functions render immutable state and emit events.
- No file, database, network or recorder I/O directly inside composables.
- A foreground service owns active microphone recording.
- A single authoritative recorder state machine owns recording lifecycle.
- Room is the source of truth for persistent app metadata.
- DataStore stores small preferences, never large domain data.
- Coroutines must use structured concurrency.
- Production code contains no `TODO()`, `NotImplementedError`, fake delays or fake repositories.
- Mock data is allowed only in previews and tests.
- Never catch `Throwable` broadly unless rethrowing cancellation and documenting the boundary.
- Do not hide errors behind empty states.
- Do not delete tests, disable lint or suppress warnings merely to get green output.
- Do not refactor unrelated working code during a feature task.
- Do not add speculative abstractions before a real second use case exists.
- Avoid both a giant `:app` module and excessive micro-modules.

## 4. Required workflow

For every task:

1. Read relevant source-of-truth docs.
2. Inspect current implementation.
3. Write/update plan in `CURRENT_TASK.md`.
4. Identify acceptance criteria and edge cases.
5. Implement the smallest complete vertical slice.
6. Add or update tests.
7. Run formatting/static checks supported by the repository.
8. Run unit tests.
9. Run debug build.
10. Run release build or explain the exact blocking environment issue.
11. Update project tracking files.
12. Report evidence honestly.

## 5. Definition of “done”

A task is not done because files exist. It is done only when:

- UX states are implemented.
- Real domain data flows through the feature.
- Success, loading, empty, permission-denied and error paths are handled.
- Process recreation has been considered.
- Accessibility labels and touch targets are present.
- Relevant tests pass.
- The app builds.
- No new critical lint/static-analysis error is introduced.
- Documentation and state tracking are updated.

Refer to `docs/18_DEFINITION_OF_DONE.md`.

## 6. Build evidence policy

Never claim a build or test passed unless the command was executed and its output confirmed success. Report:

```text
Command:
Result:
Duration:
Warnings:
Artifacts:
```

If the environment cannot run Android builds, state that explicitly and provide user-run commands. Do not fabricate results.

## 7. State and context discipline

After each milestone update:

- `PROJECT_STATUS.md`
- `CURRENT_TASK.md`
- `CHANGELOG.md`
- `KNOWN_ISSUES.md`
- `docs/20_DECISIONS.md` for durable decisions

Do not create competing planning files. Archive obsolete plans or mark them superseded.

## 8. Security and privacy

- Never store provider secrets in APK source, resources, BuildConfig or version control.
- Use Android Keystore for protected local secrets/keys.
- Use short-lived backend-issued credentials where cloud work is needed.
- Minimize analytics and never send raw audio/transcript as analytics.
- Redact sensitive data from logs.
- Keep recording notification clear and persistent.
- Explain consent responsibilities in onboarding and before first recording.

## 9. Stop conditions

Stop and ask for a decision only when a choice materially changes:
- user data compatibility;
- cost model;
- legal/privacy posture;
- public API contract;
- irreversible database schema;
- premium entitlement behavior.

For minor reversible UI/implementation choices, choose the documented default, record it and continue.


---

<!-- FILE: START_HERE.md -->

# Start Here

## Prompt đầu tiên để dán vào AI Agent

```text
You are taking ownership of an existing or new Android repository for Loomora.

Before writing any code, read these files in order:
1. AGENTS.md
2. docs/00_INDEX.md
3. docs/01_PRODUCT_VISION.md
4. docs/02_SCOPE_FEATURE_MATRIX.md
5. docs/03_USER_FLOWS.md
6. docs/04_UX_UI_SPEC.md
7. docs/05_DESIGN_SYSTEM.md
8. docs/06_ARCHITECTURE.md
9. docs/17_ROADMAP.md
10. docs/18_DEFINITION_OF_DONE.md
11. PROJECT_STATUS.md
12. CURRENT_TASK.md
13. KNOWN_ISSUES.md

Then inspect the repository and do not modify source code yet.

Produce:
- a concise repository audit;
- detected build environment and modules;
- gaps between the repository and Loomora specifications;
- risks and blockers;
- a milestone plan where each milestone is a complete vertical slice;
- exact commands you will use to verify each milestone.

Write the plan into CURRENT_TASK.md and update PROJECT_STATUS.md.
Do not implement anything until I explicitly approve the first milestone.
Do not invent successful build or test results.
```

## Sau khi agent lập kế hoạch

Dùng lần lượt các prompt trong `prompts/`. Không bỏ qua bước Foundation để nhảy thẳng vào Recorder.

## Quy tắc giao việc

Một prompt tốt chỉ giao **một mục tiêu có thể nghiệm thu**. Ví dụ tốt:

> Triển khai vertical slice Recorder từ permission đến file có thể phát lại.

Ví dụ xấu:

> Làm toàn bộ app Loomora đẹp và đầy đủ.

## Khi agent báo hoàn thành

Yêu cầu nó cung cấp:

- Danh sách file thay đổi.
- Kiến trúc/state flow đã triển khai.
- Lệnh build/test đã chạy.
- Kết quả thật của từng lệnh.
- Hạn chế còn lại.
- Test thủ công cần làm trên thiết bị thật.
- Ảnh chụp màn hình hoặc mô tả UI state nếu môi trường hỗ trợ.

Không chấp nhận câu “should compile”, “likely works”, hoặc “implementation is complete” nếu không có bằng chứng.


---

<!-- FILE: docs/01_PRODUCT_VISION.md -->

# Product Vision

## Product statement

Loomora helps people record important conversations, meetings, interviews, lectures and personal voice notes, then turn those recordings into understandable, actionable information while keeping the core experience local and reliable.

## Primary promise

> Press record with confidence, keep the original safely, and find what matters later.

## Target users

### Primary

- Professionals recording meetings.
- Students recording lectures and study notes.
- Interviewers and researchers.
- Freelancers and project teams.
- Users who need reliable personal voice notes.

### Secondary

- Sales and customer-support professionals.
- Content creators gathering spoken ideas.
- Small organizations needing an affordable meeting assistant.

## Jobs to be done

- Record a long conversation without losing data.
- Find a recording quickly.
- Replay and jump to an important moment.
- Remove irrelevant sections and improve speech clarity.
- Obtain transcript, summary, key points, decisions and tasks.
- Continue using core features without account or internet.
- Understand exactly when audio leaves the device.

## Product principles

1. **Reliability before novelty.**
2. **Local-first before cloud dependency.**
3. **The original recording is never silently destroyed.**
4. **AI assists; it does not rewrite reality.**
5. **Every premium limit is transparent.**
6. **The recording screen is calm, obvious and hard to misuse.**
7. **No login is required to access personal local recordings.**
8. **English first in resources; Vietnamese is fully supported.**
9. **Beautiful means coherent, readable and responsive—not decorative overload.**
10. **Commercial growth must not compromise user ownership of recordings.**

## Initial platform boundary

- Android microphone recording only.
- No phone-call or VoIP two-way recording in the initial product.
- No covert background recording.
- No team workspace in MVP.
- No desktop app in MVP.
- No mandatory cloud backup.

## Success criteria

- A new user starts a real recording within 30 seconds.
- The app survives interruption and recovers completed segments.
- Local recordings remain usable when logged out or offline.
- Users can understand Free, Trial and Pro without reading legal text.
- AI-derived tasks link back to evidence.
- The app looks intentional on compact and large Android phones.


---

<!-- FILE: docs/02_SCOPE_FEATURE_MATRIX.md -->

# Scope and Feature Matrix

## Plans

| Capability | Guest Free | Trial | Pro | Business later |
|---|---:|---:|---:|---:|
| Record through microphone | Yes | Yes | Yes | Yes |
| Pause/resume | Yes | Yes | Yes | Yes |
| Background recording with visible notification | Yes | Yes | Yes | Yes |
| Local playback and speed control | Yes | Yes | Yes | Yes |
| Search title/tag | Yes | Yes | Yes | Yes |
| Folders, favorites and trash | Yes | Yes | Yes | Yes |
| Basic trim | Yes | Yes | Yes | Yes |
| Basic speech clarity preset | Limited | Yes | Yes | Yes |
| Advanced denoise/enhance | No | Limited successful trials | Yes | Yes |
| Transcript | No or limited local preview | Limited successful trials | Quota-based | Organization policy |
| Smart title and summary | No | Limited successful trials | Quota-based | Yes |
| Key points, decisions and tasks | No | Limited successful trials | Quota-based | Yes |
| Speaker labeling | No | Optional trial | Yes where supported | Yes |
| Export audio | Yes | Yes | Yes | Yes |
| Export transcript/notes | No | Limited | Yes | Yes |
| Cloud sync | No | Optional | Optional | Policy-controlled |
| Offline access to local files | Yes | Yes | Yes | Yes |
| Account required | No | No for core; optional for trial | Activation/account | Yes |

## Recommended trial policy

Default product decision until superseded:

- Three successful Smart Insights runs.
- Three successful Advanced Enhance exports.
- Failed/cancelled operations do not consume a trial.
- Trial counters are visible before execution.
- Cloud-powered trials require internet.
- Free local features continue after trials are exhausted.
- Reinstallation-abuse prevention is best effort in early MVP; do not collect invasive device identifiers.

## MVP must-have

- Onboarding and consent notice.
- Recorder with real audio.
- Safe segment persistence and finalization.
- Library.
- Playback.
- Rename, favorite, tag, delete/trash.
- Basic trim.
- Settings, themes and language.
- Free offline behavior.
- Premium preview/paywall.
- Activation entry point.
- Privacy and data deletion controls.
- Crash reporting without sensitive payloads.

## Post-MVP

- Live transcript.
- High-quality diarization.
- Ask questions about a recording.
- Calendar/task integrations.
- Cloud sync.
- Business workspace.
- On-device transcription pack.
- Desktop/web companion.


---

<!-- FILE: docs/03_USER_FLOWS.md -->

# User Flows

## 1. First launch

```text
Launch
→ language follows app default (English) or user selects Vietnamese
→ concise value proposition
→ privacy/local-first explanation
→ consent reminder
→ Continue
→ Home without login
```

Do not request microphone permission during onboarding. Request it when the user taps Record.

## 2. Start recording

```text
Home → Record
→ preflight checks
→ microphone permission if needed
→ foreground service starts
→ recording state becomes active
→ waveform/timer use real recorder data
→ user can add marker, pause, resume or stop
```

Preflight:
- permission;
- available storage;
- audio input availability;
- no conflicting finalization job;
- output directory writable.

## 3. Stop and save

```text
Stop tapped
→ confirmation only when accidental-stop risk is high
→ recorder state Finalizing
→ close encoder/muxer safely
→ validate playable output
→ persist metadata
→ create waveform summary
→ open Recording Detail
```

Never navigate away while finalization is unresolved without a visible recoverable status.

## 4. Interrupted recording

Possible interruption:
- incoming call/audio focus loss;
- permission revoked;
- Bluetooth route change;
- service killed;
- storage failure;
- app process death.

Expected behavior:
- preserve completed segments;
- show clear state;
- attempt safe continuation only when technically valid;
- create a recoverable recording entry;
- never claim recording continued if it did not.

## 5. Playback

```text
Library → recording
→ Detail Overview
→ Play
→ persistent mini-player where appropriate
→ seek, speed, ±10 seconds
→ transcript follows playback when available
```

## 6. Basic edit

```text
Detail → Edit
→ load waveform/proxy
→ select range
→ preview
→ save edit recipe
→ export to new file when requested
```

Original remains unchanged.

## 7. Smart Insights trial

```text
Detail → Smart Insights
→ show data processing disclosure + remaining trials
→ user explicitly continues
→ upload/processing or on-device job
→ processing status
→ result: title, summary, key points, decisions, tasks
→ each item links to evidence where available
→ consume trial only after successful usable result
```

## 8. Upgrade/activation

```text
Premium feature → Paywall
→ compare Free and Pro
→ Buy on website / Contact sales
→ user receives account or license
→ Activate in app
→ backend validates
→ signed entitlement cached locally
→ Pro works offline for validity window
```

## 9. Offline use

- Home, recorder, library, playback, local editing and settings remain available.
- Network actions show “Requires internet” without trapping the user.
- Pending jobs are explicit; no fake progress.
- Expired cached entitlement enters a grace/explanation state, never hides local data.


---

<!-- FILE: docs/04_UX_UI_SPEC.md -->

# UX and Screen Specification

## Navigation model

Primary bottom navigation:

- **Home**
- **Library**
- **Tasks** (visible when Smart Insights exists; may be introduced after MVP)
- **Settings**

A prominent Record action is available from Home and Library. Do not overload bottom navigation with the editor, paywall or profile.

## Global UX states

Every data screen defines:

- Loading.
- Content.
- Empty.
- Recoverable error.
- Blocking error.
- Offline variant when relevant.
- Permission-denied variant when relevant.

## Home

### Purpose
Start recording immediately and surface recent value.

### Structure
- Compact top bar with Loomora wordmark and settings/avatar entry.
- Primary “New recording” action.
- Optional recording mode chips: Meeting, Interview, Lecture, Voice note.
- Recent recordings section.
- Unfinished/recoverable recordings card when present.
- Trial/Pro usage shown subtly, never as a dominating ad.

### Empty state
A calm illustration/icon, one sentence, and Record CTA. No fake recent items.

## Recorder

### Visual hierarchy
1. Recording status and title.
2. Timer.
3. Real waveform/level visualization.
4. Live transcript region only if truly available.
5. Marker and secondary controls.
6. Large pause/resume and stop controls.

### States
- Preparing.
- Recording.
- Paused.
- Finalizing.
- Saved.
- Recoverable failure.
- Fatal failure.

### Safety
- Stop requires deliberate input.
- Back gesture during recording does not silently stop.
- Screen remains readable in bright and dark conditions.
- Notification controls mirror valid actions.
- Timer comes from recorder timestamps, not a standalone UI timer.

## Library

- Search.
- Filter by date, duration, favorite, tag and processing state.
- Sort by newest, oldest, title and duration.
- List/card density adapts to device width.
- Each item shows title, date, duration, status and optional tags.
- Swipe actions must have undo.
- Multi-select is introduced only when implemented completely.

## Recording detail

Tabs or sections:
- Overview.
- Transcript.
- Audio.
- Tasks.

Overview:
- editable title;
- date/duration/source;
- summary if available;
- key points and decisions;
- markers;
- processing/error status.

Audio:
- waveform;
- playback controls;
- speed;
- marker list;
- edit and enhance actions.

Transcript:
- speaker/time;
- search;
- edit;
- tap to seek;
- clear distinction between provisional and final text.

Tasks:
- checkbox;
- title;
- assignee and due date only when supported by evidence;
- evidence link.

## Editor

- Non-destructive timeline.
- Handles with accessible alternatives to drag gestures.
- Zoom.
- Play selection.
- Trim, split, delete range.
- Undo/redo.
- Before/after preview for enhancement.
- Explicit “Save edits” and “Export copy”.
- Never overwrite original without a separate, explicit action.

## Paywall

- Explain concrete benefits.
- Show remaining trial first.
- Make Free continuation obvious.
- Avoid false urgency.
- Show internet requirement for cloud features.
- Provide Restore/Activate and Contact support.
- Do not block the back button.

## Settings

Sections:
- Appearance.
- Language.
- Recording quality.
- Storage.
- Playback.
- Smart features and privacy.
- Trial/Pro/activation.
- Export defaults.
- About, privacy, terms and delete data.

## Responsive requirements

- Compact portrait: primary target.
- Landscape recorder: usable, not stretched.
- Large phone/foldable: center content with max width and use extra space for detail.
- Font scaling up to 200% must preserve access to controls.
- Avoid hard-coded heights for text containers.


---

<!-- FILE: docs/05_DESIGN_SYSTEM.md -->

# Loomora Design System

## Design direction

Calm, trustworthy and focused. Audio is the hero; the UI should not resemble a generic admin dashboard or neon AI template.

## Brand attributes

- Clear.
- Quietly premium.
- Private.
- Dependable.
- Human.
- Modern without novelty effects.

## Color strategy

Use semantic tokens, not raw colors in feature code.

Recommended seed direction:
- Primary: deep indigo/blue-violet that remains legible in dark mode.
- Secondary: cool teal for audio/processing accents.
- Neutral surfaces: slightly warm/blue-neutral rather than pure gray.
- Recording: semantic red reserved for active recording/destructive confirmation.
- Success: green only for actual success.
- Warning: amber.
- Error: accessible red.

Required token families:
- `primary`, `onPrimary`, `primaryContainer`, `onPrimaryContainer`
- `surface`, `surfaceContainer*`, `onSurface`, `onSurfaceVariant`
- `outline`, `outlineVariant`
- `recording`, `onRecording`
- `success`, `warning`, `error`
- `waveformActive`, `waveformInactive`

Do not hard-code alpha tricks repeatedly. Create tokens.

## Typography

Use a highly readable system-compatible sans serif. Prefer Android system/Roboto unless a licensed bundled brand font is deliberately selected.

Roles:
- Display: recording timer only.
- Headline: page titles.
- Title: recording titles and cards.
- Body: transcript and descriptions.
- Label: buttons, chips and metadata.

Rules:
- Transcript body prioritizes readability over density.
- Timer uses tabular digits if available.
- Minimum important text contrast meets accessibility requirements.
- Avoid all-caps except very small status badges.

## Spacing

Base grid: 4dp.

Common values:
- 4, 8, 12, 16, 20, 24, 32, 40.
- Screen horizontal padding: 16dp compact, 24dp medium, max-width layouts on large screens.
- Card inner padding: 16dp.
- Section gap: 24dp.
- Minimum touch target: 48dp.

## Shape

- Small controls/chips: 10–12dp.
- Cards/sheets: 16–24dp depending on hierarchy.
- Primary record button: circular.
- Avoid making every container a rounded card.

## Elevation

Prefer tonal surfaces and borders. Use elevation sparingly:
- floating recorder button;
- modal/sheet;
- active mini-player.

## Components

Required reusable components:
- `LoomoraTopBar`
- `PrimaryRecordButton`
- `RecorderStatusPill`
- `AudioWaveform`
- `RecordingListItem`
- `PlaybackControls`
- `EmptyState`
- `ErrorState`
- `OfflineBanner`
- `ProcessingCard`
- `TrialUsageChip`
- `ProBadge`
- `ConfirmActionSheet`
- `PermissionRationale`
- `SettingRow`

## Motion

- Motion communicates state, not decoration.
- Recorder transition must be immediate and reassuring.
- Waveform motion must reflect real amplitude.
- Respect reduced-motion settings where available.
- Avoid infinite shimmer after a terminal error.
- Haptics for record start, marker, pause/resume and stop confirmation.

## UI review questions

- Is the main action obvious in under two seconds?
- Does the screen still work with no data?
- Are all visible controls functional?
- Can a user distinguish recording, paused and finalizing without color alone?
- Does dark mode feel designed rather than inverted?
- Does the screen fit at 360dp and 200% font scale?


---

<!-- FILE: docs/06_ARCHITECTURE.md -->

# Architecture

## Architectural style

- Single-activity Compose application.
- Unidirectional data flow.
- Feature-oriented modularization.
- Repository boundaries for persistent data and external providers.
- Foreground service for active recording.
- WorkManager for deferrable/retriable work.
- Local-first source of truth.

## Layers

### UI
- Compose screens and reusable components.
- Immutable `UiState`.
- User events/actions.
- ViewModel state orchestration.
- No direct I/O.

### Domain
- Domain models and rules.
- Use cases only where they express meaningful reusable business behavior.
- Do not create a use case class for every repository method mechanically.

### Data
- Room DAOs.
- DataStore preferences.
- File storage.
- Recorder gateway.
- Playback gateway.
- AI/transcription providers.
- Entitlement provider.
- Network API client.

## Recommended major boundaries

```text
UI → ViewModel → domain operation/repository
Repository → local source + optional remote source
Foreground recorder service → recorder engine → files + recorder state
WorkManager → upload/transcription/analysis/export jobs
```

## State ownership

- Recording lifecycle: recorder service/state store.
- Persistent recording metadata: Room.
- Playback state: playback service/controller.
- Screen transient state: ViewModel.
- Settings: DataStore exposed as Flow.
- Entitlement: entitlement repository backed by signed local cache and backend validation.
- Background processing status: Room/job records, not only WorkManager UI observation.

## Concurrency

- Never use `GlobalScope`.
- Long-lived service scopes have explicit SupervisorJob and lifecycle shutdown.
- Cancellation must propagate.
- File finalization occurs on an appropriate dispatcher and is serialized.
- Room transactions protect multi-table state changes.
- Avoid multiple collectors starting duplicate recorder/player work.

## Error model

Use typed domain errors where UI behavior differs:

- Permission denied.
- Microphone unavailable.
- Storage unavailable/full.
- Encoder initialization failed.
- Recording interrupted.
- Corrupt/recoverable file.
- Network unavailable.
- Authentication/activation failed.
- Quota exhausted.
- Provider processing failed.
- Privacy consent required.

Do not expose raw exception strings to users.

## Dependency injection

Use Hilt or the selected official DI approach consistently. Do not use a service locator hidden in singleton objects.

## Versioning

At bootstrap, resolve current stable versions from official documentation and record them in `docs/20_DECISIONS.md`. Avoid alpha/beta dependencies unless a required capability has no stable alternative and risk is accepted.


---

<!-- FILE: docs/08_AUDIO_ENGINE.md -->

# Audio Engine Specification

## Goals

- Reliable microphone recording.
- Real pause/resume.
- Safe recovery.
- Real waveform/level data.
- Long-session stability.
- Foundation for transcription and enhancement.
- Playable final output.

## Recommended pipeline

```text
AudioRecord
→ PCM frames
→ level/waveform sampling
→ optional lightweight real-time preprocessing
→ AAC encoder through MediaCodec
→ MediaMuxer M4A segment
→ segment checkpoint metadata
→ final logical recording
```

Alternative implementations may be accepted if they satisfy all acceptance criteria and are recorded as an ADR.

## Recorder state machine

```text
Idle
→ Preparing
→ Recording
↔ Paused
→ Stopping
→ Finalizing
→ Completed

Any active state
→ RecoverableError
or
→ FatalError
```

Invalid transitions must be rejected. A single boolean `isRecording` is insufficient.

## Ownership

- Foreground service owns the active recording.
- UI sends commands through a stable interface.
- Service publishes authoritative state.
- Notification reflects valid commands.
- ViewModel never owns the microphone directly.

## File strategy

- Store files in app-controlled storage by default.
- Use stable recording IDs, not titles, in filenames.
- Persist each completed segment and checksum/size metadata.
- Keep temporary and finalized states distinguishable.
- Finalize atomically where possible.
- Never overwrite the original during editing.
- Provide explicit export to user-selected location.

## Pause/resume

Pause may:
- pause codec/muxing where robustly supported; or
- close a segment and begin another on resume.

The chosen strategy must produce gap-correct playback and be device-tested.

## Waveform

- Derived from real PCM amplitude/RMS samples.
- Downsample for display and persist a compact waveform cache.
- UI waveform must not fabricate motion.
- Handle silence without displaying a failure.

## Foreground service

- Show persistent notification during recording.
- Expose pause/resume/stop actions.
- Start only from valid user-initiated context.
- Handle notification permission behavior appropriately by Android version.
- Document foreground service type and manifest requirements.

## Interruptions

Define behavior for:
- audio focus changes;
- phone call;
- other recorder app;
- route change;
- wired/Bluetooth disconnect;
- permission revoked;
- low battery;
- thermal pressure;
- storage full;
- process death.

The app must show what actually happened.

## Recovery

On launch:
1. inspect unfinished metadata and temp segments;
2. validate files;
3. offer or automatically construct a recoverable recording;
4. never delete recoverable content silently;
5. mark irrecoverable corruption clearly.

## Audio quality presets

- Standard: balanced size/quality.
- High: higher bitrate.
- Storage saver: lower bitrate.
- Custom only if supported and tested.

Record actual codec, sample rate, channel count and bitrate in metadata.

## Enhancement

Basic:
- conservative high-pass/noise reduction where available;
- loudness normalization;
- speech clarity EQ.

Advanced:
- offline or backend processing behind provider interface.

Never label unverified processing as “studio quality”. Provide before/after preview and keep original.

## Required recorder tests

- Permission denied/don't ask again.
- 1-minute, 30-minute and multi-hour sessions.
- Pause/resume repeatedly.
- Screen off.
- App UI process killed while service remains.
- Service killed/restarted behavior.
- Storage nearly full/full.
- Route changes.
- Incoming call.
- Rapid start/stop.
- Double-tap commands.
- Final file is playable and duration is reasonable.


---

<!-- FILE: docs/10_OFFLINE_FIRST.md -->

# Offline-First Behavior

## Core principle

No internet and no login must still allow the user to:

- open the app;
- record;
- pause/resume/stop;
- view the library;
- play local audio;
- rename, tag, favorite and trash;
- perform supported local edits;
- change settings;
- export local audio.

## Capability state model

A feature is one of:
- Local available.
- Cloud available.
- Queued waiting for network.
- Requires activation.
- Trial available.
- Trial exhausted.
- Temporarily unavailable.
- Unsupported on device.

Do not reduce these states to a generic disabled button.

## Network behavior

- Detect network as a hint, not proof that backend works.
- Timeouts and server errors have separate messages.
- Uploads are resumable/idempotent.
- User can cancel queued cloud work.
- Pending jobs survive process death.
- Do not auto-upload all recordings.

## Identity

Guest is a first-class state, not an error.

Account/login may be used for:
- Pro purchase recovery;
- license activation;
- multi-device sync;
- cloud quota.

Local recordings are not hidden after logout.

## Conflict policy

For MVP without sync, no conflict problem exists. When sync arrives:
- local database remains immediate source;
- immutable original audio gets stable content ID;
- metadata uses explicit conflict resolution;
- never silently discard edits.

## Offline entitlement

A successful activation yields a backend-signed entitlement cached locally.

Recommended behavior:
- Pro available offline until token expiry.
- Optional grace period for temporary connectivity problems.
- After expiry, premium actions are disabled with explanation.
- Existing premium-generated content remains readable.
- Local recordings remain fully accessible.
- Clock manipulation resistance is best effort; do not punish legitimate users aggressively.

## Trial

- Consume only on successful result.
- Reserve an operation before execution to avoid parallel abuse.
- Release reservation on confirmed failure.
- Make remaining uses visible.
- Do not require login for core Free use.


---

<!-- FILE: docs/17_ROADMAP.md -->

# Delivery Roadmap

Each milestone must compile independently and end with a usable vertical slice.

## M0 — Audit and decisions

- Inspect repository/toolchain.
- Resolve stable versions.
- Record ADRs.
- Establish build commands.
- No feature code.

## M1 — Foundation

- Gradle/project setup.
- App variants.
- Hilt/DI.
- Room/DataStore foundations.
- Navigation shell.
- CI baseline.
- Build verified.

## M2 — Design system and app shell

- Theme/system/light/dark.
- Typography, spacing, components.
- Onboarding.
- Home and settings shells with real state.
- English/Vietnamese resources.
- Screenshot review.

## M3 — Local data and library foundation

- Recording metadata schema.
- DAOs/repository.
- Library empty/content/search.
- Trash/favorite/tag basics.
- Migration tests.

## M4 — Recorder vertical slice

- Permission.
- Foreground service.
- Real AudioRecord pipeline.
- Real waveform.
- Pause/resume/stop.
- Segment persistence.
- Recovery.
- Saved recording appears in library and plays.

## M5 — Playback and detail

- Media3 playback.
- Detail screen.
- Seek/speed/markers.
- Audio session persistence.
- Error handling.

## M6 — Editor

- Non-destructive trim/split/delete recipe.
- Preview.
- Export copy.
- Basic clarity.
- Original preservation.

## M7 — Transcript and insights

- Provider contracts.
- Backend job contracts.
- Explicit consent/upload.
- Transcript UI.
- Structured insights and evidence.
- Retry/cancel.

## M8 — Trial and Pro

- Trial state machine.
- Paywall.
- Website handoff.
- Activation.
- Signed offline entitlement.
- Quota/errors.

## M9 — Accessibility/localization

- English/Vietnamese completeness.
- Font scale.
- TalkBack.
- Touch targets.
- RTL readiness review even if not shipped.
- Large-screen review.

## M10 — Hardening

- P0/P1 tests.
- Long recordings.
- Process death/recovery.
- Security/privacy review.
- Performance profiling.
- OEM device matrix.

## M11 — Release candidate

- Release signing.
- R8.
- Store declarations.
- Closed test.
- Staged rollout plan.
- Support runbook.

## M12 — Marketing website

Can run in parallel after stable screenshots:
- Loomora rebrand.
- Pricing.
- Buy/activate.
- Download.
- Blog.
- Vercel deployment.


---

<!-- FILE: docs/18_DEFINITION_OF_DONE.md -->

# Definition of Done

## Feature-level

A feature is done only when:

### Product
- Scope matches the feature matrix.
- User flow is complete.
- No visible dead control.
- Free/Trial/Pro behavior is correct.

### UX
- Loading/content/empty/error/offline states.
- Back behavior.
- Permission rationale.
- Accessibility semantics.
- Light/dark.
- English/Vietnamese.
- Compact and large-screen review.

### Engineering
- Real implementation.
- Correct state ownership.
- Persistence/recovery considered.
- Typed errors.
- No TODO/fake production data.
- No unrelated refactor.
- Documentation updated.

### Testing
- Unit tests for rules/state.
- Integration/instrumentation where appropriate.
- Manual device test for hardware-dependent behavior.
- Regression scenarios recorded.

### Verification
- Debug build passes.
- Relevant tests pass.
- Release build passes or exact external blocker is documented.
- Agent reports actual commands and outputs.

## Milestone-level

- Every feature in milestone meets feature DoD.
- P0 regressions are zero.
- Known P1 issues have owner and plan.
- `PROJECT_STATUS.md` updated.
- `CURRENT_TASK.md` closed.
- `CHANGELOG.md` updated.
- Screenshots or UI review evidence produced.
- No critical privacy/security issue.
