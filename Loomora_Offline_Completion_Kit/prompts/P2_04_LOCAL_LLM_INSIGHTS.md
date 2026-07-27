# P2.4 — Local LLM Meeting Insights with LiteRT-LM

```text
Implement task P2.4 only.

Use official LiteRT-LM Kotlin SDK. Pin exact stable version in version catalog. Use a `.litertlm` instruction model selected through manifest; start with a model small enough for integration/reference device and keep model choice configurable.

Required outputs:
- smart title;
- concise summary;
- key points;
- decisions;
- action items;
- open questions;
- topics/chapters;
- evidence segment IDs.

Requirements:
1. Engine initialize/generation off main thread.
2. Explicit close/use lifecycle.
3. Capability check and CPU/GPU/NPU fallback policy.
4. Hierarchical processing for long transcripts.
5. Timestamp-preserving chunks.
6. Strict JSON/schema parsing and validation.
7. Invalid/truncated output is failure/retryable parse error, not success.
8. Evidence IDs must exist in transcript.
9. `assignee` and `dueDate` null unless explicit.
10. Distinguish decisions, suggestions and questions.
11. Persist model version, prompt version, schema version and pipeline version.
12. Keep generated and user-edited revisions distinguishable.
13. No transcript/audio leaves device.
14. Model missing/incompatible UI.
15. Do not load ASR and LLM models simultaneously unless memory benchmark proves safe; unload ASR before LLM by default.
16. Chunk result checkpointing.
17. Output language follows user setting or chosen processing language.

Tests:
- valid structured output;
- invalid JSON;
- missing evidence;
- hallucinated segment ID;
- no action items;
- missing assignee/deadline;
- long transcript chunking/deduplication;
- cancellation;
- backend fallback;
- engine close after error;
- user edit preserved.

Acceptance:
- real local model generates structured result in airplane mode.
- no fake summary.
- report model size, load time, generation time and memory observation.
```

## Quy trình bắt buộc

Trước khi sửa:

1. Inspect source và git status.
2. Viết plan cụ thể vào `CURRENT_TASK.md`.
3. Liệt kê acceptance criteria và tests.
4. Không sửa ngoài phạm vi nếu không cần để build.

Sau khi sửa:

1. Chạy checks/tests/build liên quan.
2. Ghi kết quả thật.
3. Cập nhật tracking docs.
4. Báo file thay đổi, rủi ro và manual tests.
5. Dừng sau task này; không tự nhảy sang prompt kế tiếp.

## Cấm

- Fake/hard-coded success.
- Tắt test/lint.
- Xóa test để build.
- Che lỗi bằng empty state.
- Bịa kết quả command.
