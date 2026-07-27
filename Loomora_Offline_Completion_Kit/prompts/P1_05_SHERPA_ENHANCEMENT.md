# P1.5 — sherpa-onnx Speech Enhancement

```text
Implement task P1.5 only: integration sherpa-onnx tối thiểu cho speech enhancement thật.

Read official sherpa-onnx Android and speech enhancement documentation at implementation time.

Phase A — integration spike:
1. Chọn và pin một sherpa-onnx release/tag.
2. Quyết định integration: official prebuilt artifacts hoặc build/copy JNI libs theo official project.
3. Support ít nhất arm64-v8a; x86_64 cho emulator chỉ khi artifact chính thức phù hợp.
4. Record native size, ABI và license.
5. Tạo explicit close/release lifecycle.
6. Chạy một fixture through engine.

Phase B — product slice:
1. Decode source to required PCM without loading whole recording into RAM.
2. Tích hợp một model official: GTCRN hoặc DPDFNet sau benchmark.
3. Giữ original.
4. Export enhanced file thật.
5. Presets map vào parameters đã benchmark, không phải toggle UI giả.
6. Progress, cancel, typed errors.
7. Compare input/output metadata and playback.
8. Cleanup PCM/temp/native resources.
9. Device incompatible/model missing state.
10. Không thêm transcription trong task này.

Tests:
- model missing;
- checksum mismatch;
- engine init failure;
- cancellation;
- short clean speech;
- noisy fixture;
- output playable;
- original unchanged;
- repeated runs do not leak/crash.

Acceptance:
- output audio khác nội dung signal theo processing thật, không chỉ copy.
- manual A/B playback.
- memory/processing time được ghi trên reference device.
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
