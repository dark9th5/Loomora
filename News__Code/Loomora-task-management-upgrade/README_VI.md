# Nâng cấp Loomora: từ Action Items thành Task quản lý được

Gói này được tạo trên mã nguồn nhánh `main` mới nhất của `dark9th5/Loomora` tại thời điểm ngày 30/07/2026. Patch không chứa model AI, APK hoặc khóa bí mật.

## Những gì đã nâng cấp

### 1. Task được lưu độc lập trong Room

- Thêm bảng `recording_tasks` và `RecordingTaskDao`.
- Nâng database từ version 8 lên version 9, có `MIGRATION_8_9`; không dùng destructive migration.
- Mỗi `actionItem` do phần phân tích sinh ra được tự đồng bộ thành một task.
- ID task được tạo ổn định theo `recordingId + nội dung task` để hạn chế tạo bản sao khi phân tích lại.
- Nếu người dùng đã sửa task, đánh dấu hoàn thành hoặc ẩn task thì lần mở lại không làm mất trạng thái đó.
- Xóa vĩnh viễn bản ghi sẽ xóa task liên quan bằng foreign key `CASCADE`.

### 2. Task có thể sử dụng thực tế

Trong tab **Thông tin chính / Insights**, mỗi task có:

- Checkbox hoàn thành/chưa hoàn thành.
- Nội dung công việc.
- Người phụ trách.
- Hạn hoàn thành.
- Nhãn cho biết kết quả đến từ `LLM_ENHANCED` hay bộ trích xuất cục bộ cần kiểm tra lại.
- Nút sửa.
- Nút ẩn task.

Khi chia sẻ hoặc xuất kết quả phân tích, Loomora ưu tiên danh sách task đã lưu và thêm trạng thái `[ ]` hoặc `[x]`.

### 3. Fallback heuristic thận trọng hơn

Thêm `HeuristicActionItemExtractor`:

- Loại câu hỏi như “Ai sẽ gửi báo cáo?”.
- Loại câu phủ định như “Không cần gửi báo cáo nữa”.
- Chỉ nhận task khi có động từ hành động tương đối cụ thể.
- Nhận dạng một số mẫu giao việc/nhận việc bằng tiếng Việt và tiếng Anh.
- Trích xuất người phụ trách, ngày cụ thể hoặc hạn tương đối như “thứ Sáu”, “ngày mai”, “cuối tuần”, “Friday”, “next week”.
- Tăng tối đa task fallback từ 3 lên 5 nhưng lọc chặt hơn.

Bài test kèm theo kiểm tra các trường hợp giao việc tiếng Việt, cam kết tiếng Anh, câu hỏi và câu phủ định.

## Điều patch này không giả định

Patch **không đóng gói model `.litertlm`**. Luồng hiện tại của Loomora sẽ:

1. Dùng LiteRT-LM thật khi người dùng đã cài/import model Insights hợp lệ và runtime chạy thành công.
2. Tự chuyển về heuristic nếu không có model, model lỗi, thiếu RAM hoặc đầu ra không hợp lệ.
3. Hiển thị nguồn của từng task để người dùng biết task nào cần xác nhận lại.

Vì vậy, chất lượng hiểu ngữ nghĩa cao nhất vẫn phụ thuộc model Insights được cài trên thiết bị. Không nên quảng cáo mọi task fallback là kết luận chắc chắn của AI.

## Cách áp dụng trên Windows

Từ thư mục đã giải nén gói patch:

```powershell
powershell -ExecutionPolicy Bypass -File .\apply-loomora-task-upgrade.ps1 -ProjectRoot "D:\duong-dan\Loomora"
```

Script tạo backup trong thư mục dạng:

```text
Loomora/.loomora-task-upgrade-backup-YYYYMMDD-HHMMSS
```

## Cách áp dụng trên macOS/Linux

```bash
./apply-loomora-task-upgrade.sh /duong-dan/Loomora
```

## Build và test

Windows:

```powershell
cd D:\duong-dan\Loomora
.\gradlew.bat clean :core:database:testDebugUnitTest :core:offlineai:testDebugUnitTest :app:assembleDebug
```

macOS/Linux:

```bash
cd /duong-dan/Loomora
./gradlew clean :core:database:testDebugUnitTest :core:offlineai:testDebugUnitTest :app:assembleDebug
```

Room/KSP sẽ tạo schema version 9 tại:

```text
core/database/schemas/com.loomora.core.database.LoomoraDatabase/9.json
```

Hãy review và commit file schema đó cùng code.

## Kiểm thử thủ công nên thực hiện

1. Mở một bản ghi đã có action items: task phải tự xuất hiện mà không cần phân tích lại.
2. Sửa nội dung, người phụ trách và deadline; thoát màn hình rồi mở lại, dữ liệu phải còn nguyên.
3. Đánh dấu hoàn thành rồi mở lại app; checkbox và gạch ngang phải còn.
4. Ẩn một task rồi phân tích lại cùng nội dung; task đã ẩn không được tự xuất hiện lại.
5. Phân tích câu “Ai sẽ gửi báo cáo vào ngày mai?”; không được sinh task.
6. Phân tích câu “Chúng ta không cần gửi báo cáo nữa”; không được sinh task.
7. Phân tích câu “Giao cho Lan cập nhật báo cáo trước thứ Sáu”; task phải có assignee `Lan` và hạn `thứ Sáu`.
8. Xuất file text; task hoàn thành phải bắt đầu bằng `[x]`.
9. Nâng cấp app đang có database version 8; dữ liệu bản ghi/transcript/insights cũ phải được giữ lại.

## Các file chính

- `core/database/.../entity/RecordingTaskEntity.kt`
- `core/database/.../dao/RecordingTaskDao.kt`
- `core/database/.../LoomoraDatabase.kt`
- `core/database/.../Migrations.kt`
- `core/database/.../di/DatabaseModule.kt`
- `core/offlineai/.../HeuristicActionItemExtractor.kt`
- `core/offlineai/.../ExtractiveMeetingInsightEngine.kt`
- `feature/recordingdetail/.../RecordingDetailViewModel.kt`
- `feature/recordingdetail/.../RecordingDetailScreen.kt`
- `feature/recordingdetail/.../RecordingDetailDialogs.kt`
- `feature/recordingdetail/.../RecordingDetailRoute.kt`

## Hoàn tác

Chép các file trong `.loomora-task-upgrade-backup-*` về vị trí cũ. Hai file mới `RecordingTaskEntity.kt` và `RecordingTaskDao.kt` cần xóa thủ công nếu hoàn tác toàn bộ code. Không hạ database đã chạy version 9 về version 8 trên cùng dữ liệu; nên gỡ app/xóa dữ liệu thử nghiệm hoặc viết migration hạ cấp riêng nếu thật sự cần.
