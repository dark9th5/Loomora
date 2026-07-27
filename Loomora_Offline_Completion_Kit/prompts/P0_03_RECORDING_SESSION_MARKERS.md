# P0.3 — Durable Recording Session and Markers

```text
Implement task P0.3 only: sửa active recording session và marker ID.

Inspect:
- AudioRecordEngine
- AudioRecorderService
- RecorderViewModel/Screen
- recording and marker entities/DAOs/repositories
- foreign keys and Room migrations
- notification actions

Required design:
1. Tạo `recordingId` trước khi bắt đầu recorder.
2. Một active session dùng cùng ID ở:
   - Room recording row;
   - recorder engine;
   - service command;
   - output path;
   - UI state;
   - marker foreign key.
3. Không dùng literal `"active"`.
4. Persist recording row/status trước khi marker có thể insert.
5. Nếu recorder start fail, chuyển typed failed/interrupted state và cleanup file/row theo policy.
6. Marker count là Room Flow theo active recording ID, không phải MutableStateFlow chỉ ở RAM.
7. Label marker dùng resource hoặc domain default, không hard-code không thể localize.
8. Pause/resume/stop phải đi qua một command/controller path thống nhất. ViewModel không bypass service để điều khiển engine trực tiếp.
9. Chặn duplicate start và stale command từ session cũ.
10. Room schema change phải có migration/test; không destructive migration production.
11. Dùng transaction cho các update nhiều bảng liên quan.

Tests:
- marker insert dùng active ID thật;
- foreign key không fail;
- marker count survives ViewModel recreation;
- second recording không nhận marker của first recording;
- start failure không để active session giả;
- duplicate start chỉ tạo một recording.

Acceptance:
- Không còn `"active"` production marker ID.
- Recording và marker xuất hiện đúng sau recreate.
- State owner rõ ràng, không có hai đường điều khiển recorder.
- Build/test pass.
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
