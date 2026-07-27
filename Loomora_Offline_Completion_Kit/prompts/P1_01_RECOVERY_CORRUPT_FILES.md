# P1.1 — Recording Recovery and Corrupt Files

```text
Implement task P1.1 only: recovery sau interruption/process death và file validation.

Requirements:
1. Persist session journal/status đủ để startup biết recording nào đang RECORDING/FINALIZING.
2. Tạo startup recovery scanner/use case:
   - row không có file;
   - file zero-byte;
   - file playable;
   - file không finalizable/corrupt;
   - orphan file không có row.
3. Không tự đánh dấu mọi partial file là SAVED.
4. Validate bằng media metadata/extractor và playback probe phù hợp.
5. Playable partial có thể được phục hồi với nhãn `Recovered`; corrupt phải hiện hành động xóa/giữ để chẩn đoán theo policy.
6. Không xóa file người dùng im lặng.
7. Nếu robust crash recovery đòi đổi recorder sang segment-based hoặc AudioRecord/MediaCodec pipeline, viết ADR trước, làm migration theo vertical slice và không rewrite toàn bộ app trong một commit.
8. Cleanup temp files phải có retention policy.
9. Recovery idempotent: chạy nhiều lần không duplicate row/file.
10. Handle app update/schema migration.

Tests:
- process interrupted status;
- zero byte;
- missing file;
- playable partial;
- corrupt file;
- orphan file;
- recovery run twice;
- no original deletion without explicit policy.

Acceptance:
- app startup không crash do dangling recording.
- user thấy trạng thái rõ.
- recovered file play được trước khi mark saved.
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
