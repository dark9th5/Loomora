# Execution Map

## Mục tiêu sửa lại thứ tự

Thứ tự cũ đưa transcript/backend trước khi recorder, persistence và editor đủ ổn. Thứ tự mới bảo đảm mỗi lớp có dữ liệu thật để lớp sau sử dụng.

```text
P0 Stability
  ↓
P1 Real local audio product
  ↓
P2 Offline intelligence + licensing
  ↓
Release audit
```

## P0

### P0.1 CI baseline
Build phải xanh trước khi thêm native AI.

### P0.2 Signing
Không dùng debug key cho release.

### P0.3 Durable recording session
Recording ID tồn tại trước marker và được dùng thống nhất bởi UI, service, file và Room.

### P0.4 Recorder correctness
Explicit start, accurate paused duration, save acknowledgement.

### P0.5 Lifecycle/localization
Release resources và loại hard-coded UI strings.

## P1

### P1.1 Recovery
Phân biệt recording đang ghi, đang finalizing, playable partial và corrupt.

### P1.2 File operations
Rename, trash, restore, delete, share/export và storage warning.

### P1.3 Real waveform
Waveform từ audio đã decode, có cache.

### P1.4 Real editor
Edit recipe thật và export nội dung audio thật.

### P1.5 Local enhancement
Tích hợp sherpa-onnx ở phạm vi nhỏ trước: PCM preparation + speech enhancement.

## P2

### P2.1 Offline AI foundation
Model manager, native runtime lifecycle, job schema.

### P2.2 Transcription
VAD + Whisper multilingual bằng sherpa-onnx.

### P2.3 Diarization
Pyannote segmentation + speaker embedding qua sherpa-onnx.

### P2.4 Meeting insights
LiteRT-LM, hierarchical summary, strict structured output.

### P2.5 Resilient pipeline
Checkpoint, retry, cancel, idempotency, foreground progress.

### P2.6 Commercial offline entitlement
Signed offline license + durable trial.

### P2.7 Release truthfulness
Website chỉ quảng cáo thứ đã hoạt động.

## Nguyên tắc giao task

Mỗi prompt phải tạo một vertical slice có thể nghiệm thu. Không giao “làm toàn bộ AI” hoặc “hoàn thiện app”.
