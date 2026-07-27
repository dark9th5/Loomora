# 99 — Final Production Audit

```text
Perform a final audit. Do not add new features unless fixing a release blocker.

Audit categories:
1. Build/CI/signing.
2. Recorder state and service control.
3. Session/marker integrity.
4. Pause duration and save acknowledgement.
5. Recovery/corrupt files.
6. Library/file/storage operations.
7. Real waveform.
8. Real editor/export.
9. Real speech enhancement.
10. Offline model management.
11. Real transcription.
12. Real diarization.
13. Real local LLM insights.
14. Job retry/cancel/idempotency.
15. Offline license/trial.
16. Localization/accessibility.
17. Privacy/security/logging.
18. Performance/memory/thermal.
19. Website truthfulness.
20. Tests/manual device evidence.

Search production source for:
- TODO()
- NotImplementedError
- fake/dummy/sample production repositories
- hard-coded transcript/summary
- copyTo exporter pretending to edit
- `"active"` recording ID
- debug release signing
- `latest.release`
- API keys/endpoints for AI processing
- raw `file://` sharing
- hard-coded user-visible strings
- unmanaged CoroutineScope/ExoPlayer/native engine
- plain `isPro` authority
- catch(Throwable) cancellation bugs

Run:
- ./gradlew clean check
- ./gradlew testDebugUnitTest
- ./gradlew assembleDebug
- ./gradlew assembleRelease
- connected tests when environment supports
- airplane-mode device workflow

Create FINAL_AUDIT_REPORT.md with:
- PASS/FAIL per category;
- command evidence;
- real device matrix;
- known limitations;
- P0 blockers;
- P1 before public release;
- deferred items;
- exact release recommendation: NOT READY, CLOSED BETA or READY.
Do not soften failures.
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
