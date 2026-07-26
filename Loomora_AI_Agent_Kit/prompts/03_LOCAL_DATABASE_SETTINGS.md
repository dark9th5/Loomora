# 03 — Local Data Foundation

## Use when
After design system.

## Agent prompt

```text
Implement milestone M3 only.

Read docs/09_DATA_MODEL.md, docs/10_OFFLINE_FIRST.md and docs/13_SECURITY_PRIVACY.md.

Implement:
- Room entities/DAOs for recordings, segments, markers, tags, job status and trial/entitlement shells needed now;
- repositories with Flow-based observation;
- DataStore settings;
- real Library empty/content/search/sort state backed by Room;
- favorite, tag and trash/restore;
- schema export and migration test infrastructure.

Production source sets must not insert sample recordings.
Preview/test fixtures remain isolated.
Deletion must not accidentally delete arbitrary paths.

Run database tests, unit tests, debug build and release compile.
Update docs and tracking.
```

## Stop condition
Stop before microphone recording.
