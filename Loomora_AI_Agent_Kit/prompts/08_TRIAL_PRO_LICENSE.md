# 08 — Trial, Pro and Activation

## Use when
After premium capabilities have real contracts.

## Agent prompt

```text
Implement milestone M8 only.

Read docs/02_SCOPE_FEATURE_MATRIX.md, docs/10_OFFLINE_FIRST.md, docs/12_MONETIZATION_LICENSE.md and docs/21_API_CONTRACTS.md.

Implement:
- capability-based entitlement model;
- visible successful-use trial counters;
- reserve/complete/release operation accounting;
- paywall that allows continuing Free;
- Buy Pro website handoff;
- activation UI;
- backend validation contract;
- signed cached entitlement;
- offline expiry/grace behavior;
- restore/refresh;
- errors that never hide local recordings.

Forbidden:
- plain isPro boolean as security;
- decrementing trial before success;
- blocking Home/Library when activation fails;
- storing raw license code insecurely.

Add tamper/error/unit/UI tests possible in the repository.
Build and update tracking.
```

## Stop condition
Stop before automated payment gateway unless separately approved.
