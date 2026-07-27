# P2.7 — Release Hardening and Website Truthfulness

```text
Implement task P2.7 only.

App hardening:
1. Re-run all P0/P1/P2 gates.
2. Enable/test R8 only with rules for Room/Hilt/serialization/sherpa-onnx/LiteRT-LM as required by official docs.
3. Release signed path and unsigned CI compile both correct.
4. Check ABI splits/app size/model distribution.
5. No model/private key accidentally in base APK.
6. Privacy disclosure states processing is on device.
7. Model licenses/attributions included.
8. Test airplane mode after models installed.
9. Test reference device matrix and report unsupported devices.
10. No sensitive logs.
11. Crash-safe model cleanup and recording access.

Website:
1. Audit every claim against real app.
2. Remove/mark Coming Soon for incomplete:
   - editor;
   - denoise;
   - diarization;
   - transcript;
   - summary/actions;
   - activation.
3. State clearly:
   - processing happens locally;
   - model download/import size;
   - device requirements;
   - limitations;
   - no guaranteed perfect transcript;
   - speaker labels are probabilistic.
4. Do not advertise cloud sync/API if not implemented.
5. Pricing/license wording matches actual offline license behavior.
6. Do not claim immediate revocation for offline license.
7. Link privacy policy and model attribution.

Acceptance:
- release candidate build passes.
- website claims map to acceptance evidence.
- feature matrix contains Available / Beta / Coming Soon.
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
