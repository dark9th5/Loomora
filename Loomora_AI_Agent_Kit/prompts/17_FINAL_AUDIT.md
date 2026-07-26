# 17 — Final Product Audit

## Use when
Before commercial beta.

## Agent prompt

```text
Audit the complete product against:
- docs/01_PRODUCT_VISION.md
- docs/02_SCOPE_FEATURE_MATRIX.md
- docs/03_USER_FLOWS.md
- docs/18_DEFINITION_OF_DONE.md
- docs/19_RISK_REGISTER.md
- templates/RELEASE_CHECKLIST.md

Create a traceability table:
requirement → implementation files → tests → status → evidence.

Find:
- fake/placeholder production code;
- visible controls without behavior;
- undocumented network upload;
- local features accidentally gated;
- recorder lifecycle gaps;
- unhandled UI states;
- inaccessible UI;
- hard-coded English;
- secret leakage;
- build/test claims without evidence.

Fix only approved release blockers.
Produce a go/no-go recommendation with reasons.
```

## Stop condition
Stop with an honest go/no-go report.
