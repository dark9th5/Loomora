# Definition of Done

## Feature-level

A feature is done only when:

### Product
- Scope matches the feature matrix.
- User flow is complete.
- No visible dead control.
- Free/Trial/Pro behavior is correct.

### UX
- Loading/content/empty/error/offline states.
- Back behavior.
- Permission rationale.
- Accessibility semantics.
- Light/dark.
- English/Vietnamese.
- Compact and large-screen review.

### Engineering
- Real implementation.
- Correct state ownership.
- Persistence/recovery considered.
- Typed errors.
- No TODO/fake production data.
- No unrelated refactor.
- Documentation updated.

### Testing
- Unit tests for rules/state.
- Integration/instrumentation where appropriate.
- Manual device test for hardware-dependent behavior.
- Regression scenarios recorded.

### Verification
- Debug build passes.
- Relevant tests pass.
- Release build passes or exact external blocker is documented.
- Agent reports actual commands and outputs.

## Milestone-level

- Every feature in milestone meets feature DoD.
- P0 regressions are zero.
- Known P1 issues have owner and plan.
- `PROJECT_STATUS.md` updated.
- `CURRENT_TASK.md` closed.
- `CHANGELOG.md` updated.
- Screenshots or UI review evidence produced.
- No critical privacy/security issue.
