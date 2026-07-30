# Bản sửa Loomora: transcript, người nói và Smart Insights

Bản sửa này được tạo dựa trên nhánh `main` của repository Loomora được kiểm tra ngày 30/07/2026.

## Những lỗi đã xử lý

### 1. Transcript bị tách thành các mẩu 2–3 từ

- Không còn chia văn bản theo mọi mốc diarization thô.
- Chỉ chiếu một ASR block dài lên nhiều lượt nói khi có đủ chữ để tạo các đoạn có ý nghĩa.
- Mỗi đoạn chiếu có tối thiểu khoảng 7 từ; các đuôi quá ngắn được ghép lại.
- Các transcript liên tiếp của cùng người nói, cách nhau không quá 1,8 giây, được ghép thành một lượt nói hoàn chỉnh.

### 2. Một người đang nói liên tục nhưng bị đổi thành Speaker 2

- Thêm hậu xử lý `SpeakerTurnStabilizer` sau Sherpa ONNX.
- Ghép các lượt cùng speaker có khoảng ngắt ngắn.
- Một “đảo speaker” dài tối đa 1,4 giây chỉ bị đổi lại khi nó nằm giữa hai lượt của cùng một speaker. Cách này giảm đổi speaker giả nhưng không xóa mù quáng mọi câu chen ngang.
- Tăng thời gian chờ im lặng của VAD để một khoảng nghỉ ngắn trong câu không tạo thêm quá nhiều speech window.

### 3. Timeline và bản chép lời hiển thị quá nhiều dòng nhỏ

- Timeline và Transcript dùng `LazyRow`.
- Một item tương ứng với **một lượt nói liên tục**, gồm:
  - tên speaker/alias;
  - thời gian bắt đầu – kết thúc;
  - toàn bộ câu/đoạn nói;
  - nút phát hoặc seek từ đúng thời điểm.
- Không gom toàn bộ mọi lần xuất hiện của một người thành duy nhất một item, vì cùng một người có thể nói ở nhiều thời điểm không liên tục.

### 4. “Thông tin chính” chỉ sao chép các đoạn transcript

- Sửa lỗi routing: model `.litertlm` giờ được đưa vào `LiteRtLmMeetingInsightEngine`; trước đây nó bị trả thẳng về heuristic.
- Chỉ chạy heuristic khi không có model phù hợp hoặc model AI lỗi/thiếu bộ nhớ.
- Trước khi phân tích, transcript được ghép thành các lượt nói hoàn chỉnh.
- Prompt yêu cầu phân tích toàn bộ hội thoại nhiều người, tổng hợp chủ đề, diễn biến, quyết định, câu hỏi và việc tiếp theo; không được lấy cụm 2–3 từ làm insight.
- Engine heuristic fallback cũng được đổi từ “top câu rồi copy” sang tổng hợp chủ đề theo toàn bộ bản ghi và các giai đoạn của cuộc trao đổi.

## Các file được thay đổi

1. `core/offlineai/src/main/java/com/loomora/core/offlineai/OfflineAiModels.kt`
2. `core/offlineai/src/main/java/com/loomora/core/offlineai/TranscriptTextSegmenter.kt`
3. `core/offlineai/src/main/java/com/loomora/core/offlineai/TranscriptSpeakerFusion.kt`
4. `core/offlineai/src/main/java/com/loomora/core/offlineai/SherpaOnnxDiarizationEngine.kt`
5. `core/offlineai/src/main/java/com/loomora/core/offlineai/AudioTranscriptionPreprocessor.kt`
6. `core/offlineai/src/main/java/com/loomora/core/offlineai/ExtractiveMeetingInsightEngine.kt`
7. `core/offlineai/src/main/java/com/loomora/core/offlineai/LiteRtLmMeetingInsightEngine.kt`
8. `core/offlineai/src/main/java/com/loomora/core/offlineai/LlamaCppMeetingInsightEngine.kt`
9. `core/offlineai/src/main/java/com/loomora/core/offlineai/FallbackMeetingInsightEngine.kt`
10. `core/offlineai/src/main/java/com/loomora/core/offlineai/di/OfflineAiModule.kt`
11. `feature/recordingdetail/src/main/java/com/loomora/feature/recordingdetail/RecordingDetailScreen.kt`
12. `feature/recordingdetail/src/main/java/com/loomora/feature/recordingdetail/RecordingDetailViewModel.kt`

## Cách áp dụng trên Windows

Đặt thư mục giải nén ở bất kỳ đâu rồi chạy PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File .\apply-loomora-fix.ps1 -ProjectRoot "D:\duong-dan\Loomora"
```

Script sẽ sao lưu các file cũ vào thư mục `.loomora-backup-<thời gian>` trong project trước khi chép file mới.

Sau đó chạy tại thư mục project:

```powershell
.\gradlew.bat clean assembleDebug
```

Hoặc mở Android Studio, chọn **Sync Project with Gradle Files**, sau đó build app.

## Cách áp dụng trên macOS/Linux

```bash
chmod +x apply-loomora-fix.sh
./apply-loomora-fix.sh /duong-dan/Loomora
cd /duong-dan/Loomora
./gradlew clean assembleDebug
```

## Sau khi cài bản APK mới

1. Mở lại bản ghi cũ: giao diện sẽ ghép các mẩu transcript cũ để không còn hàng loạt card 2–3 từ.
2. Chọn phân tích/chạy lại bản ghi để tạo diarization và insights theo pipeline `v2`.
3. Kiểm tra model insight đã được cài và có trạng thái `READY`. Nếu model `.litertlm` không tồn tại hoặc chạy lỗi, hệ thống vẫn dùng heuristic fallback và đánh dấu chất lượng suy giảm.

## Các ngưỡng có thể tinh chỉnh

Trong `TranscriptSpeakerFusion.kt`:

- `MERGE_TRANSCRIPT_GAP_MS = 1_800`: khoảng ngắt tối đa để ghép hai transcript cùng speaker.
- `MERGE_SAME_SPEAKER_GAP_MS = 750`: khoảng ngắt tối đa để ghép diarization cùng speaker.
- `ISLAND_MAX_DURATION_MS = 1_400`: độ dài tối đa của speaker “đảo” có thể coi là lỗi clustering.

Trong `AudioTranscriptionPreprocessor.kt`:

- `AMPLITUDE_SILENCE_HANGOVER_MS = 800`.
- `VAD_MERGE_GAP_MS = 800`.

Nếu app vẫn đổi speaker quá nhiều, có thể tăng `ISLAND_MAX_DURATION_MS` lên khoảng `1_700–2_000`. Nếu app nuốt mất câu chen ngang thật của người khác, giảm xuống khoảng `900–1_100`.

## Phạm vi kiểm tra

- Đã biên dịch độc lập bằng Kotlin compiler các phần: segmenter, fusion, stabilizer, heuristic insight và router model.
- Đã chạy test mô phỏng:
  - một đoạn 35 từ của Speaker 1 có “đảo” Speaker 2 dài 0,8 giây được giữ thành một lượt Speaker 1;
  - một lượt đổi speaker thật dài 2,8 giây vẫn được giữ riêng;
  - không tạo đoạn chiếu chỉ 2–3 từ.
- Chưa chạy được toàn bộ Android Gradle build trong môi trường tạo bản sửa vì không có bản clone đầy đủ và Android SDK của project. Hãy chạy `assembleDebug` trên máy phát triển trước khi merge/deploy.
