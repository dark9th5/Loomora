# 06 — Non-destructive Editor

## Use when
After stable playback.

## Agent prompt

```text
Implement milestone M6 only.

Read docs/04_UX_UI_SPEC.md, docs/08_AUDIO_ENGINE.md and docs/18_DEFINITION_OF_DONE.md.

Implement:
- non-destructive edit recipe;
- trim, split and delete-range operations;
- undo/redo;
- selection preview;
- zoomable waveform/timeline;
- accessible alternatives to drag handles;
- save edit recipe;
- export to a new output file;
- basic conservative speech clarity;
- before/after preview;
- cancel/retry/progress and storage errors.

Never overwrite the original automatically.
Use Media3 or another documented stable approach.
Test operation math and export output validity.
Run debug/release builds and update tracking.
```

## Stop condition
Stop before Smart Insights.
