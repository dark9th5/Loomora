# Offline AI Pipeline

## Hard boundary

Không upload và không gọi API cho:

- denoise;
- voice enhancement;
- voice activity detection;
- transcription;
- language detection;
- speaker diarization;
- speaker identification;
- summary;
- decisions;
- action items;
- chapters;
- tags.

## Runtime selection

### sherpa-onnx

Dùng làm runtime chung cho:

- VAD;
- speech enhancement;
- non-streaming ASR;
- speaker diarization;
- speaker embeddings.

Baseline model direction:

- ASR: Whisper multilingual ONNX, tiered by device capability.
- Diarization segmentation: sherpa-onnx-supported Pyannote segmentation model.
- Speaker embedding: 3D-Speaker INT8 hoặc model chính thức tương thích.
- Enhancement: GTCRN hoặc DPDFNet sau benchmark.

Agent phải kiểm tra model license và tài liệu official trước khi phân phối.

### LiteRT-LM

Dùng Kotlin SDK cho local LLM inference.

Baseline integration model:

- một model `.litertlm` nhỏ, instruction-tuned, hỗ trợ tiếng Việt đủ dùng;
- có thể bắt đầu bằng model được sample chính thức dùng để chứng minh integration;
- model selection là cấu hình/manifest, không hard-code sâu trong UI.

Pin SDK version cụ thể. Không dùng `latest.release`.

## Pipeline

```text
Original M4A
  ↓ validate and fingerprint
Decode to PCM
  ↓ resample 16 kHz mono for speech pipeline
Optional speech enhancement
  ↓
VAD segmentation
  ↓
ASR with timestamped segments
  ↓
Speaker diarization
  ↓
Timeline alignment
  ↓
Normalized transcript
  ↓ split into timestamp-preserving chunks
Local LLM per-chunk extraction
  ↓
Global synthesis + deduplication
  ↓
Schema validation + evidence validation
  ↓
Persist result
```

## Data preservation

Giữ:

```text
original audio
edit recipe
exported edited audio
analysis PCM temp only while needed
transcript revisions
insight revisions
```

Xóa PCM temp sau success/cancel/failure cleanup.

## Hierarchical summary

Không đưa toàn bộ cuộc họp dài vào một prompt.

```text
3–5 minute transcript chunks
  → chunk summaries and facts
  → aggregate summaries
  → final summary/decisions/actions
```

Chunk boundary nên tôn trọng speaker turn và câu hoàn chỉnh.

## Structured output

LLM phải trả schema có thể validate. Ví dụ:

```json
{
  "title": "string",
  "summary": "string",
  "decisions": [
    {
      "text": "string",
      "evidenceSegmentIds": ["segment-id"],
      "confidence": 0.0
    }
  ],
  "actionItems": [
    {
      "task": "string",
      "assignee": null,
      "dueDate": null,
      "evidenceSegmentIds": ["segment-id"],
      "confidence": 0.0
    }
  ],
  "openQuestions": [],
  "topics": []
}
```

## Hallucination rules

- `assignee = null` nếu không rõ.
- `dueDate = null` nếu không rõ.
- Không đổi suggestion thành decision.
- Item không có evidence hợp lệ phải bị loại hoặc đánh dấu low confidence.
- Output parse lỗi không được coi là success.
- User có thể chỉnh sửa kết quả nhưng app phải phân biệt user-edited và model-generated.

## Device capability

Trước khi chạy:

- kiểm tra ABI;
- available storage;
- memory class;
- model compatibility;
- thermal/battery constraints nếu job dài;
- backend CPU/GPU/NPU hỗ trợ.

Fallback phải deterministic:

```text
preferred backend fails → safe CPU fallback if supported
device insufficient → typed incompatibility error
```

Không retry vô hạn khi OOM hoặc model incompatible.
