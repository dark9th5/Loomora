# P1.4 — Real Non-Destructive Audio Editor

```text
Implement task P1.4 only.

Mục tiêu: thay exporter copy-file giả bằng edit/export audio thật.

Use official current Media3 Transformer/Composition APIs where suitable. Resolve and pin a stable Media3 version in version catalog. Do not add abandoned/unlicensed FFmpeg binary casually.

Domain:
- immutable edit recipe;
- keep ranges or trim/delete operations;
- undo/redo;
- source fingerprint;
- recipe revision.

Requirements:
1. Original file never overwritten.
2. Preview phản ánh recipe.
3. Export tạo audio có nội dung thật:
   - trim;
   - delete middle range;
   - concatenate remaining ranges.
4. Dùng temp output, validate rồi publish.
5. Re-read actual duration, MIME/container, sample rate/channels/size từ output.
6. Không chỉ sửa database metadata.
7. Cancel export cleanup temp.
8. Export failure giữ recipe và original.
9. UI progress/error/success.
10. Share exported result.
11. Document unsupported operations, codec/device limitations.
12. Nếu Media3 API hiện hành không đáp ứng một operation, implement adapter boundary và ADR; không giả success.

Tests:
- trim start/end;
- delete middle;
- keep multiple ranges;
- invalid/overlap ranges;
- empty result rejected;
- cancel;
- corrupt source;
- output duration/content validation;
- original hash unchanged.

Acceptance:
- nghe output xác nhận đoạn bị xóa thật.
- output metadata khớp file.
- existing fake copy path bị xóa khỏi production.
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
