# P2.1 — Offline AI Foundation and Model Manager

```text
Implement task P2.1 only. Chưa làm full transcript/summary.

Requirements:
1. Loại hướng cloud/API khỏi production AI contracts.
2. Tạo `:core:offlineai` chỉ nếu boundary là thật; nếu giữ trong module hiện tại phải giải thích.
3. Tạo provider-neutral local interfaces:
   - LocalTranscriptionEngine
   - LocalDiarizationEngine
   - LocalMeetingInsightEngine
   - LocalSpeechEnhancementEngine
4. Implement model manifest/install state theo docs/05_MODEL_MANAGEMENT.md.
5. Import model bằng SAF, stream copy, SHA-256 verify, atomic publish.
6. Store model metadata/status bền vững.
7. Detect ABI/RAM/storage/backend compatibility.
8. Tạo engine lifecycle manager; không để ViewModel giữ native engine.
9. Pin exact sherpa-onnx và LiteRT-LM SDK versions sau official-doc spike.
10. Không dùng `latest.release`.
11. Tạo AnalysisJob schema/state foundation và Room migration tests.
12. Xóa/disable production fake AI success path trong `core:network`; network module không được dùng cho offline processing.
13. Processing must be demonstrably callable without network.
14. UI model-management tối thiểu: installed/missing/verifying/corrupt/incompatible/remove/import.
15. Ghi model license/source trong manifest và About/Model details.

Tests:
- import valid;
- invalid checksum;
- interrupted import;
- duplicate import;
- incompatible ABI;
- model file missing after READY;
- remove model keeps existing transcript/results;
- no production fake provider.

Acceptance:
- có thể import và verify ít nhất một small fixture/model artifact.
- app build với native dependencies.
- airplane-mode foundation smoke test.
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
