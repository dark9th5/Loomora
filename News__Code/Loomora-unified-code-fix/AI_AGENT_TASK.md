# AI_AGENT_TASK — Hoàn tất live caption và live translation

Các lỗi transcript/task/audio/model routing đã được script trong gói áp dụng trực tiếp. Không viết lại chúng nếu test đã xanh.

## Việc còn phải tích hợp để bật live feature

1. Tạo `PcmAudioRecordEngine` bằng Android `AudioRecord`:
   - PCM 16-bit, 48 kHz, mono.
   - foreground service.
   - pause/resume.
   - amplitude.
   - encode AAC/M4A bằng MediaCodec + MediaMuxer.
   - ghi âm phải tiếp tục dù ASR/dịch lỗi.

2. Chia PCM:
   - nhánh ghi M4A;
   - nhánh RNNoise 480 samples/frame;
   - resample 16 kHz cho VAD/ASR.

3. Live ASR:
   - VAD chốt utterance sau 400–800 ms im lặng.
   - partial có thể hiển thị câu gốc.
   - final phát `LiveTranscriptEvent.Final`.
   - model phải chọn qua `TranscriptionModelSelector`.

4. Translation provider:
   - implement `LocalTranslationEngine`.
   - model phải được tải trước nếu yêu cầu offline.
   - không gọi mạng ngầm trong buổi ghi.
   - chỉ dịch final utterance mặc định.

5. Recorder UI:
   - Live captions toggle.
   - Translation toggle.
   - Source: Auto / vi / en.
   - Target: vi / en; không cho source == target.
   - Hiển thị source và translated text thành hai dòng.
   - Khi backlog cao: tắt dịch trước, rồi giảm partial; không ảnh hưởng recorder.

6. Data:
   - lưu live revision riêng;
   - batch transcript sau khi dừng là revision chính xác hơn;
   - không ghi đè âm thầm live revision;
   - task/evidence mặc định lấy batch final transcript.

## Acceptance

- EN → VI và VI → EN chạy ở airplane mode sau khi model đã tải.
- Một người nói dài vẫn được ngắt thành caption rows dễ đọc.
- Tắt ASR/dịch cưỡng bức không làm hỏng M4A.
- Source/target đổi được trước buổi ghi.
- Build/test xanh trên ARM64 thật.
