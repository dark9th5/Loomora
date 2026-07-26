# 02 — Design System and App Shell

## Use when
After foundation builds.

## Agent prompt

```text
Implement milestone M2 only.

Read docs/04_UX_UI_SPEC.md, docs/05_DESIGN_SYSTEM.md and docs/23_COPYWRITING_LOCALIZATION.md.

Create Loomora's production design system:
- semantic color tokens for light/dark;
- typography, spacing, shapes and component tokens;
- reusable components listed in the design system;
- onboarding flow;
- Home shell;
- Settings shell;
- bottom navigation;
- system/light/dark selection;
- English default and Vietnamese resources.

Requirements:
- no generic Material sample appearance;
- no fake recent recordings;
- empty states are real;
- all visible controls work or are omitted;
- 360dp, large width and 200% font scale considered;
- accessibility semantics and 48dp touch targets.

Add Compose tests/screenshot review artifacts supported by the repository.
Build and verify. Update status files.
```

## Stop condition
Stop before database/library/recorder implementation.
