# P2.3 — Offline Speaker Diarization and Transcript Fusion

```text
Implement task P2.3 only.

Use sherpa-onnx official diarization support:
- supported Pyannote segmentation model;
- supported 3D-Speaker INT8 or equivalent official embedding model;
- internal clustering/config documented by sherpa-onnx.

Requirements:
1. Diarization runs locally.
2. Output speaker turns with start/end timestamps and generic labels.
3. Align speaker turns with transcript segments deterministically.
4. Handle segment crossing speaker boundary by split/assignment rule documented and tested.
5. Overlapped speech represented as uncertain/multiple speaker where supported; do not silently claim precision.
6. Do not infer real names.
7. Optional speaker enrollment is separate:
   - explicit user sample/consent;
   - encrypted/local embedding;
   - confidence threshold;
   - user confirmation before replacing generic label.
8. Persist diarization revision, model version and clustering settings.
9. Cancellation/retry/idempotency.
10. UI lets user rename Speaker 1 manually without changing biometric identity model unless explicitly enrolled.
11. Resource cleanup and model unload.

Tests:
- one speaker;
- two speakers;
- short turns;
- overlap fixture if available;
- diarization no result;
- alignment boundaries;
- retry no duplicates;
- manual speaker rename;
- low-confidence enrollment remains unassigned.

Acceptance:
- real two-speaker fixture produces timeline, not hard-coded labels.
- click speaker turn seeks correct audio.
- limitations visible/documented.
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
