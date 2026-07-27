# Data and Job States

## Recording status

Khuyến nghị:

```text
CREATING
RECORDING
PAUSED
FINALIZING
SAVED
INTERRUPTED
CORRUPT
DELETED
```

State transition phải được validate. Không chuyển trực tiếp từ bất kỳ state nào sang `SAVED`.

## Active session

Active recording session cần tối thiểu:

```text
recordingId
title
outputPath
startedAt
pausedAt
totalPausedMs
lastKnownDurationMs
status
serviceInstanceId
updatedAt
```

Marker luôn dùng `recordingId` thật.

## Analysis job status

```text
QUEUED
PREPARING_AUDIO
ENHANCING
DETECTING_SPEECH
TRANSCRIBING
DIARIZING
ALIGNING
SUMMARIZING_CHUNKS
SYNTHESIZING
VALIDATING
COMPLETED
CANCEL_REQUESTED
CANCELLED
RETRYABLE_FAILURE
TERMINAL_FAILURE
```

## Job identity

Unique logical key:

```text
recordingId + sourceFingerprint + pipelineVersion + requestedOptions
```

Enqueue lặp lại cùng key không tạo duplicate work.

## Checkpoints

Mỗi stage lưu:

- stage;
- progress;
- attempt;
- input fingerprint;
- output reference;
- started/finished timestamps;
- typed error;
- model version.

Retry bắt đầu từ checkpoint hợp lệ gần nhất.

## Atomic publication

Output không được xuất hiện như complete khi mới ghi một phần.

```text
write temp
→ validate
→ Room transaction
→ rename/publish
→ mark COMPLETED
```

## Typed errors

Ví dụ:

```text
PermissionDenied
MicrophoneUnavailable
StorageLow
RecorderInitializationFailed
RecordingInterrupted
FileMissing
FileCorrupt
ModelMissing
ModelChecksumMismatch
DeviceIncompatible
ModelInitializationFailed
OutOfMemory
ProcessingCancelled
InvalidModelOutput
ExportFailed
LicenseInvalid
LicenseExpired
```

UI không hiển thị raw stack trace.
