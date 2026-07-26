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
