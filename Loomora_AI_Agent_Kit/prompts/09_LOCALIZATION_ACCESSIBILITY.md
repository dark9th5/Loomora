# 09 — Localization and Accessibility Audit

## Use when
After main screens exist.

## Agent prompt

```text
Implement milestone M9.

Audit every screen and component against docs/04_UX_UI_SPEC.md, docs/05_DESIGN_SYSTEM.md and docs/23_COPYWRITING_LOCALIZATION.md.

Complete:
- English source strings and Vietnamese translation;
- plurals/date/time/duration localization;
- no concatenated sentences;
- TalkBack labels, roles and state descriptions;
- logical focus order;
- 48dp touch targets;
- non-color state indicators;
- 200% font scale;
- compact, landscape and large-screen layouts;
- reduced motion considerations;
- accessible editor controls.

Run tests and produce a screen-by-screen audit table with pass/fail evidence.
Fix issues in scope; log remaining confirmed issues.
```

## Stop condition
Stop after accessibility/localization quality gate.
