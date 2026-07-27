# 00 — Audit and Baseline

```text
Thực hiện audit repository Loomora hiện tại. Chưa sửa feature code.

Đọc:
- Loomora_Offline_Completion_Kit/AGENTS.md
- docs/00_EXECUTION_MAP.md
- docs/01_CURRENT_REPO_FINDINGS.md
- docs/02_TARGET_ARCHITECTURE.md
- docs/06_DEFINITION_OF_DONE.md

Nhiệm vụ:
1. Inspect module graph, Gradle, manifest, CI, Room schemas, recorder/service/controller, player, editor, fake network/AI và entitlement.
2. Kiểm tra git status và không ghi đè thay đổi chưa commit.
3. Chạy từng baseline command riêng để xác định failure đầu tiên:
   - ./gradlew check --stacktrace
   - ./gradlew testDebugUnitTest --stacktrace
   - ./gradlew assembleDebug --stacktrace
   - ./gradlew assembleRelease --stacktrace
4. Xác nhận từng finding trong docs/01_CURRENT_REPO_FINDINGS.md: TRUE, FIXED hoặc CHANGED.
5. Tạo/cập nhật CURRENT_TASK.md với task P0 sớm nhất.
6. Cập nhật PROJECT_STATUS.md và KNOWN_ISSUES.md.
7. Không thêm dependency hoặc implementation trong audit.

Output:
- repository map;
- build environment;
- command/result;
- first actionable root cause;
- current production-fake paths;
- recommended next prompt;
- exact files likely affected.
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
