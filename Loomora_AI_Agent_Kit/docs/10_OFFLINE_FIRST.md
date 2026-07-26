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
