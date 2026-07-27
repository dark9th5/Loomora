# Current Repository Findings

Mốc rà soát ban đầu: 2026-07-26. Agent phải xác nhận lại vì source có thể đã thay đổi.

## Cấu trúc hiện tại

Các module chính đã thấy:

```text
:app
:core:common
:core:model
:core:designsystem
:core:database
:core:datastore
:core:audio
:core:network
:core:testing
:feature:onboarding
:feature:home
:feature:recorder
:feature:library
:feature:recordingdetail
:feature:editor
:feature:settings
:feature:subscription
```

## P0 findings cần xác minh

### CI

`.github/workflows/ci.yml` chạy:

```text
./gradlew check
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleRelease
```

Không được sửa CI bằng cách bỏ task đang fail.

### Release signing

`app/build.gradle.kts` từng có:

```kotlin
release {
    isMinifyEnabled = false
    signingConfig = signingConfigs.getByName("debug")
}
```

Debug signing cho production release phải bị loại bỏ.

### Recording ID và marker

`RecorderViewModel` từng insert marker với:

```kotlin
recordingId = "active"
```

Marker phải dùng ID thật của active session và count phải lấy từ Room flow.

### Auto-record

`RecorderScreen` từng start recording:

- ngay sau khi permission được grant;
- trong `LaunchedEffect(hasMicPermission)` khi screen mở.

Phải chuyển thành explicit user action.

### Duration

`AudioRecordEngine.calculateElapsedDurationMs()` từng trừ `totalPausedMs`, nhưng `totalPausedMs` chỉ cập nhật khi resume. Stop trực tiếp lúc paused có thể tính sai.

### Save acknowledgement

Service từng phát `Completed`, gọi save trong coroutine rồi dừng ngay. UI không có confirmation rằng Room insert đã thành công.

### Resource lifecycle

Engine/service từng dùng `CoroutineScope(... + Job())` mà không có cleanup rõ. Player phải được kiểm tra tương tự.

### Hard-coded strings

Recorder và notification có nhiều English strings hard-code.

## P1 findings cần xác minh

### Editor giả

Editor exporter từng copy nguyên file và chỉ thay metadata duration. Acceptance criteria mới bắt buộc kiểm tra audio output thật.

### Waveform

Live amplitude history không thay thế waveform từ file đã lưu.

### File recovery

Cần xác định app hiện có journal/status/recovery scan hay chưa.

## P2 findings cần xác minh

### Fake AI provider

Các provider trong `core:network` từng trả failure hoặc fake result. Production AI phải chuyển sang local runtime và xóa fake success path.

### Fake entitlement

Entitlement từng dựa trên chuỗi key đơn giản và state trong memory. Phải thay bằng durable state + signed license.

### Existing agent docs conflict

`Loomora_AI_Agent_Kit/docs/11_AI_PIPELINE.md` từng yêu cầu hybrid/cloud và upload. Hướng đó bị superseded bởi offline-only architecture trong bộ này.
