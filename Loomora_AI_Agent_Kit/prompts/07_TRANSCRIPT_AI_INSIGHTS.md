# 07 — Transcript and Smart Insights

## Use when
After local audio workflow is stable.

## Agent prompt

```text
Implement milestone M7 only.

Read docs/11_AI_PIPELINE.md, docs/13_SECURITY_PRIVACY.md and docs/21_API_CONTRACTS.md.

First implement provider-neutral contracts and job state. Then implement only the backend/provider integration that is actually configured.

Requirements:
- explicit disclosure before upload;
- no provider key in APK;
- idempotent job;
- resumable/cancellable work where supported;
- timestamped transcript storage;
- structured title, summary, key points, decisions, tasks and chapters;
- evidence links;
- null unknown assignee/due date;
- retryable versus terminal errors;
- failed insight generation preserves transcript;
- no trial consumption yet unless M8 accounting is already implemented correctly.

UI must distinguish no result, processing, partial, complete and failed.
Do not simulate successful AI in production.
Build/test and report required external configuration honestly.
```

## Stop condition
Stop after one real configured provider path or a fully testable provider-neutral integration boundary.
