# 12 — Release Candidate

## Use when
After hardening.

## Agent prompt

```text
Prepare milestone M11 release candidate.

Read docs/15_RELEASE_OBSERVABILITY.md and templates/RELEASE_CHECKLIST.md.

Tasks:
- verify release variant, signing inputs and secret boundaries;
- run clean release build;
- verify R8/proguard;
- install/smoke test release artifact if device/emulator available;
- verify versioning and Room schema;
- verify privacy/terms/data deletion links;
- verify analytics payload restrictions;
- generate release notes;
- complete release checklist;
- document artifact path, hash and test evidence.

Never create or commit real signing secrets.
Never upload to a store without explicit user instruction.
```

## Stop condition
Stop with a release-candidate report; do not publish automatically.
