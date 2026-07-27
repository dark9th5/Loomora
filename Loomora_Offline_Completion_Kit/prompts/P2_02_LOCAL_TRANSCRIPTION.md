# P2.2 — Local Offline Transcription

```text
Implement task P2.2 only: timestamped offline transcription bằng sherpa-onnx.

Model direction:
- Whisper multilingual ONNX through sherpa-onnx for non-streaming ASR.
- Chọn tier nhỏ trước để integration/test; model selection nằm trong manifest.
- Không dùng `.en` cho tiếng Việt.

Pipeline:
1. Validate/fingerprint source.
2. Decode/resample to required 16 kHz mono PCM.
3. VAD để bỏ silence hoặc chia speech segments, nhưng không làm mất timestamp.
4. Run ASR locally.
5. Normalize timestamped segments.
6. Persist transcript revision in Room.
7. Cleanup temp PCM.

Requirements:
- progress/cancel;
- typed model/device/file errors;
- no full-file PCM in memory;
- Vietnamese and mixed Vietnamese/English;
- preserve raw model text and normalized text if normalization applied;
- transcript segment IDs stable within revision;
- pipeline/model version stored;
- retry does not duplicate segments;
- no API/network processing;
- airplane mode manual test;
- UI states: not processed, model missing, queued, processing, partial if supported, complete, failed, cancelled;
- click transcript segment seeks audio.

Tests:
- silence;
- short Vietnamese fixture;
- mixed-language fixture;
- corrupt file;
- model missing;
- cancel;
- retry/idempotency;
- timestamp ordering and bounds;
- source changed invalidates/stales result.

Acceptance:
- real transcript from real fixture.
- no hard-coded/sample transcript production.
- record processing duration and memory observation.
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
