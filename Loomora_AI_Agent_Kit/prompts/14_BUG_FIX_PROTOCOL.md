# 14 — Bug Fix Protocol

## Use when
Whenever a concrete bug is reported.

## Agent prompt

```text
Fix only the reported bug and its directly related root cause.

Process:
1. Reproduce or state why reproduction is unavailable.
2. Add a failing regression test when feasible.
3. Identify root cause, not just symptom.
4. Check nearby state/lifecycle/data effects.
5. Implement the smallest safe fix.
6. Run targeted tests, then relevant broader suite.
7. Verify no data migration or file compatibility regression.
8. Update KNOWN_ISSUES.md and CHANGELOG.md.
9. Report actual commands/results.

Do not redesign the screen, refactor unrelated modules or suppress the failure.
```

## Stop condition
Stop after the regression is verified.
