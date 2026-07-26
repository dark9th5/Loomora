# Testing Strategy

## Test pyramid

### Unit tests
- state reducers;
- ViewModels;
- repositories with fakes;
- trial accounting;
- entitlement parsing;
- edit recipes;
- data mapping;
- recovery decisions;
- task/evidence validation.

### Database tests
- DAO behavior;
- transactions;
- migrations from every production schema.

### Instrumentation/Compose tests
- navigation;
- permission states;
- empty/error/content UI;
- accessibility semantics;
- theme/language;
- configuration recreation.

### Service/audio integration
- recorder state transitions;
- service command handling;
- segment finalization;
- notification actions;
- recovery metadata.

### Device/manual
- actual microphone/codec behavior;
- long recording;
- screen off;
- OEM battery management;
- Bluetooth/wired route;
- phone call;
- storage pressure;
- thermal/battery impact;
- playback correctness.

## Required P0 scenarios

1. Start → record real audio → stop → playable file.
2. Pause/resume → correct duration and no corrupt file.
3. UI process killed during active service → recording state remains truthful.
4. Storage becomes full → recover completed data and show error.
5. App killed during finalization → recover or clearly report.
6. Database migration preserves recordings.
7. Offline guest can record and play.
8. Failed premium job does not consume trial.
9. Expired Pro never hides local recordings.
10. Delete removes intended data only.

## UI golden/screenshot review

Use screenshot testing if stable in chosen stack. At minimum capture:
- Home empty/content.
- Recorder preparing/recording/paused/finalizing/error.
- Library empty/content/search.
- Detail with/without transcript.
- Editor.
- Paywall.
- Light/dark.
- English/Vietnamese.
- 360dp and large width.
- 100% and 200% font scale.

## Quality gates

- No flaky tests accepted without issue and owner.
- Never use arbitrary sleeps when idling/synchronization is possible.
- Never weaken assertions to pass.
- Test result evidence is attached to milestone report.
