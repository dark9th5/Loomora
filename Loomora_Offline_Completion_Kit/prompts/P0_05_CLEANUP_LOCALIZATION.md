# P0.5 — Resource Cleanup and Localization

```text
Implement task P0.5 only.

Requirements:
1. Audit all long-lived coroutine scopes in recorder, service, player and repositories.
2. Dùng structured concurrency/SupervisorJob phù hợp.
3. Service cancel scope và release recorder trong `onDestroy`.
4. Recorder luôn release ở success, error và cancellation paths.
5. ExoPlayer/Media3 player có explicit release/close.
6. Timer/amplitude collectors không sống sau session.
7. Không gọi native/media object sau release.
8. Chuyển toàn bộ user-visible hard-coded strings sang resources:
   - recorder screen;
   - dialogs;
   - notification channel/title/actions;
   - errors có user-facing mapping.
9. Hoàn thiện English + Vietnamese cho strings mới.
10. Content descriptions, TalkBack labels và touch target cho controls quan trọng.
11. Không lấy resource bằng cách đưa Android Context vào domain layer; map tại UI/service boundary.
12. Thêm leak/lifecycle tests khả thi.

Acceptance:
- không có hard-coded user-visible recorder/notification English strings.
- service/player/recorder cleanup có test hoặc deterministic proof.
- rotate/navigate/background không tạo duplicate collector.
- P0 exit commands đều pass.
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
