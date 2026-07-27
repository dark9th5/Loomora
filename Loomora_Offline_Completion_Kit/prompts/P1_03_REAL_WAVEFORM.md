# P1.3 — Real Persisted Waveform

```text
Implement task P1.3 only: waveform từ dữ liệu audio đã lưu.

Requirements:
1. Không dùng live amplitude history làm waveform của recording.
2. Decode audio hoặc dùng Media3 waveform/audio sink API hiện hành để tạo amplitude bins.
3. Normalize và downsample thành số bin hợp lý cho UI zoom levels.
4. Cache waveform theo:
   - recording/source fingerprint;
   - algorithm version;
   - resolution.
5. Generate ở background; UI có loading/error/fallback.
6. Cancel khi recording bị xóa.
7. Không giữ toàn bộ PCM của recording dài trong RAM.
8. Waveform timestamp mapping phải khớp seek/playback/editor.
9. Regenerate khi export/source revision đổi.
10. Thêm fixture audio deterministic.

Tests:
- silence;
- constant tone;
- short file;
- long file streaming memory behavior;
- corrupt input;
- cache hit;
- cache invalidation;
- timestamp-to-bin mapping.

Acceptance:
- waveform hiển thị lại sau restart.
- seek vào waveform phát đúng vị trí.
- không OOM với fixture dài hợp lý.
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
