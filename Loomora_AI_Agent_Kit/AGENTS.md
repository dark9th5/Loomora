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
