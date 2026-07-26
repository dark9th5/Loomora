# 04 — Real Recorder Vertical Slice

## Use when
Critical milestone after local data.

## Agent prompt

```text
Implement milestone M4 as one complete vertical slice.

Read docs/03_USER_FLOWS.md, docs/04_UX_UI_SPEC.md, docs/08_AUDIO_ENGINE.md, docs/09_DATA_MODEL.md and docs/14_TESTING_STRATEGY.md.

Implement real microphone recording:
- point-of-use permission and rationale;
- foreground microphone service and persistent notification;
- authoritative recorder state machine;
- AudioRecord-based real audio pipeline or a documented robust alternative;
- real amplitude/waveform data;
- pause, resume, marker and stop;
- segment checkpoint persistence;
- safe finalization;
- storage preflight and write-failure handling;
- recovery discovery;
- saved recording metadata in Room;
- saved file opens in the playback path;
- truthful behavior for interruption and process recreation.

Forbidden:
- fake waveform;
- UI-only timer;
- TODO recorder;
- empty service methods;
- a single isRecording boolean as the complete state model;
- claiming device verification without a device.

Add unit/service integration tests. Build debug/release.
Provide a device test checklist and report which cases were actually run.
```

## Stop condition
Stop when a real saved recording can be played. Do not add AI or advanced editor.
