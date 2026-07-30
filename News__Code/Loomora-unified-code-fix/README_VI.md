# Loomora Unified Code Fix

Đây là một gói duy nhất để sửa các lỗi đã thảo luận, mục tiêu là áp dụng an toàn lên `ad3683b` hoặc nhánh kế thừa.

## Những gì script tự sửa

### Transcript

`TranscriptTextSegmenter`:

- mục tiêu khoảng 28 từ/đoạn;
- tối đa 38 từ;
- ưu tiên dấu câu, dấu phẩy và ranh giới ý;
- không tạo các mảnh 2–3 từ;
- không mất hoặc lặp từ.

`TranscriptSpeakerFusion`:

- chỉ nối các mảnh ngắn cùng người nói;
- không nối nếu vượt 32 từ, 260 ký tự hoặc 15 giây;
- dừng nối khi câu trước đã hoàn chỉnh;
- `displayRows()` chia lại cả transcript cũ khi hiển thị/xuất.

### Task tự động

- Loại câu hỏi và phủ định.
- Nhận câu giao việc/commitment Việt và Anh.
- Nhận `Lan, please send...`.
- Nhận người phụ trách, ngày tương đối, ngày số và thời gian.
- Có thể tách hai việc trong cùng một transcript row.
- Tăng action candidate lên 8.
- Task identity:
  `recordingId + evidence + normalized title + normalized assignee`.

### Audio/RNNoise

- Dùng `AudioCaptureSpec` làm nguồn cấu hình chung.
- Sửa metadata Room từ 44,1 kHz/stereo thành 48 kHz/mono.
- Giữ bitrate 128 kbps.
- Không xóa bản M4A gốc.
- Gói này chưa đổi WAV enhanced sang M4A vì cần một bước encoder thật; đổi đuôi file mà không encode sẽ làm hỏng audio.

### Model Việt/Anh

- Thêm `TranscriptionModelSelector`.
- Tiếng Việt ưu tiên model chuyên Việt khi READY.
- English dùng model hỗ trợ English/multilingual.
- Auto ưu tiên multilingual.
- Model người dùng chọn thủ công nhưng không tương thích sẽ bị từ chối thay vì chạy sai.

### Live caption và dịch

Gói đã thêm domain code và quy tắc:

- partial chỉ hiện câu gốc;
- final utterance mới dịch;
- source/target dùng language tag;
- không cho source và target giống nhau;
- lỗi dịch không làm mất caption gốc.

Phần capture PCM realtime vẫn phải được nối theo `AI_AGENT_TASK.md`.

## Cách áp dụng

PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File "<package>\apply-loomora-unified-fix.ps1"
```

Linux/macOS:

```bash
bash "<package>/apply-loomora-unified-fix.sh"
```

Script:

- tạo backup trong `.loomora-unified-backup`;
- chạy lặp lại an toàn;
- dừng nếu cấu trúc source không khớp;
- không chạy destructive Room migration.

## Kiểm tra

```powershell
.\gradlew.bat :core:offlineai:testDebugUnitTest :core:audio:testDebugUnitTest
.\gradlew.bat clean check testDebugUnitTest :app:assembleDebug
```

Kiểm thử máy thật vẫn bắt buộc cho microphone, RNNoise và hiệu năng.
