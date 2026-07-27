# P1.2 — Library Operations and Storage Safety

```text
Implement task P1.2 only.

Implement real flows:
- rename;
- favorite;
- soft delete to trash;
- restore;
- permanent delete;
- share;
- export/copy through Storage Access Framework;
- storage usage and low-space warning.

Requirements:
1. Room + file operations nhất quán.
2. Trash không xóa file ngay; retention policy rõ.
3. Permanent delete xử lý file failure và DB failure an toàn.
4. Share dùng FileProvider/content URI, không expose `file://`.
5. Export dùng user-selected destination và báo progress/error.
6. Không overwrite destination im lặng.
7. Nếu source file missing/corrupt, UI có typed error và repair/remove action.
8. Hiển thị dung lượng recordings, exports, temp và models tách biệt nếu có.
9. Check free storage trước record/export/model import.
10. Search/filter không chặn main thread.

Tests:
- rename persists;
- trash/restore;
- permanent delete;
- failed filesystem delete;
- share URI permission;
- export cancellation;
- low storage warning;
- missing source file.

Acceptance:
- operations chạy trên file thật.
- restart app giữ đúng state.
- không tạo orphan thông thường trong tested failure paths.
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
