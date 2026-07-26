# Release, Performance and Observability

## Build variants

Recommended:
- `debug`
- `staging`
- `release`

Separate:
- application IDs where appropriate;
- backend endpoints;
- logging;
- analytics;
- signing;
- feature flags.

## Release build requirements

- R8/proguard verified.
- No debug endpoint/key.
- No verbose sensitive logs.
- App version and database schema documented.
- Signed artifact reproducible through CI or controlled machine.
- Mapping files retained.
- Privacy/data-safety declarations match implementation.
- APK/AAB installed and smoke-tested.

## CI

At minimum:
- wrapper validation;
- assemble/lint;
- unit tests;
- database migration tests;
- formatting/static analysis if adopted;
- release compile on protected branch;
- artifact retention.

Do not add tools solely for appearance. Every gate needs ownership.

## Observability

Collect:
- crash and ANR.
- recorder start failures by category.
- finalization failures.
- corrupt/recovery counts.
- processing job success/failure/duration.
- paywall viewed and activation outcome.
- performance metrics.

Do not collect audio/transcript content.

## Performance budgets

Set and measure:
- cold start.
- time from tap to recording confirmation.
- recorder CPU/battery.
- memory during long sessions.
- waveform rendering frame rate.
- library query latency.
- large transcript rendering.
- APK size.
- local model pack size separately.

## Rollout

- Internal testing.
- Closed testing on diverse devices.
- Staged production rollout.
- Monitor crashes and recorder failures.
- Ability to disable cloud features server-side without breaking local recording.
