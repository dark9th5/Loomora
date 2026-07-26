# 05 — Library, Detail and Playback

## Use when
After recorder produces real files.

## Agent prompt

```text
Implement milestone M5 only.

Read docs/03_USER_FLOWS.md, docs/04_UX_UI_SPEC.md and docs/09_DATA_MODEL.md.

Implement:
- Media3-based playback architecture;
- Recording Detail overview/audio sections;
- play/pause, seek, ±10 seconds and speed;
- real duration and progress;
- waveform seek where data exists;
- markers;
- mini-player only if lifecycle is complete;
- missing/corrupt file error states;
- library search/filter/sort integration;
- state restoration after configuration/process recreation where practical.

Do not fabricate transcripts or insights.
Add tests for ViewModels, playback mapping and UI states.
Run build/test evidence and update tracking.
```

## Stop condition
Stop before editor work.
