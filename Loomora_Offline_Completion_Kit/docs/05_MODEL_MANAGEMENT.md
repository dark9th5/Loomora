# Offline Model Management

## Model manifest

Mỗi model pack phải có:

```kotlin
data class OfflineModelManifest(
    val id: String,
    val version: String,
    val capability: ModelCapability,
    val runtime: RuntimeKind,
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String,
    val minimumRamMb: Int?,
    val supportedAbis: Set<String>,
    val supportedLanguages: Set<String>,
    val licenseName: String,
    val licenseUrl: String?,
    val sourceUrl: String?,
    val pipelineCompatibility: String
)
```

## Install states

```text
NOT_INSTALLED
IMPORTING
VERIFYING
READY
INCOMPATIBLE
CORRUPT
REMOVING
ERROR
```

## Install methods

Bắt buộc:

- import từ file bằng Storage Access Framework;
- checksum verification;
- atomic move vào app-managed storage.

Tùy chọn sau:

- tải từ URL tĩnh/asset delivery;
- bundle một model nhỏ.

Processing runtime không phụ thuộc vào model download API.

## Storage

Model không đặt trong cache có thể bị hệ thống xóa. Dùng app-managed files và hiển thị dung lượng.

Cần hỗ trợ:

- xem dung lượng từng pack;
- xóa model mà không xóa transcript/result cũ;
- update model;
- rollback khi update lỗi;
- phát hiện file model bị thiếu.

## Lifecycle

- Model init trên background dispatcher.
- Một engine/session lớn tại một thời điểm trừ khi benchmark chứng minh an toàn.
- `close()` trong `finally`/`use`.
- Không giữ LLM và ASR model cùng lúc nếu không cần.
- Không để ViewModel trực tiếp sở hữu native engine.
- Process death không để DB ghi model `READY` khi file chưa hoàn tất.

## Version policy

- Pin exact dependency version trong `libs.versions.toml`.
- Pin model ID/version/SHA.
- Ghi quyết định trong ADR.
- Upgrade runtime/model thành task riêng có regression tests.
