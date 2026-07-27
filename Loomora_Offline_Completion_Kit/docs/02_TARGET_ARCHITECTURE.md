# Target Architecture

## Dependency direction

```text
Compose UI
  → ViewModel
    → repositories/controllers
      → Room / DataStore / file storage
      → recorder/player/audio processing
      → offline AI runtimes
      → WorkManager orchestration
```

## Module strategy

Không tạo nhiều module rỗng. Khuyến nghị giữ module hiện tại và chỉ thêm một boundary khi bắt đầu P2:

```text
:core:offlineai
```

`core:offlineai` có thể chứa:

```text
runtime/
  SherpaRuntime
  LiteRtLmRuntime
model/
  ModelManifest
  ModelCompatibility
manager/
  OfflineModelManager
pipeline/
  LocalTranscriptionEngine
  LocalDiarizationEngine
  LocalMeetingInsightEngine
```

`core:audio` tiếp tục sở hữu:

- recorder and playback gateways;
- decode/encode/container operations;
- PCM conversion;
- edit recipe/export;
- waveform extraction;
- file validation.

`core:database` sở hữu:

- recordings;
- markers;
- transcript segments;
- speakers;
- analysis jobs;
- insight results;
- installed model metadata.

`feature:transcript` và `feature:insights` chỉ thêm khi UI thật được triển khai. Không tạo trước dưới dạng shell.

## Recording ownership

Sai:

```text
UI → engine.start
Notification → service.start
ViewModel → engine.stop
```

Đúng:

```text
UI/ViewModel
  → RecordingController
    → AudioRecorderService commands
      → RecorderEngine
        → RecorderSessionStore + files
```

Mọi caller quan sát cùng một persisted session state.

## Processing ownership

```text
UI requests processing
  → repository creates AnalysisJob row
  → WorkManager enqueues unique work
  → worker reads job and model manifests
  → stage runner writes checkpoints
  → outputs written to temp files/tables
  → transaction publishes completed result
  → UI observes Room
```

## Provider contracts

Tên interface không được gắn cloud:

```kotlin
interface LocalTranscriptionEngine
interface LocalDiarizationEngine
interface LocalMeetingInsightEngine
interface LocalSpeechEnhancementEngine
```

Không dùng `NetworkAiProvider` hoặc provider DTO trong domain/UI.

## Result integrity

Mỗi result lưu:

- recording ID;
- source file fingerprint;
- pipeline version;
- model ID/version;
- prompt/schema version;
- created time;
- status;
- evidence timestamps;
- error code nếu fail.

Nếu source file thay đổi hoặc model/pipeline đổi, kết quả cũ được giữ nhưng đánh dấu stale hoặc tạo revision mới.
