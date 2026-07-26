# 11 — Test and Reliability Hardening

## Use when
After features are implemented.

## Agent prompt

```text
Execute M10/M11 hardening based on docs/14_TESTING_STRATEGY.md and docs/19_RISK_REGISTER.md.

Create a traceability matrix from P0/P1 scenarios to tests or manual procedures.
Run all executable tests.
Add missing high-value tests.
Do not chase line coverage at the expense of behavior.

Specifically validate:
- recorder state transitions;
- pause/resume/finalization;
- storage failure/recovery;
- database migrations;
- offline guest behavior;
- entitlement expiry;
- trial success/failure accounting;
- deletion boundaries;
- dark/light/locales and key accessibility paths.

Produce TEST_REPORT.md from the template with actual evidence.
Do not mark hardware cases passed unless run on hardware.
```

## Stop condition
Stop when blockers and residual risks are explicit.
