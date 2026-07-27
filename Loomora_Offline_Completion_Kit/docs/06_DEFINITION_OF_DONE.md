# Definition of Done

Một task chỉ complete khi tất cả mục liên quan đạt.

## Functional

- Dữ liệu production thật đi qua toàn bộ vertical slice.
- Loading, success, empty, permission denied, cancellation và error có UI/state.
- Không có fake result.
- Không làm mất original recording.
- Process recreation/interruption đã được xem xét.

## Data integrity

- Room/file update nhất quán.
- Không có orphan file hoặc orphan row sau failure path đã test.
- Output metadata được đọc từ file thật.
- Marker/timestamp không trỏ recording giả.
- Job/result có pipeline/model version.

## Tests

- Unit test cho business/state transitions.
- Room integration/migration test khi schema đổi.
- File/audio test fixture phù hợp.
- Worker idempotency/cancellation test khi có job.
- UI test cho critical state nếu khả thi.
- Native runtime có ít nhất một smoke test trên thiết bị thật trước khi gọi complete.

## Build evidence

Tối thiểu:

```bash
./gradlew check
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleRelease
```

Nếu release signing secrets không có, release compile phải vẫn được chứng minh theo cấu hình CI an toàn. Không dùng debug key.

Khi có thiết bị:

```bash
./gradlew connectedDebugAndroidTest
```

## Offline evidence

Với task AI:

- model đã được cài;
- bật airplane mode;
- transcript/analysis vẫn hoàn tất;
- không có network dependency trong processing path;
- app xử lý rõ trường hợp model chưa cài.

## Performance evidence

Ghi lại trên reference device:

- model load time;
- processing duration;
- peak memory nếu đo được;
- output file size;
- crash/OOM;
- thermal observation cho recording dài.

Không cần hứa một threshold chưa được benchmark, nhưng phải báo số thật.

## Documentation

Cập nhật:

- `CURRENT_TASK.md`
- `PROJECT_STATUS.md`
- `CHANGELOG.md`
- `KNOWN_ISSUES.md`
- ADR nếu có quyết định bền vững.
