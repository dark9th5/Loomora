# Trạng thái chức năng sau khi áp dụng

## Code được áp dụng trực tiếp

- Chia transcript dài thành đoạn dễ đọc.
- Không ghép vô hạn các đoạn của cùng người nói.
- Tự sửa cách hiển thị transcript cũ mà không đổi evidence trong database.
- Bộ lọc task Việt/Anh chặt hơn.
- Nhận nhiều task trong cùng một transcript row.
- Tăng tối đa action candidates từ 5 lên 8.
- Task ID gồm evidence + title + assignee, tránh gộp hai việc khác nhau trong cùng segment.
- Chọn model theo ngôn ngữ:
  - Việt ưu tiên model Việt chuyên biệt.
  - Anh không dùng model chỉ hỗ trợ tiếng Việt.
  - Auto ưu tiên model multilingual.
- Đồng bộ metadata audio với định dạng ghi thực tế: 48 kHz, mono, 128 kbps.
- Bổ sung contract và coordinator cho live caption/translation; chỉ dịch utterance đã chốt.

## Chưa được bật tự động

Live caption và live translation chưa thể hoạt động chỉ bằng việc thêm giao diện, vì recorder hiện tại dùng `MediaRecorder` và không phát PCM realtime cho ASR.

Gói đã thêm code lõi compile-ready cho:
- LiveTranscriptEvent
- LiveCaptionRow
- TranslationSelection
- LocalTranslationEngine
- FinalUtteranceTranslationCoordinator

Để bật tính năng trên máy thật vẫn phải hoàn thành `AudioRecord`/PCM capture, streaming VAD/ASR và provider dịch on-device theo `AI_AGENT_TASK.md`.

Đây là giới hạn kỹ thuật thật, không phải một toggle còn thiếu.
