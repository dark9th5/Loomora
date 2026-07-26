# Transcript and Smart Insights

## Product boundary

AI is a value layer, not a dependency for recording.

## Provider abstraction

Use interfaces for:
- transcription;
- diarization;
- summary/insights;
- audio enhancement.

Do not expose provider-specific DTOs to UI/domain models.

## Initial strategy

Hybrid:
- local recording and editing;
- explicit opt-in cloud processing for high-quality transcript/insights;
- later optional on-device packs for capable devices.

Provider keys live only on backend.

## Processing pipeline

```text
User requests Smart Insights
→ privacy disclosure and trial/quota check
→ create idempotent job
→ prepare/segment audio
→ upload through signed URL if cloud
→ transcription
→ optional diarization
→ normalize timestamped transcript
→ structured insight extraction
→ evidence validation
→ persist result
→ notify UI
```

## Structured result

At minimum:
- smart title;
- concise summary;
- detailed summary;
- key points;
- decisions;
- action tasks;
- open questions;
- chapters;
- tags;
- evidence references.

## Hallucination controls

- Assignee is null unless explicit.
- Due date is null unless explicit.
- Decisions are distinguished from suggestions.
- Every important item should cite transcript segments.
- Low-confidence items are labeled.
- User can edit/remove results.
- Prompt/model/provider version is stored for reproducibility.
- Do not present AI output as a legal or verbatim record.

## Long recordings

Use hierarchical processing:
- timestamp-preserving chunks;
- per-chunk summaries;
- global synthesis;
- deduplication;
- evidence mapping;
- coverage checks.

## Failure behavior

- Preserve transcript if insight generation fails.
- Preserve uploaded/job state for safe retry.
- Do not consume trial on provider failure.
- Present actionable error codes.
- Allow user to delete remote processing data.

## Live transcript

Post-MVP unless the recorder is already stable. Realtime text is provisional; run a final pass after recording.
