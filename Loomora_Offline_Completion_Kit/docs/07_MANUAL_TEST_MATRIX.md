# Manual Test Matrix

## Recorder

- Permission denied.
- Permission granted nhưng chưa bấm Record: không ghi.
- Double tap Record: chỉ một session.
- Pause → wait → Stop: duration không tính thời gian pause.
- Pause → resume nhiều lần.
- Stop từ notification.
- App background trong lúc ghi.
- Activity bị recreate.
- Low storage.
- Microphone đang bị app khác dùng.
- Force-stop/process kill để kiểm tra interrupted state.
- Marker trước/sau pause.
- Marker count sau recreate.

## Library

- Rename.
- Favorite.
- Trash/restore.
- Permanent delete.
- Share qua FileProvider/SAF.
- File bị xóa bên ngoài.
- File zero-byte/corrupt.
- Storage nearly full.

## Editor

- Trim đầu/cuối.
- Xóa một đoạn giữa.
- Giữ nhiều đoạn và ghép.
- Undo/redo.
- Cancel export.
- Export failure.
- Output play được.
- Duration output khớp nội dung.
- Original không đổi.

## Enhancement

- Voice clean.
- Fan noise.
- Traffic/background noise.
- Very quiet speech.
- Input stereo/mono.
- Cancellation.
- Device low memory.

## Transcript

- Tiếng Việt.
- Tiếng Anh.
- Code-switch Vietnamese/English.
- Tên riêng.
- Im lặng dài.
- Hai người nói.
- Nhiều người nói.
- Nói chồng.
- Airplane mode.
- Model missing/corrupt.

## Insights

- Meeting không có action item.
- Có task nhưng không có assignee.
- Có assignee nhưng không có deadline.
- Suggestion, không phải decision.
- Transcript dài.
- Invalid/truncated LLM output.
- User edit result.
- Evidence click phát đúng đoạn.

## License

- Valid signed license.
- Modified payload.
- Wrong signature.
- Expired.
- Clock rollback.
- Trial success/failure/cancel.
- Free local feature khi license invalid.
