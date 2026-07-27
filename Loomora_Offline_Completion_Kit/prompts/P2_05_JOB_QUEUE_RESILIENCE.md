# P2.5 — Persistent, Idempotent Offline Processing Queue

```text
Implement task P2.5 only.

Use WorkManager for persistent work and Room as source of truth.

Requirements:
1. Unique job key:
   recording + source fingerprint + pipeline version + options.
2. Duplicate enqueue returns/observes same logical job.
3. Persist stage checkpoints:
   prepare, enhance, VAD, ASR, diarization, align, summarize chunks, synthesize, validate.
4. Retry starts from last valid checkpoint.
5. Temp outputs use atomic publication.
6. Cancel request is persisted; workers check cancellation frequently.
7. Long-running work uses correct foreground progress notification and service type requirements.
8. Typed retryable vs terminal errors.
9. Exponential backoff only for truly retryable resource/transient errors; model incompatible/OOM after safe fallback is terminal.
10. Process death/reboot preserves job.
11. Source file deletion/change cancels or invalidates job safely.
12. Model removal mid-job produces typed failure.
13. Retry does not consume trial again or duplicate result rows.
14. Cleanup policy for abandoned temp/checkpoints.
15. UI observes Room job, not only WorkInfo.
16. Work constraints must not require network.
17. Provide pause only if it is real checkpoint/cancel-resume semantics; do not fake pause.

Tests:
- duplicate enqueue;
- process recreation;
- retry from checkpoint;
- cancellation;
- source change;
- model missing;
- temp cleanup;
- complete transaction;
- worker run twice;
- no network constraint.

Acceptance:
- kill app during processing, reopen and observe correct persisted state.
- retry/cancel tested.
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
