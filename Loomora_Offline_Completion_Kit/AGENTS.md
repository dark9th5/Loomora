# Loomora Offline Agent Contract

File này là hợp đồng bắt buộc cho mọi coding agent làm việc trên Loomora.

## 1. Sứ mệnh

Hoàn thiện Loomora thành ứng dụng Android ghi âm và phân tích cuộc họp có thể phát hành, không phải prototype.

Core workflow phải chạy local:

```text
Record → Save → Recover → Play → Edit → Enhance
       → Transcribe → Diarize → Summarize → Extract actions
```

## 2. Quy tắc sản phẩm không được thay đổi

- Kotlin + Jetpack Compose.
- Local-first và offline-first.
- Ghi âm, phát lại và quản lý file không cần đăng nhập.
- Không bí mật ghi âm cuộc gọi.
- Không upload audio hoặc transcript để xử lý AI.
- Không dùng API cho speech-to-text, diarization, summary hoặc action-item extraction.
- File ghi âm gốc không bị ghi đè.
- Editor là non-destructive cho đến lúc export.
- Kết quả AI phải gắn với timestamp/evidence khi có thể.
- Không tự gán tên người nói nếu người đó chưa được xác minh.
- Không tự tạo assignee hoặc deadline không xuất hiện rõ trong transcript.
- AI failure không được làm mất hoặc khóa recording.
- Free local recording không bao giờ bị khóa bởi license.
- Không tính trial khi job thất bại, bị hủy hoặc output không hợp lệ.

## 3. Stack mục tiêu

- `sherpa-onnx`: VAD, speech enhancement, offline ASR, speaker diarization/embedding.
- `LiteRT-LM`: local LLM inference cho summary và structured extraction.
- Media3 ExoPlayer: playback.
- Media3 Transformer/Composition: trim, ghép, export nếu API hiện hành đáp ứng.
- Room: source of truth cho metadata, transcript, jobs, results.
- DataStore: settings nhỏ.
- WorkManager: persistent/retriable processing.
- Android Keystore: bảo vệ local secrets/state cần thiết.
- Signed offline license: xác minh bằng public key trong app; private key không bao giờ nằm trong repo/APK.

Agent phải kiểm tra tài liệu chính thức và pin version cụ thể trong version catalog. Không dùng `latest.release`.

## 4. Quy tắc kỹ thuật

- Một state machine duy nhất sở hữu recording lifecycle.
- UI chỉ render immutable state và phát event.
- Composable không trực tiếp làm file/database/recorder/model I/O.
- ViewModel không điều khiển một đường khác với foreground service.
- Mọi lệnh start/pause/resume/stop phải đi qua một controller/service contract duy nhất.
- Room là nguồn sự thật cho recording, marker và background jobs.
- WorkManager không phải nguồn sự thật duy nhất cho job status.
- File output dùng temp path rồi atomic rename/move khi hoàn tất.
- Multi-table update dùng transaction.
- Long-lived scope dùng `SupervisorJob` và có lifecycle shutdown rõ.
- Native engines phải có `close/release`.
- Không log audio, transcript, license code hoặc dữ liệu nhạy cảm.
- Không catch `Throwable` bừa bãi; luôn bảo toàn coroutine cancellation.
- Không refactor code không liên quan trong một task.
- Không thêm module nếu chưa có boundary thực.
- Production không có `TODO()`, `NotImplementedError`, fake delay, fake repository hoặc fake provider success.
- Mock/fake chỉ dùng trong tests, previews hoặc sample riêng.

## 5. Chính sách AI offline

- Processing phải chạy được ở airplane mode sau khi model đã được cài.
- Model có manifest, version, SHA-256, size, license và minimum capability.
- Không bundle toàn bộ model lớn vào base APK nếu làm app không thể phân phối hợp lý.
- Phải hỗ trợ ít nhất một cách cài model không phụ thuộc API runtime: import file bằng SAF.
- Model load/unload không chạy trên main thread.
- Không load đồng thời nhiều model lớn nếu không cần.
- Phải có memory/resource cleanup.
- Device không đủ khả năng phải nhận lỗi typed `DeviceIncompatible`, không crash/OOM.
- Model output phải được parse/validate; JSON lỗi không được coi là success.
- Lưu model version, prompt version và pipeline version cùng kết quả.

## 6. Workflow cho từng task

1. Đọc prompt và docs liên quan.
2. Inspect source hiện tại.
3. Cập nhật `CURRENT_TASK.md`.
4. Xác định acceptance criteria và edge cases.
5. Implement smallest complete vertical slice.
6. Thêm test.
7. Chạy checks/tests/build.
8. Test thủ công nếu có thiết bị/emulator phù hợp.
9. Cập nhật `PROJECT_STATUS.md`, `CHANGELOG.md`, `KNOWN_ISSUES.md`.
10. Báo bằng chứng thật.

## 7. Output cuối mỗi task

```text
Task:
Status: Complete | Partial | Blocked

Files changed:
Architecture/state flow:
Tests added:
Commands executed:
- Command:
  Result:
Warnings:
Manual device tests:
Remaining risks:
Next eligible prompt:
```

## 8. Stop conditions

Dừng và yêu cầu quyết định chỉ khi thay đổi:

- định dạng file hoặc data migration không thể đảo ngược;
- chính sách privacy/legal;
- mô hình license/giá;
- minimum supported device/RAM;
- model license không tương thích thương mại;
- thay toàn bộ recorder engine;
- thay public data contract.

Với lựa chọn kỹ thuật nhỏ, có thể đảo ngược: chọn phương án hợp lý, ghi ADR và tiếp tục.
