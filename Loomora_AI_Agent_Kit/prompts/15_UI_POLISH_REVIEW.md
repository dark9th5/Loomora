# 15 — UI Polish Review

## Use when
When functionality works but visual quality needs improvement.

## Agent prompt

```text
Perform a UI/UX audit without changing product behavior.

Read docs/04_UX_UI_SPEC.md and docs/05_DESIGN_SYSTEM.md.

Review each implemented screen for:
- hierarchy;
- spacing/token consistency;
- typography;
- contrast;
- light/dark;
- empty/error/offline states;
- responsive layout;
- animation/haptics;
- generic template appearance;
- duplicate components;
- visible dead controls.

First produce a prioritized audit with screenshot references if available.
Then fix P0/P1 visual/usability issues only.
Do not replace working business logic.
Run UI tests/build and document before/after.
```

## Stop condition
Stop after the agreed polish scope.
