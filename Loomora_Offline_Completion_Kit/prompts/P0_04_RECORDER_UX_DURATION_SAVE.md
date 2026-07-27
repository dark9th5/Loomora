# P0.4 — Recorder UX, Paused Duration and Save Acknowledgement

```text
Implement task P0.4 only.

Requirements:
1. Mở Recorder screen không tự ghi.
2. Grant permission không tự ghi; sau khi grant hiển thị ready state.
3. Chỉ start khi user bấm Record.
4. Double tap/rapid recomposition không tạo hai session/service start.
5. Title truyền từ UI/session được lưu thật, không bị thay bằng title tự sinh ngoài ý muốn.
6. Sửa duration:
   - paused time không được tính;
   - stop trực tiếp khi paused trả final duration đúng;
   - multiple pause/resume đúng;
   - final duration được snapshot trước khi reset engine.
7. State flow tối thiểu:
   `Idle/Ready → Preparing → Recording ↔ Paused → Finalizing → Saving → Saved`
   và typed error states.
8. UI chỉ navigate khỏi recorder sau khi Room/file save thành công.
9. Save failure giữ user ở state có thể retry/recover và không báo Saved.
10. Notification stop dùng cùng save flow.
11. Back navigation khi recording có confirmation đúng.
12. Không hiển thị raw exception text.

Tests:
- screen does not auto-start;
- permission grant does not auto-start;
- stop while paused duration;
- multiple pauses;
- save success emits Saved only after repository success;
- save failure does not navigate;
- title is persisted;
- duplicate start ignored.

Acceptance:
- device/manual test từ screen và notification.
- library chỉ hiện recording đã lưu thật.
- duration metadata khớp playback trong tolerance hợp lý.
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
