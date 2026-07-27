# P0.1 — Fix CI Until Green

```text
Implement task P0.1 only: làm toàn bộ CI baseline xanh bằng cách sửa root causes.

Read:
- .github/workflows/ci.yml
- Gradle root/settings/version catalog
- failing module build files
- Loomora_Offline_Completion_Kit/docs/06_DEFINITION_OF_DONE.md

Requirements:
1. Reproduce failures locally, one command at a time.
2. Fix compilation, lint, test or configuration root causes.
3. Không bỏ `check`, unit tests, debug build hoặc release compile.
4. Không disable lint, không blanket suppress, không đổi warning thành ignored chỉ để xanh.
5. CI phải dùng JDK/Gradle/AGP tương thích với repo.
6. Có thể cải thiện cache và upload test/lint reports khi failure, nhưng không che failure.
7. Official production signing secret không được yêu cầu trong pull-request CI.
8. Nếu release compile cần điều chỉnh signing, chỉ làm thay đổi tối thiểu và để signing đầy đủ cho P0.2.
9. Ghi rõ mọi warning còn lại.

Acceptance:
- `./gradlew check` pass.
- `./gradlew testDebugUnitTest` pass.
- `./gradlew assembleDebug` pass.
- `./gradlew assembleRelease` pass hoặc tạo unsigned release compile hợp lệ mà không dùng debug signing.
- CI YAML tương ứng với các lệnh đã chứng minh.
- Không có test bị xóa/skip mới vô lý.
```

## Quy trình bắt buộc

Trước khi sửa:

1. Inspect source và git status.
2. Viết plan cụ thể vào `CURRENT_TASK.md`.
3. Liệt kê acceptance criteria và tests.
4. Không sửa ngoài phạm vi nếu không cần để build.

Sau khi sửa:

1. Chạy checks/tests/build liên quan.
2. Ghi kết quả thật.
3. Cập nhật tracking docs.
4. Báo file thay đổi, rủi ro và manual tests.
5. Dừng sau task này; không tự nhảy sang prompt kế tiếp.

## Cấm

- Fake/hard-coded success.
- Tắt test/lint.
- Xóa test để build.
- Che lỗi bằng empty state.
- Bịa kết quả command.
