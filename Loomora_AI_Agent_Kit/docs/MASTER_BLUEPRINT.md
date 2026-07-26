# Loomora Product UX Engineering Blueprint

**Product Name:** Loomora

**Marketing Title:** Loomora – Smart Voice Recorder

**Tagline:** Smart Voice Recorder & AI Notes

---

# Loomora — Product, UX & Engineering Blueprint

> Tài liệu triển khai thực chiến cho ứng dụng Android Kotlin + Jetpack Compose ghi âm thông minh qua microphone.  
> Mục tiêu của tài liệu này không chỉ là liệt kê lỗi, mà là buộc đội phát triển hoặc AI coding agent tạo ra một sản phẩm **đẹp, dùng được, ổn định và có thể thương mại hóa**.

---

## 0. Cách sử dụng tài liệu này

Không giao cho AI yêu cầu kiểu:

> “Hãy làm toàn bộ app ghi âm thông minh cho tôi.”

Cách đó thường tạo ra một ứng dụng “vibe coding” có các vấn đề:

- Giao diện giống template hoặc dashboard web thu nhỏ.
- Mỗi màn hình dùng màu, khoảng cách và component khác nhau.
- Nút bấm có giao diện nhưng chưa nối nghiệp vụ thật.
- State ghi âm nằm rải rác trong UI, ViewModel và Service.
- Dữ liệu demo được hard-code.
- Chỉ chạy trong happy path; mất quyền, hết bộ nhớ hoặc app bị kill là hỏng.
- Không có migration, recovery, test release và kiểm thử trên thiết bị thật.

Quy trình bắt buộc:

1. Chốt một vertical slice nhỏ.
2. Viết UI specification và state machine.
3. Viết acceptance criteria.
4. AI chỉ triển khai đúng slice đó.
5. Build bản release, chạy trên thiết bị thật.
6. Kiểm thử happy path, error path và offline path.
7. Chụp ảnh hoặc quay video để rà soát giao diện.
8. Chỉ merge khi qua Definition of Done.
9. Sau đó mới sang slice tiếp theo.

**Một vertical slice phải đi xuyên suốt:** UI → ViewModel → Repository → Database/File → Service/Engine → Test.

---

# 1. Tầm nhìn sản phẩm

## 1.1. Định vị

**Loomora** là ứng dụng ghi âm local-first giúp người dùng:

- Ghi lại cuộc họp, phỏng vấn, bài giảng và ghi chú bằng giọng nói.
- Nghe lại nhanh, cắt chỉnh và làm rõ giọng.
- Khi có mạng, dùng AI để phiên âm, tóm tắt và tạo task.
- Vẫn ghi âm và sử dụng đầy đủ tính năng Free khi không đăng nhập hoặc không có Internet.

Tagline đề xuất:

> **Record locally. Remember clearly.**

## 1.2. Nguyên tắc sản phẩm

1. **Recording must never fail silently.** Ứng dụng không bao giờ được hiển thị “đang ghi” khi dữ liệu âm thanh thực tế không được ghi.
2. **Local-first by default.** Audio không tự động rời khỏi thiết bị.
3. **No login wall.** Người dùng có thể mở app và ghi ngay mà không cần tài khoản.
4. **Original audio is immutable.** Mọi chỉnh sửa mặc định không phá hủy file gốc.
5. **AI is evidence-based.** Mọi task, quyết định và nội dung quan trọng phải dẫn về timestamp nguồn.
6. **Offline is a first-class state.** Offline không phải lỗi bất ngờ mà là trạng thái được thiết kế sẵn.
7. **Beauty follows consistency.** Giao diện đẹp nhờ hệ thống nhất quán, không nhờ thêm nhiều gradient và animation.
8. **Release build is the truth.** Không coi debug build chạy được là hoàn thành.

---

# 2. Phạm vi sản phẩm

## 2.1. Free — luôn dùng được offline, không cần đăng nhập

- Ghi âm, pause, resume, stop.
- Ghi khi tắt màn hình bằng foreground service.
- Danh sách bản ghi.
- Đổi tên, yêu thích, tag và thư mục.
- Nghe lại, tua, tốc độ phát.
- Waveform.
- Trim đầu/cuối.
- Split và xóa đoạn không phá hủy file gốc.
- Bộ lọc tạp âm cơ bản nếu thiết bị hỗ trợ.
- Export M4A.
- Light, Dark, Follow system.
- English mặc định, Vietnamese tùy chọn.

## 2.2. Trial — cho phép trải nghiệm trước khi mua

Đề xuất ban đầu:

- 3 lần Enhance Speech nâng cao.
- 3 lần phiên âm và tóm tắt AI.
- 1 lần export nâng cao.
- Không bắt đăng nhập để bắt đầu trial.
- Hiển thị rõ số lượt còn lại trước khi người dùng chạy tác vụ.
- Chỉ trừ lượt khi tác vụ thành công.
- Nếu xử lý thất bại do server hoặc mất mạng, không trừ lượt.

## 2.3. Pro

- Enhance Speech nâng cao.
- Phiên âm dài hơn.
- Speaker diarization.
- Tóm tắt, key points, decisions, open questions.
- Tạo task có dẫn chứng timestamp.
- Tìm kiếm nội dung transcript.
- Export WAV, transcript, Markdown/PDF sau này.
- Cloud backup tùy chọn sau này.
- Custom vocabulary sau này.

## 2.4. Không làm trong MVP

- Ghi âm hai chiều cuộc gọi điện thoại.
- Ghi âm trực tiếp Zalo, Messenger, WhatsApp.
- Workspace nhiều người.
- SSO doanh nghiệp.
- AI chatbot tổng quát không dựa trên transcript.
- Thay đổi hoặc giả giọng người nói.

---

# 3. Kiến trúc trải nghiệm người dùng

## 3.1. Navigation chính

Bottom navigation chỉ gồm:

- **Home**
- **Library**
- **Tasks**
- **Settings**

Nút Record là Floating Action Button nổi bật, có mặt ở Home và Library.

Không đặt quá 5 mục điều hướng chính. Không dùng hamburger menu cho chức năng thường xuyên.

## 3.2. Luồng chính

```text
Open app
→ Home
→ Tap Record
→ Permission check
→ Recording session
→ Stop
→ Save immediately
→ Recording detail
→ Optional: Edit / Enhance / Transcribe / Summarize
```

## 3.3. Các trạng thái toàn cục bắt buộc

Mỗi màn hình có tối thiểu các trạng thái phù hợp:

- Initial.
- Loading.
- Content.
- Empty.
- Offline.
- Permission denied.
- Recoverable error.
- Fatal error.
- Processing.
- Success.

Không được dùng một biến Boolean như `isLoading` để biểu diễn toàn bộ trạng thái phức tạp.

Ví dụ:

```kotlin
data class LibraryUiState(
    val recordings: List<RecordingItemUi> = emptyList(),
    val query: String = "",
    val isRefreshing: Boolean = false,
    val selection: Set<RecordingId> = emptySet(),
    val message: UiMessage? = null,
    val error: LibraryError? = null
)
```

---

# 4. Design system bắt buộc

## 4.1. Mục tiêu thẩm mỹ

Phong cách:

- Tối giản, hiện đại, tin cậy.
- Ưu tiên nội dung và trạng thái ghi âm.
- Không dùng quá nhiều card lồng card.
- Không dùng gradient làm nền toàn màn hình.
- Không nhồi icon vào mọi dòng.
- Không biến app thành dashboard quản trị.

Tham chiếu cảm giác sử dụng:

- Sạch như Google Recorder.
- Tập trung như Voice Memos.
- Có chiều sâu thông tin như Notion, nhưng không sao chép giao diện.

## 4.2. Typography

Dùng font hệ thống Android/Roboto để bảo đảm dễ đọc và không tăng kích thước app.

| Token | Size | Weight | Dùng cho |
|---|---:|---:|---|
| Display | 40sp | 600 | Timer ghi âm lớn |
| Headline Large | 28sp | 600 | Tiêu đề màn hình |
| Headline Medium | 22sp | 600 | Tiêu đề section |
| Title Large | 18sp | 600 | Tên bản ghi |
| Title Medium | 16sp | 500 | Card, dialog title |
| Body Large | 16sp | 400 | Nội dung chính |
| Body Medium | 14sp | 400 | Metadata |
| Label Large | 14sp | 600 | Button |
| Label Small | 12sp | 500 | Chip, timestamp |

Quy tắc:

- Không dùng nhiều hơn 3 cấp chữ trên một màn hình.
- Không dùng chữ nhỏ hơn 12sp.
- Hỗ trợ font scale 200% không vỡ layout.
- Dùng số tabular cho timer để chữ số không nhảy chiều rộng.

## 4.3. Color tokens

### Light

- `Primary`: `#4F46E5`
- `OnPrimary`: `#FFFFFF`
- `PrimaryContainer`: `#E7E7FF`
- `Background`: `#F8F9FC`
- `Surface`: `#FFFFFF`
- `SurfaceVariant`: `#EEF0F5`
- `TextPrimary`: `#17181C`
- `TextSecondary`: `#626772`
- `Outline`: `#D8DBE3`
- `Recording`: `#D92D20`
- `Success`: `#12805C`
- `Warning`: `#B54708`

### Dark

- `Primary`: `#B9B7FF`
- `OnPrimary`: `#201A73`
- `Background`: `#101114`
- `Surface`: `#181A1F`
- `SurfaceVariant`: `#24272E`
- `TextPrimary`: `#F3F4F6`
- `TextSecondary`: `#AEB3BE`
- `Outline`: `#383C45`
- `Recording`: `#FF6B63`

Quy tắc:

- Màu đỏ chỉ dành cho ghi âm, xóa và lỗi nghiêm trọng.
- Không dùng primary color cho tất cả icon.
- Bảo đảm contrast tối thiểu WCAG AA.
- Waveform chưa phát dùng màu trung tính; phần đã phát dùng primary.

## 4.4. Spacing, shape và elevation

Spacing scale duy nhất:

```text
4, 8, 12, 16, 20, 24, 32, 40, 48 dp
```

Shape:

- Button: 14dp.
- Card: 18dp.
- Sheet: top corners 28dp.
- Chip: pill hoặc 12dp.
- Recorder main control: circle.

Elevation:

- Hầu hết surface: 0–1dp.
- Bottom bar/FAB: 3–6dp.
- Không dùng shadow nặng cho card.

## 4.5. Component library

Không viết lại component tùy ý trong từng màn hình. Design system phải có:

- `LoomoraTopBar`
- `LoomoraPrimaryButton`
- `LoomoraSecondaryButton`
- `LoomoraIconButton`
- `LoomoraRecordButton`
- `LoomoraRecordingCard`
- `LoomoraEmptyState`
- `LoomoraErrorState`
- `LoomoraOfflineBanner`
- `LoomoraProcessingBanner`
- `LoomoraSectionHeader`
- `LoomoraFilterChip`
- `LoomoraConfirmDialog`
- `LoomoraBottomSheet`
- `LoomoraWaveform`
- `LoomoraSkeleton`

Mỗi component phải có preview cho:

- Light.
- Dark.
- English.
- Vietnamese.
- Font scale lớn.
- Trạng thái disabled/loading/error khi phù hợp.

---

# 5. Đặc tả từng màn hình

## 5.1. Splash và khởi tạo

Không hiển thị splash tùy chỉnh kéo dài. Dùng Android SplashScreen API.

Trong lúc khởi tạo:

- Mở database.
- Scan session chưa finalize.
- Khôi phục entitlement local.
- Không gọi mạng bắt buộc.

Nếu database migration thất bại, không xóa dữ liệu tự động. Hiển thị Recovery screen.

## 5.2. Onboarding

Tối đa 3 trang:

1. Record locally.
2. Turn speech into useful notes.
3. Your audio stays on your device unless you choose AI cloud processing.

Cuối onboarding:

- `Start recording`.
- `Explore first`.

Không xin microphone ngay khi mở app. Chỉ xin khi người dùng chủ động bấm Record.

## 5.3. Home

Bố cục:

1. Top bar: logo chữ Loomora, search, profile/license indicator.
2. Hero compact: `Ready to record?` + Record button.
3. Recent recordings: tối đa 4 bản ghi.
4. Pending tasks: tối đa 3 task.
5. Trial/Pro card chỉ xuất hiện khi phù hợp, không chiếm nửa màn hình.

Empty state:

- Minh họa waveform đơn giản.
- Tiêu đề: `Your recordings will appear here`.
- CTA: `Start your first recording`.

Không hiển thị biểu đồ giả, số liệu giả hoặc placeholder “120 hours saved”.

## 5.4. Recorder

Đây là màn hình quan trọng nhất.

Bố cục từ trên xuống:

1. Close/back có xác nhận nếu đang ghi.
2. Editable title hoặc `Untitled recording`.
3. Input source nhỏ: Phone microphone/Bluetooth headset.
4. Timer lớn.
5. Waveform chiếm vùng trung tâm.
6. Quality status: Good / Too quiet / Clipping / No signal.
7. Live transcript panel nếu người dùng bật.
8. Controls: Marker, Pause/Resume, Stop.

Quy tắc tương tác:

- Stop là nút vuông đỏ trong vòng tròn trắng/neutral, không dùng icon khó hiểu.
- Pause và Resume thay đổi rõ icon và label accessibility.
- Bấm Stop một lần phải disable ngay để tránh finalize hai lần.
- Khi chưa ghi được byte PCM sau một khoảng kiểm tra, hiện lỗi thay vì tiếp tục timer giả.
- Khi dung lượng thấp, hiển thị banner cảnh báo và thời gian còn lại ước tính.
- Khi app ra background, notification hiển thị timer và Pause/Stop.

Recorder state machine:

```text
Idle
→ Preparing
→ Recording
↔ Paused
→ Stopping
→ Finalizing
→ Saved

Any active state
→ Recovering
→ Saved or Failed
```

Không cho phép chuyển trực tiếp bằng cách set Boolean. Mọi command đi qua reducer/state machine duy nhất.

## 5.5. Save result

Sau khi stop:

- Lưu bản ghi trước.
- Điều hướng tới detail ngay khi metadata và file đã an toàn.
- Các tác vụ waveform generation hoặc AI có thể chạy sau.
- Không giữ người dùng trong màn hình spinner dài.

Nếu finalize một số segment thất bại:

- Vẫn lưu phần đã thu được.
- Hiển thị `Recovered recording`.
- Cho thử repair/export.

## 5.6. Library

Mỗi recording row/card gồm:

- Title tối đa 2 dòng.
- Date/time.
- Duration.
- Tag hoặc trạng thái processing.
- Waveform mini hoặc icon audio, không bắt buộc load waveform đầy đủ.
- Overflow menu.

Hỗ trợ:

- Search.
- Filter: All, Favorites, Transcribed, Unprocessed.
- Sort: Newest, Oldest, Duration, Name.
- Multi-select.
- Trash.

Danh sách lớn phải dùng lazy loading/paging, stable key và không decode waveform trên main thread.

## 5.7. Recording Detail

Top area:

- Editable title.
- Date, duration, file size.
- Favorite, Share/Export, More.
- Compact player luôn hiển thị.

Tabs:

- `Overview`
- `Transcript`
- `Audio`
- `Tasks`

### Overview

- AI summary hoặc CTA tạo summary.
- Key points.
- Decisions.
- Open questions.
- Mỗi item có timestamp evidence.

### Transcript

- Speaker name.
- Timestamp.
- Text.
- Search.
- Tap segment để seek.
- Edit transcript.

### Audio

- Waveform lớn.
- Trim/split/delete range.
- Denoise.
- Enhance Speech.
- Before/After preview.

### Tasks

- Checkbox.
- Task title.
- Assignee nếu có bằng chứng.
- Due date nếu được nói rõ.
- Source timestamp.

## 5.8. Audio Editor

Không cố làm DAW chuyên nghiệp.

MVP chỉ cần:

- Zoom waveform.
- Drag trim handles.
- Split tại playhead.
- Chọn range và delete.
- Undo/redo.
- Preview.
- Save as new version hoặc update edit plan.
- Export.

Luôn hiển thị:

- `Original audio is preserved`.
- Duration trước/sau.
- Nút Reset edits.

Khi export:

- Tạo temp file.
- Verify duration/size.
- Atomic rename.
- Dọn temp khi lỗi.

## 5.9. AI Processing

Trước upload:

- Cho biết dữ liệu nào sẽ được gửi.
- Provider/cloud processing disclosure.
- Ước tính dung lượng.
- Số lượt trial còn lại.
- Nút Cancel.

Các trạng thái:

```text
Ready
→ WaitingForNetwork
→ Uploading(progress)
→ Transcribing
→ Analyzing
→ Completed
or Failed(retryable/non-retryable)
```

Không dùng spinner vô hạn. Mỗi state có nội dung mô tả và hành động phù hợp.

## 5.10. Paywall và mua Pro

Paywall phải nêu rõ:

- Free vẫn dùng được.
- Pro mở khóa gì.
- Trial còn bao nhiêu lượt.
- Gói/thời hạn.
- Cách khôi phục hoặc kích hoạt license.
- Link Privacy/Terms.

Không khóa hoặc làm mờ bản ghi local của người dùng sau khi hết Pro.

## 5.11. Settings

Nhóm cài đặt:

1. Recording: quality, input, silence behavior.
2. Appearance: system/light/dark.
3. Language: English/Vietnamese.
4. Storage: used space, cache, trash.
5. AI & Privacy: cloud consent, auto-delete uploaded audio.
6. Pro: status, activate, restore, manage.
7. About: version, privacy, terms, contact.

---

# 6. Accessibility và localization

Bắt buộc:

- Touch target tối thiểu 48dp.
- Icon-only button có `contentDescription`.
- Trạng thái Recording/Paused được TalkBack đọc đúng.
- Không chỉ dùng màu để biểu diễn trạng thái.
- Hỗ trợ font scale 200%.
- Hỗ trợ màn hình nhỏ 360dp width.
- Hỗ trợ landscape cho Recorder và Editor ở mức sử dụng được.
- Không nối chuỗi từ nhiều phần nhỏ gây sai ngữ pháp tiếng Việt/Anh.
- Không hard-code text trong Composable.
- Date, time, plural và số dùng locale API.

---

# 7. Kiến trúc kỹ thuật

## 7.1. Module

```text
:app
:core:common
:core:model
:core:designsystem
:core:database
:core:datastore
:core:audio
:core:media
:core:network
:core:ai
:core:license
:core:testing

:feature:onboarding
:feature:home
:feature:recorder
:feature:library
:feature:recordingdetail
:feature:editor
:feature:tasks
:feature:settings
:feature:pro
```

Không tách module chỉ để “trông enterprise”. Mỗi module phải có owner và ranh giới dependency rõ.

## 7.2. Dependency rule

```text
UI → Domain/use case → Repository interface
Infrastructure implementation → Repository interface
```

- Composable không gọi Room DAO.
- ViewModel không giữ `Context` trừ abstraction hợp lệ.
- Recorder Service không phụ thuộc Navigation.
- Audio engine không phát event UI trực tiếp.
- Billing/license không được quyết định chỉ bằng Boolean trong DataStore.

## 7.3. Single source of truth

Recording session state thuộc `RecordingSessionRepository` hoặc service-bound controller duy nhất.

UI quan sát:

```kotlin
val sessionState: StateFlow<RecordingSessionState>
```

UI gửi command:

```kotlin
start()
pause()
resume()
addMarker()
stop()
cancel()
```

Không cho UI tự set state.

## 7.4. Error model

Không truyền `Throwable.message` thẳng ra UI.

```kotlin
sealed interface RecordingError {
    data object PermissionDenied : RecordingError
    data object MicrophoneUnavailable : RecordingError
    data object NoAudioSignal : RecordingError
    data object StorageFull : RecordingError
    data object EncoderFailed : RecordingError
    data class Unknown(val cause: Throwable) : RecordingError
}
```

Mỗi lỗi phải ánh xạ tới:

- User message.
- Recovery action.
- Log code.
- Analytics event nếu được đồng ý.

---

# 8. Recording engine specification

## 8.1. Audio format

- Capture internal PCM 16-bit mono.
- Preferred sample rate 48 kHz.
- Fallback 44.1 kHz, sau đó 16 kHz nếu thiết bị không hỗ trợ.
- Output M4A/AAC-LC cho bản ghi thông thường.
- Segment hóa để phục hồi khi crash.

## 8.2. Threading

- Audio read loop chạy trên thread/dispatcher chuyên dụng.
- Không cấp phát object trong vòng lặp đọc PCM.
- Dùng buffer pool.
- Waveform UI nhận dữ liệu đã downsample.
- Database update không chạy trong audio loop.

## 8.3. Data safety

Mỗi segment:

1. Tạo file temp.
2. Ghi PCM/encoded data.
3. Flush/finalize.
4. Verify byte count và metadata.
5. Atomic rename.
6. Ghi DB transaction.

Startup recovery:

- Tìm session `Recording/Finalizing` từ lần chạy trước.
- Scan temp và segment files.
- Repair hoặc ghép phần hợp lệ.
- Không tự xóa ngay file không nhận diện được.

## 8.4. Watchdog

Trong khi ghi, theo dõi:

- Byte count tăng.
- Sample timestamp tăng.
- AudioRecord state.
- Disk free space.
- Encoder output.

Nếu timer chạy nhưng byte count không tăng, chuyển sang lỗi `NoAudioSignal` hoặc `RecorderStalled`.

---

# 9. Local-first, offline và license

## 9.1. Offline behavior matrix

| Hành động | Không mạng | Không đăng nhập | Hết Pro |
|---|---|---|---|
| Ghi âm | Hoạt động | Hoạt động | Hoạt động |
| Nghe lại | Hoạt động | Hoạt động | Hoạt động |
| Trim/split | Hoạt động | Hoạt động | Hoạt động |
| Export M4A | Hoạt động | Hoạt động | Hoạt động |
| Basic denoise | Hoạt động | Hoạt động | Hoạt động |
| Cloud transcription | Chờ mạng | Có thể dùng trial guest | Bị giới hạn |
| AI summary | Chờ mạng | Có thể dùng trial guest | Bị giới hạn |
| Pro local feature | Hoạt động nếu token còn hiệu lực | Hoạt động | Tắt khi entitlement hết hạn |
| Khôi phục mua hàng | Cần mạng | Cần account/code | Cần mạng |

## 9.2. Entitlement

Backend phát signed entitlement token chứa:

- License ID.
- Installation ID hash.
- Plan.
- Feature set.
- Issued at.
- Expires at hoặc perpetual.
- Signature.

App:

- Xác minh chữ ký bằng public key nhúng trong app.
- Lưu token mã hóa bằng Keystore-backed key.
- Không lưu secret ký token trong app.
- Không dùng `isPro=true` đơn giản.
- Không khóa local data khi token hết hạn.

## 9.3. Trial

Trial offline không thể chống gian lận tuyệt đối. Mục tiêu là chống reset đơn giản, không phải DRM cực đoan.

Quy tắc:

- Trial action được reserve trước khi chạy.
- Chỉ commit consumption khi thành công.
- Crash giữa chừng được reconciliation.
- Server có thể đối chiếu khi online.
- Không thu thập hardware identifier nhạy cảm không cần thiết.

---

# 10. AI pipeline

## 10.1. Provider abstraction

```kotlin
interface TranscriptionProvider {
    suspend fun transcribe(request: TranscriptionRequest): TranscriptResult
}

interface InsightProvider {
    suspend fun analyze(request: InsightRequest): InsightResult
}
```

Không để UI hoặc ViewModel biết tên provider cụ thể.

## 10.2. Output có cấu trúc

AI phải trả schema có validation:

```json
{
  "title": "Weekly product meeting",
  "summary": "...",
  "keyPoints": [],
  "decisions": [],
  "tasks": [],
  "openQuestions": [],
  "chapters": []
}
```

Mỗi insight có:

- `evidenceSegmentIds`.
- `confidence`.
- `sourceStartMs` khi có thể.

Nếu không có bằng chứng:

- Không hiển thị như sự thật.
- Gắn nhãn AI suggestion hoặc bỏ kết quả.

## 10.3. Error handling

Phân biệt:

- No network.
- Unauthorized.
- Trial exhausted.
- File too large.
- Unsupported language.
- Provider timeout.
- Rate limit.
- Invalid AI schema.
- Moderation/policy refusal.
- Server error.

Retry chỉ áp dụng với lỗi retryable và dùng exponential backoff.

---

# 11. Coding rules chống “vibe app”

## 11.1. Cấm

- Cấm hard-code dữ liệu demo trong production source set.
- Cấm Composable dài hơn khoảng 150 dòng mà không tách hợp lý.
- Cấm gọi network, DAO hoặc file I/O trực tiếp trong Composable.
- Cấm `GlobalScope`.
- Cấm `!!` trong luồng nghiệp vụ chính.
- Cấm catch `Exception` rồi bỏ qua.
- Cấm TODO trong release build.
- Cấm destructive migration.
- Cấm API key trong APK.
- Cấm một Boolean `isRecording` điều khiển toàn bộ recorder.
- Cấm tạo file output trực tiếp trên file gốc.
- Cấm hoàn thành feature mà chưa có empty/error/offline state.
- Cấm merge khi chỉ test debug emulator.

## 11.2. Bắt buộc

- Kotlin explicit API cho module core quan trọng khi phù hợp.
- Version catalog.
- Static analysis: Android Lint, Detekt hoặc tương đương.
- Formatter thống nhất.
- Unit test cho reducer/use case.
- Migration test.
- Release build CI.
- Screenshot test cho màn hình cốt lõi hoặc visual regression tương đương.
- Structured logging với mã lỗi, không log transcript/audio nhạy cảm.
- `@Preview` cho design system và màn hình chính.
- Stable keys trong Lazy lists.
- Immutable UI models.
- State restoration cho màn hình cần thiết.

## 11.3. Naming

- `RecordingEntity`: database.
- `Recording`: domain.
- `RecordingItemUi`: UI.
- `RecordingRepository`: interface.
- `DefaultRecordingRepository`: implementation.
- `RecordingDetailViewModel`: screen state owner.

Không dùng tên chung chung như `Manager`, `Helper`, `Utils`, `Data`, `Response2` nếu có thể đặt tên theo trách nhiệm.

---

# 12. Definition of Done

Một feature chỉ được đánh dấu Done khi đáp ứng tất cả mục liên quan:

## 12.1. Product

- Đúng user story.
- Không vượt phạm vi.
- Có acceptance criteria được kiểm chứng.
- Không dùng dữ liệu giả trong production.

## 12.2. UI/UX

- Đúng design tokens.
- Light và Dark.
- English và Vietnamese.
- Empty, loading, error, offline.
- Font scale 200%.
- Touch target 48dp.
- TalkBack cơ bản.
- Không overflow trên màn hình 360dp.
- Có screenshot hoặc video review.

## 12.3. Engineering

- Architecture boundary đúng.
- Không I/O trên main thread.
- State không bị duplicate.
- Error được typed.
- Không secret.
- Release build thành công.
- R8 build chạy được.

## 12.4. Test

- Unit test pass.
- UI/instrumentation test quan trọng pass.
- Test offline.
- Test permission denied.
- Test process recreation nếu liên quan.
- Test trên ít nhất một thiết bị thật.
- Không có P0/P1 mở.

## 12.5. Data safety

- Không mất file gốc.
- Tác vụ hủy giữa chừng không để file rác lâu dài.
- Database và file system reconciliation đúng.
- Có recovery path.

---

# 13. Quality gates trước mỗi bản phát hành

## Gate A — Build

- Debug build pass.
- Release build pass.
- R8 enabled build pass.
- Không có lint error nghiêm trọng.
- Dependency/license scan pass.

## Gate B — Recording reliability

- Ghi 1 phút, 30 phút, 2 giờ.
- Pause/resume nhiều lần.
- Lock screen.
- App process bị kill mô phỏng.
- Incoming call/audio interruption.
- Low storage.
- Permission revoked.
- Không có file 0 byte.

## Gate C — UI

- Light/Dark screenshots.
- English/Vietnamese screenshots.
- Small screen.
- Large font.
- Empty library.
- Library 1.000 items.
- Long title và long transcript.

## Gate D — Offline

- Fresh install không mạng.
- Ghi, nghe, chỉnh sửa, export không mạng.
- Mở app khi license cache tồn tại nhưng server không truy cập được.
- Cloud task chuyển WaitingForNetwork, không mất dữ liệu.

## Gate E — Commercial

- Trial không trừ lượt khi thất bại.
- License activation.
- Expired entitlement.
- Restore/refresh.
- Website download URL đúng.
- APK signed đúng key.
- Privacy/Terms/Contact hoạt động.

---

# 14. Test cases ưu tiên cao

## REC-P0-001 — Không được ghi giả

**Given:** người dùng đã bấm Record.  
**When:** AudioRecord không trả PCM hoặc mic bị chiếm.  
**Then:** app không tiếp tục hiển thị timer như đang ghi; phải báo lỗi và giữ phần dữ liệu hợp lệ.

## REC-P0-002 — Crash recovery

**Given:** session đang ghi và đã có nhiều segment.  
**When:** process bị kill hoặc máy mất pin.  
**Then:** lần mở tiếp theo app phát hiện session, khôi phục các segment hợp lệ và không tạo file 0 byte.

## REC-P0-003 — Hết dung lượng

**Given:** bộ nhớ sắp hết.  
**When:** dung lượng giảm dưới ngưỡng an toàn.  
**Then:** app cảnh báo; nếu không thể tiếp tục, app stop/finalize an toàn phần đã ghi.

## UI-P1-001 — Font scale

**Given:** font scale 200%.  
**When:** mở Home, Recorder và Detail.  
**Then:** các hành động chính vẫn nhìn thấy và sử dụng được; text không đè lên nhau.

## OFF-P1-001 — Guest offline

**Given:** fresh install, không mạng, chưa đăng nhập.  
**When:** người dùng ghi, đổi tên, nghe lại và trim.  
**Then:** mọi thao tác Free hoạt động bình thường.

## TRIAL-P1-001 — Không trừ lượt khi lỗi

**Given:** còn 1 lượt AI trial.  
**When:** upload timeout hoặc server trả lỗi.  
**Then:** lượt trial vẫn còn 1.

## LIC-P1-001 — Pro offline

**Given:** entitlement hợp lệ đã được xác minh và lưu cục bộ.  
**When:** mất mạng.  
**Then:** các tính năng Pro local vẫn dùng được tới hết thời hạn token.

---

# 15. Lộ trình phát triển theo vertical slice

## Phase 0 — Foundation

- Product requirements.
- Design tokens và component library.
- Module skeleton.
- Room schema v1.
- CI debug/release.
- Error model.

**Không chuyển phase nếu chưa có preview Light/Dark và CI release build.**

## Phase 1 — Record and recover

- Permission UX.
- Foreground recording service.
- AudioRecord engine.
- Segment writer.
- Watchdog.
- Pause/resume/stop.
- Crash recovery.

**Demo bắt buộc:** ghi 30 phút, khóa màn hình, kill process mô phỏng, mở lại và nghe được phần đã ghi.

## Phase 2 — Library and playback

- Save metadata.
- Library.
- Search/filter/sort.
- Media3 player.
- Waveform seek.
- Trash.

## Phase 3 — Non-destructive editor

- Edit operation model.
- Trim/split/delete range.
- Undo/redo.
- Export pipeline.
- Original preserved.

## Phase 4 — Local polish

- Basic denoise.
- Voice clarity presets.
- Storage management.
- Accessibility.
- Localization.

## Phase 5 — Trial and license

- Trial ledger.
- Signed entitlement.
- Activate code.
- Offline Pro cache.
- Paywall.

## Phase 6 — Cloud AI

- Consent.
- Resumable upload.
- Transcription.
- Structured insights.
- Evidence timestamp.
- Retry/WaitingForNetwork.

## Phase 7 — Commercial release

- Analytics opt-in/privacy-safe.
- Crash reporting.
- Website integration.
- Signed APK.
- Release checklist.
- Beta feedback loop.

---

# 16. Prompt chuẩn cho AI coding agent

## 16.1. Master prompt

```text
You are implementing Loomora, a commercial Android local-first smart recorder.
Use Kotlin, Jetpack Compose, coroutines/Flow, Room, DataStore and Media3 where appropriate.

Non-negotiable rules:
1. Free recording, playback and basic editing must work offline without login.
2. Never fake recording state. Recording UI must be driven by one recording session state machine.
3. Original audio must never be overwritten by editing.
4. No network, DAO or file I/O in Composables.
5. No hard-coded demo data in production source sets.
6. Every screen must handle content, empty, loading, recoverable error and offline states where applicable.
7. Use the existing design system tokens/components only; do not invent arbitrary colors, spacing or shapes.
8. Do not add dependencies without explaining license, size and maintenance impact.
9. Do not place secrets/API keys in the app.
10. The feature is not complete until release build, tests and acceptance criteria pass.

Before coding:
- Restate the exact vertical slice.
- List files/modules to change.
- Define state model and state transitions.
- List failure cases.
- List acceptance tests.

During coding:
- Implement production behavior, not a visual mock.
- Keep domain, data and UI boundaries.
- Use typed errors.
- Add tests with the implementation.

After coding:
- Report changed files.
- Report test/build commands and results.
- State remaining limitations honestly.
- Do not claim completion if build or tests were not run.
```

## 16.2. Prompt triển khai một feature

```text
Implement only this vertical slice: [FEATURE].

User story:
[USER STORY]

Acceptance criteria:
[CRITERIA]

Required UI states:
[STATES]

Required failure cases:
[FAILURES]

Constraints:
- Preserve current architecture and design tokens.
- No placeholder implementation.
- No unrelated refactor.
- Add unit tests and relevant UI/instrumentation tests.
- Run release build or clearly state why it could not be run.

First provide a brief implementation plan and identify any conflict with existing code. Then implement the slice.
```

## 16.3. Prompt audit bug và kiến trúc

```text
Audit this feature as if it is going to production.
Do not focus only on syntax.

Check:
- data loss and file corruption
- state races and double commands
- process death and recovery
- offline and permission behavior
- main-thread I/O and performance
- Room migration and consistency
- Compose recomposition and state ownership
- accessibility, localization, small screens and dark mode
- trial/license abuse and incorrect entitlement
- privacy and secret leakage
- release/R8-only failures

Return findings grouped as P0, P1, P2 and P3.
For every finding include evidence, reproduction steps and a concrete fix.
Do not invent issues without evidence from the code.
```

## 16.4. Prompt review giao diện

```text
Review the attached screenshots against the Loomora design system.
Evaluate hierarchy, spacing, typography, contrast, touch targets, component consistency,
empty/error/offline states, English/Vietnamese text expansion, dark mode and 200% font scale.

Do not suggest decorative changes without a usability reason.
Return:
1. blocking usability defects
2. consistency defects
3. accessibility defects
4. precise changes with dp/sp/token references
```

---

# 17. Quy trình làm việc với AI để tránh app xấu và nhiều bug

Cho mỗi feature:

1. Cung cấp file hiện có, không chỉ mô tả bằng lời.
2. Yêu cầu AI đọc architecture/design rules trước.
3. Yêu cầu kế hoạch file-level trước khi sửa.
4. Giới hạn một vertical slice.
5. Bắt AI viết state machine và failure cases.
6. Bắt AI chạy build/test.
7. Dùng screenshot để review giao diện.
8. Dùng audit prompt ở một lượt khác, tốt nhất bằng agent/model khác.
9. Không chấp nhận câu “đã hoàn thành” nếu không có build result.
10. Commit nhỏ để có thể revert.

Checklist khi AI trả code:

- Có dùng dữ liệu giả không?
- Nút có gọi nghiệp vụ thật không?
- Khi mất mạng thì sao?
- Khi từ chối quyền thì sao?
- Khi bấm hai lần thì sao?
- Khi process chết thì sao?
- Khi file/DB lệch nhau thì sao?
- Release build có chạy không?
- UI tiếng Việt có vỡ không?
- Dark mode/font lớn có vỡ không?

---

# 18. Tiêu chí MVP được phép đưa cho người dùng thử

Không phát hành beta nếu chưa đạt:

- Ghi liên tục tối thiểu 2 giờ trên thiết bị thật.
- Pause/resume ít nhất 20 lần không hỏng timeline.
- Khôi phục được phần lớn audio sau process death mô phỏng.
- Không có trường hợp UI báo Recording nhưng file không tăng.
- Free hoạt động hoàn toàn khi offline và chưa đăng nhập.
- Không có P0/P1 mở.
- Light/Dark, English/Vietnamese được review bằng screenshot.
- Library 1.000 bản ghi vẫn cuộn ổn.
- File export phát được trên Android, Windows và ít nhất một player phổ biến khác.
- Trial không bị trừ khi tác vụ thất bại.
- Pro local dùng được khi mất mạng với entitlement hợp lệ.
- Privacy, Terms, Contact và Delete/clear data flow rõ ràng.
- APK release đã ký và test, không chỉ debug APK.

---

# 19. Danh mục lỗi và rủi ro chi tiết

Phần dưới đây là catalog lỗi dùng để audit và viết test. Catalog không thay thế đặc tả sản phẩm, design system và Definition of Done ở trên.


# 3. Danh mục lỗi và rủi ro toàn diện

Mức độ:

- **P0**: mất dữ liệu, ghi âm sai, vi phạm pháp lý/bảo mật, không thể phát hành.
- **P1**: tính năng chính hỏng, crash/ANR, thanh toán sai, ảnh hưởng nhiều người dùng.
- **P2**: lỗi chức năng phụ, UX xấu, có workaround.
- **P3**: lỗi hiển thị nhỏ hoặc trường hợp hiếm.

## 3.1 Khởi tạo dự án, Gradle và dependency

| ID | Mức | Lỗi/rủi ro | Cách phòng ngừa/kiểm thử |
|---|---|---|---|
| BLD-001 | P1 | Kotlin, AGP, Gradle và Compose Compiler không tương thích | Dùng version catalog, BOM, CI build sạch trên JDK chuẩn |
| BLD-002 | P1 | Dependency trùng phiên bản gây `Duplicate class` | Dependency insight, loại exclude có kiểm soát |
| BLD-003 | P1 | R8/ProGuard loại nhầm class serialization/Room/DI | Test release build, keep rules tối thiểu |
| BLD-004 | P1 | Debug chạy được nhưng release crash | Internal testing luôn dùng signed release |
| BLD-005 | P2 | KSP/KAPT tăng thời gian build hoặc lỗi incremental | Ưu tiên KSP, khóa phiên bản, cache CI |
| BLD-006 | P1 | Native library chỉ có một ABI | Build/test arm64-v8a, armeabi-v7a nếu còn hỗ trợ, x86_64 emulator |
| BLD-007 | P1 | Kích thước APK tăng mạnh do model AI/native libs | Dynamic download model, split ABI, đo APK Analyzer |
| BLD-008 | P2 | License thư viện không phù hợp thương mại | Lập SBOM và kiểm tra GPL/AGPL trước khi dùng |
| BLD-009 | P1 | CVE trong dependency | Dependabot/Renovate, scan định kỳ, pin patch bảo mật |
| BLD-010 | P2 | Build không tái lập | Khóa dependency, wrapper Gradle, CI container |
| BLD-011 | P1 | Secret/API key bị commit | Secret scanning, `.env`/local.properties, rotate key |
| BLD-012 | P2 | Cache Gradle lỗi tạo build không nhất quán | CI có job clean build định kỳ |
| BLD-013 | P2 | Nâng target SDK làm thay đổi hành vi | Regression matrix theo API level |
| BLD-014 | P1 | Package name đổi sau khi phát hành | Chốt package sớm và giữ vĩnh viễn |
| BLD-015 | P0 | Mất release signing key | Backup mã hóa ngoại tuyến, quy trình hai người, key rotation plan |

## 3.2 Quyền microphone và foreground service

| ID | Mức | Lỗi/rủi ro | Cách phòng ngừa/kiểm thử |
|---|---|---|---|
| MIC-001 | P1 | Người dùng từ chối `RECORD_AUDIO` | Permission UX, giải thích, mở Settings khi “Don’t ask again” |
| MIC-002 | P1 | Start microphone foreground service khi app đang background | Chỉ khởi động từ hành động người dùng khi activity visible |
| MIC-003 | P1 | Thiếu `foregroundServiceType="microphone"` | Manifest test theo API 34+ |
| MIC-004 | P1 | Thiếu quyền foreground service microphone | Lint + instrumentation test |
| MIC-005 | P1 | Notification channel bị tắt hoặc notification không hiện | Kiểm tra notification permission và fallback UI |
| MIC-006 | P1 | `ForegroundServiceStartNotAllowedException` | Catch, log mã lỗi, quay lại màn hình recorder |
| MIC-007 | P1 | `SecurityException` do quyền bị thu hồi giữa phiên | Theo dõi permission, kết thúc file an toàn |
| MIC-008 | P0 | App tiếp tục báo đang ghi nhưng microphone đã dừng | Watchdog theo số byte PCM và timestamp |
| MIC-009 | P1 | Dịch vụ bị hệ thống kill do OEM battery optimization | Checkpoint segment, hướng dẫn battery setting có chọn lọc |
| MIC-010 | P2 | Người dùng force-stop app | Hiển thị cảnh báo rằng force-stop sẽ dừng ghi |
| MIC-011 | P1 | Notification action Pause/Stop gửi trùng intent | Idempotent command + mutex |
| MIC-012 | P1 | Service lifecycle lệch với UI lifecycle | Single source of truth trong service/repository |
| MIC-013 | P2 | Process chết, UI mở lại hiển thị sai trạng thái | Khôi phục session từ DB + file checkpoint |
| MIC-014 | P1 | Khởi động ghi từ widget/shortcut bị Android chặn | Mở activity trung gian visible trước khi start service |
| MIC-015 | P2 | Microphone privacy toggle của hệ thống đang tắt | Phát hiện silence bất thường, chỉ dẫn người dùng |

## 3.3 AudioRecord và thiết bị âm thanh

| ID | Mức | Lỗi/rủi ro | Cách phòng ngừa/kiểm thử |
|---|---|---|---|
| AUD-001 | P1 | `AudioRecord` khởi tạo thất bại | Thử cấu hình fallback 48k/44.1k/16k và mono |
| AUD-002 | P1 | Buffer nhỏ gây underrun, audio giật | Dùng `getMinBufferSize` nhân hệ số an toàn |
| AUD-003 | P0 | Ghi file rỗng hoặc 0 byte | Kiểm tra byte count, header, duration trước khi hoàn tất |
| AUD-004 | P0 | File hỏng khi crash/mất pin | Segment nhỏ + atomic rename + recovery scan |
| AUD-005 | P1 | Sai sample rate làm giọng nhanh/chậm | Ghi sample rate trong metadata, test decoder |
| AUD-006 | P1 | Sai endianness/PCM encoding | Unit test vector PCM |
| AUD-007 | P1 | Stereo/mono mapping sai | Chuẩn hóa internal format mono |
| AUD-008 | P2 | Mức âm quá nhỏ | Metering, gain warning, AGC có điều kiện |
| AUD-009 | P2 | Clipping do nguồn quá lớn | Peak detector, limiter hậu kỳ |
| AUD-010 | P2 | Điện thoại đặt xa làm giọng mờ | Quality indicator và hướng dẫn vị trí mic |
| AUD-011 | P1 | Bluetooth chuyển profile làm chất lượng thấp | Hiển thị nguồn hiện tại, test SCO/A2DP behavior |
| AUD-012 | P1 | Rút tai nghe giữa phiên | Audio device callback, chuyển nguồn có đánh dấu timeline |
| AUD-013 | P1 | Cuộc gọi đến chiếm microphone | Audio focus/device callback, pause/stop an toàn |
| AUD-014 | P1 | App camera/voice khác cùng dùng mic | Xử lý audio input sharing và thông báo xung đột |
| AUD-015 | P2 | Mic trước/sau khác chất lượng theo OEM | Không hard-code source; benchmark thiết bị |
| AUD-016 | P2 | Hardware noise suppressor tạo artefact | Cho tắt, A/B test, fallback software |
| AUD-017 | P2 | `NoiseSuppressor.create()` trả `null` | Capability check |
| AUD-018 | P2 | Echo canceller làm méo khi không cần | Chỉ bật trong scenario phát loa |
| AUD-019 | P2 | AGC “bơm” tiếng nền khi im lặng | Gate/VAD và preset Balanced |
| AUD-020 | P1 | CPU cao làm mất frame audio | Thread ưu tiên audio, tránh cấp phát trong loop |
| AUD-021 | P1 | GC pause trong đường ghi PCM | Buffer pool, object reuse |
| AUD-022 | P1 | Deadlock giữa pause/stop/write | State machine + structured concurrency |
| AUD-023 | P1 | Bấm Stop liên tục tạo double finalize | Atomic state transition |
| AUD-024 | P2 | Timer UI lệch duration file | Timer từ sample count, không chỉ wall clock |
| AUD-025 | P2 | Clock thay đổi khi người dùng đổi giờ | Dùng elapsed realtime cho duration |

## 3.4 Encoder, container và file audio

| ID | Mức | Lỗi/rủi ro | Cách phòng ngừa/kiểm thử |
|---|---|---|---|
| ENC-001 | P1 | AAC encoder không có hoặc lỗi OEM | Codec capability query, fallback PCM/WAV |
| ENC-002 | P1 | MediaCodec trả format chậm | Xử lý state đúng, timeout có log |
| ENC-003 | P0 | M4A chưa ghi atom cuối nên không phát | Fragment/segment hoặc repair/finalize khi recovery |
| ENC-004 | P1 | Timestamp không đơn điệu | Tính PTS từ sample count |
| ENC-005 | P1 | Gap/overlap khi pause-resume | Timeline mapping rõ và test waveform |
| ENC-006 | P1 | Ghép segment tạo tiếng click | Cắt ở zero crossing hoặc crossfade ngắn |
| ENC-007 | P2 | Bitrate quá thấp | Preset speech 48–96 kbps và nghe benchmark |
| ENC-008 | P2 | Bitrate quá cao gây tốn bộ nhớ | Ước lượng dung lượng trước khi ghi |
| ENC-009 | P1 | Export format không được player khác hỗ trợ | Test VLC, Android, Windows, macOS |
| ENC-010 | P2 | Metadata title không hỗ trợ Unicode | UTF-8 và test tiếng Việt |
| ENC-011 | P1 | Tên file chứa ký tự cấm | Dùng ID nội bộ, title chỉ là metadata |
| ENC-012 | P1 | Ghi đè file khi trùng tên | UUID + atomic file creation |
| ENC-013 | P1 | SAF URI mất quyền truy cập | Persistable URI permission, copy vào app storage khi cần |
| ENC-014 | P2 | Export bị hủy giữa chừng để lại file rác | Temp file + cleanup worker |
| ENC-015 | P1 | File >4 GB trên filesystem/format không phù hợp | Segment và cảnh báo theo dung lượng |

## 3.5 Bộ nhớ, dung lượng và quản lý file

| ID | Mức | Lỗi/rủi ro | Cách phòng ngừa/kiểm thử |
|---|---|---|---|
| STO-001 | P0 | Hết dung lượng trong lúc ghi | Preflight + theo dõi dung lượng + stop an toàn |
| STO-002 | P0 | DB ghi có bản ghi nhưng file không tồn tại | Transactional metadata + reconciliation job |
| STO-003 | P0 | File tồn tại nhưng DB mất record | Startup orphan scanner |
| STO-004 | P1 | Cache AI/audio chiếm đầy máy | Quota cache + LRU + màn hình Storage |
| STO-005 | P1 | Người dùng xóa file ngoài app | Detect missing file, giữ metadata với trạng thái Missing |
| STO-006 | P1 | Backup/restore tạo duplicate | Content hash + conflict strategy |
| STO-007 | P1 | Xóa nhầm file gốc khi export | Không dùng chung path source/output |
| STO-008 | P1 | Thùng rác không dọn | Retention worker có điều kiện |
| STO-009 | P2 | File name lộ nội dung nhạy cảm | Dùng opaque UUID file name |
| STO-010 | P1 | Storage encryption làm chậm ghi | Mã hóa theo chunk sau khi ghi hoặc benchmark streaming encryption |
| STO-011 | P1 | Key mã hóa mất sau restore app | Thiết kế backup policy và thông báo không thể giải mã |
| STO-012 | P0 | Xóa account làm xóa cả local ngoài ý muốn | Tách rõ delete cloud/delete local |
| STO-013 | P2 | Dung lượng hiển thị sai do đơn vị | Dùng IEC/SI nhất quán |
| STO-014 | P1 | WorkManager cleanup chạy khi đang ghi | Lock session đang active |
| STO-015 | P1 | Path traversal từ tên import | Không nối đường dẫn từ input người dùng |

## 3.6 Room, DataStore và migration

| ID | Mức | Lỗi/rủi ro | Cách phòng ngừa/kiểm thử |
|---|---|---|---|
| DB-001 | P0 | Migration làm mất database | Migration test từ mọi phiên bản hỗ trợ |
| DB-002 | P0 | Dùng destructive migration production | Cấm bằng build check |
| DB-003 | P1 | Schema và file metadata không đồng bộ | Reconciliation + foreign keys |
| DB-004 | P1 | Query transcript lớn gây OOM | Paging, FTS, load theo vùng timeline |
| DB-005 | P1 | N+1 query trong Compose | Relation/query tối ưu, profiler |
| DB-006 | P2 | Flow phát quá nhiều update | `distinctUntilChanged`, granular query |
| DB-007 | P1 | Concurrent edit gây lost update | Version column hoặc optimistic locking |
| DB-008 | P1 | Transaction quá dài block recorder | Không ghi blob audio vào Room |
| DB-009 | P1 | Corrupt database | Backup metadata định kỳ, open callback xử lý |
| DB-010 | P2 | FTS tokenizer tiếng Việt tìm kiếm kém | Benchmark tokenizer, normalized text |
| DB-011 | P2 | DataStore nhiều writer | Single repository owner |
| DB-012 | P1 | Trial counter bị rollback từ backup | Signed state + server reconciliation khi online |
| DB-013 | P1 | Date/time timezone sai | Lưu Instant UTC; render theo locale |
| DB-014 | P2 | Sort không ổn định | Secondary key ID/createdAt |
| DB-015 | P1 | Xóa recording nhưng insight/task còn | Foreign key cascade có kiểm thử |

## 3.7 Compose UI, navigation và state

| ID | Mức | Lỗi/rủi ro | Cách phòng ngừa/kiểm thử |
|---|---|---|---|
| UI-001 | P1 | Recomposition quá mức làm waveform giật | Stable model, draw canvas, sample UI updates |
| UI-002 | P1 | Main thread đọc/ghi file | StrictMode, dispatcher IO |
| UI-003 | P1 | State recorder bị nhân đôi giữa service và ViewModel | Repository/state machine duy nhất |
| UI-004 | P2 | Rotate màn hình mất dialog/edit | `SavedStateHandle` |
| UI-005 | P1 | Back làm dừng ghi ngoài ý muốn | Confirm và service độc lập navigation |
| UI-006 | P2 | Bottom sheet che nút Stop | Insets + accessibility test |
| UI-007 | P2 | Font scale lớn vỡ layout | Test 200% font, responsive constraint |
| UI-008 | P2 | Dark mode tương phản thấp | WCAG contrast audit |
| UI-009 | P2 | Dynamic color làm thương hiệu khó đọc | Fallback palette và contrast guard |
| UI-010 | P1 | Double tap tạo hai session | Debounce + idempotent start |
| UI-011 | P2 | Loading vô hạn khi job fail | Timeout + retry/cancel/error state |
| UI-012 | P2 | Empty state không hướng dẫn | CTA rõ ràng |
| UI-013 | P2 | Snackbar quan trọng biến mất nhanh | Persistent banner cho lỗi ghi âm |
| UI-014 | P2 | TalkBack không đọc nút icon | Content descriptions, semantics |
| UI-015 | P2 | Touch target nhỏ | Tối thiểu 48dp |
| UI-016 | P2 | Waveform không hỗ trợ RTL | Test layout direction |
| UI-017 | P2 | Scroll transcript nhảy khi partial text cập nhật | Anchor theo segment ID |
| UI-018 | P1 | UI hiển thị “saved” trước khi flush file | Chỉ xác nhận sau durable checkpoint |
| UI-019 | P2 | Lifecycle collect gây leak | `collectAsStateWithLifecycle` |
| UI-020 | P1 | Navigation deep link mở record không tồn tại | Guard và error screen |

## 3.8 Đa ngôn ngữ và locale

| ID | Mức | Lỗi/rủi ro | Cách phòng ngừa/kiểm thử |
|---|---|---|---|
| I18N-001 | P2 | Hard-code chuỗi trong code | Lint và string resources |
| I18N-002 | P2 | Thiếu bản dịch làm hiện key/English ngoài ý muốn | Translation coverage test |
| I18N-003 | P2 | Placeholder thứ tự sai | Positional formatting và test locale |
| I18N-004 | P2 | Số nhiều sai | plurals resource |
| I18N-005 | P2 | Ngày giờ hiển thị sai locale | ICU/Java time formatter |
| I18N-006 | P2 | Title AI dùng sai ngôn ngữ | Tách UI locale và output language |
| I18N-007 | P1 | Đổi ngôn ngữ restart làm mất recorder UI | Service giữ session, activity recreate an toàn |
| I18N-008 | P2 | Tiếng Việt bị cắt do chuỗi dài | Pseudolocalization + screenshot tests |
| I18N-009 | P2 | Search không bỏ dấu | Normalize tùy lựa chọn người dùng |
| I18N-010 | P2 | Model không nhận đúng vi/en code-switch | Auto-detect + cho chọn thủ công |

## 3.9 Playback và audio focus

| ID | Mức | Lỗi/rủi ro | Cách phòng ngừa/kiểm thử |
|---|---|---|---|
| PLAY-001 | P1 | Phát không được file vừa ghi | Validate container trước khi đưa vào library |
| PLAY-002 | P2 | Seek lệch waveform/transcript | Chuẩn timeline chung milliseconds |
| PLAY-003 | P2 | Speed làm giọng khó nghe | Time-stretch engine phù hợp |
| PLAY-004 | P1 | Phát audio trong lúc đang ghi gây echo | Chặn hoặc cảnh báo rõ |
| PLAY-005 | P1 | Không nhả audio focus | Lifecycle player và focus callback |
| PLAY-006 | P2 | Tai nghe Bluetooth reconnect làm player đứng | Device callback + retry |
| PLAY-007 | P2 | Notification player và recorder xung đột | Separate session IDs/channels |
| PLAY-008 | P2 | Resume sai vị trí | Persist position có debounce |
| PLAY-009 | P1 | Decoder OEM lỗi | Media3 fallback và test corpus |
| PLAY-010 | P2 | Trim preview không đúng output | Preview áp cùng edit decision list |

## 3.10 Editor và xử lý không phá hủy

| ID | Mức | Lỗi/rủi ro | Cách phòng ngừa/kiểm thử |
|---|---|---|---|
| EDT-001 | P0 | Edit ghi đè file gốc | Source immutable, output riêng |
| EDT-002 | P1 | Trim start > end | Constraint và validation |
| EDT-003 | P1 | Các range edit chồng nhau | Normalize edit decision list |
| EDT-004 | P1 | Undo/redo sai thứ tự | Command stack unit tests |
| EDT-005 | P2 | Waveform cache không cập nhật | Cache key gồm edit version |
| EDT-006 | P1 | Export hủy giữa chừng treo lock | Cooperative cancellation + finally cleanup |
| EDT-007 | P1 | Noise reduction tạo artefact mạnh | Preset, preview A/B, không mặc định Strong |
| EDT-008 | P2 | Normalize làm clipping | True peak limiter |
| EDT-009 | P2 | VAD cắt mất âm đầu/cuối | Padding và threshold test |
| EDT-010 | P2 | Ghép đoạn khác sample rate | Resample trước merge |
| EDT-011 | P1 | Native DSP crash | Boundary validation và fallback |
| EDT-012 | P1 | DSP quá chậm/hao pin | Benchmark theo phút audio và thermal |
| EDT-013 | P2 | Process background bị giới hạn | WorkManager/foreground processing khi cần |
| EDT-014 | P2 | Người dùng rời app tưởng đã export | Progress notification + persisted job |
| EDT-015 | P1 | Timestamp transcript không cập nhật sau cut | Mapping old-to-new timeline |

## 3.11 Phiên âm và AI local/cloud

| ID | Mức | Lỗi/rủi ro | Cách phòng ngừa/kiểm thử |
|---|---|---|---|
| AI-001 | P1 | Không có mạng nhưng app gửi job cloud | Network constraint, queue local |
| AI-002 | P1 | Upload audio thất bại giữa chừng | Multipart/resumable upload + checksum |
| AI-003 | P0 | Upload nhầm recording | Bound job ID + checksum + user confirmation |
| AI-004 | P0 | Audio bị upload khi chưa đồng ý | Explicit consent gate và audit event |
| AI-005 | P1 | Transcript sai ngôn ngữ | Language hint + retry provider |
| AI-006 | P1 | Nói chồng làm transcript sai | Confidence/evidence và cho sửa |
| AI-007 | P1 | Diarization gán nhầm speaker | Không suy ra danh tính; label Speaker 1/2 |
| AI-008 | P1 | AI bịa task/deadline/assignee | Structured output + evidence timestamp + confirmation |
| AI-009 | P1 | JSON output sai schema | Schema validation, repair retry, fallback text |
| AI-010 | P1 | Context dài vượt token | Chunk/map-reduce/hierarchical summary |
| AI-011 | P2 | Summary trùng lặp | Dedup semantic + merge rules |
| AI-012 | P1 | Prompt injection trong transcript | Transcript là untrusted data, system rule cố định |
| AI-013 | P0 | Model/provider lưu dữ liệu trái cam kết | DPA, retention settings, provider review |
| AI-014 | P1 | API key lộ trong APK | Backend proxy hoặc BYOK trong Keystore với cảnh báo |
| AI-015 | P1 | Chi phí AI tăng đột biến | Quota, cost ceiling, provider metrics |
| AI-016 | P1 | Retry tính quota nhiều lần | Idempotency key và ledger |
| AI-017 | P2 | Partial transcript nhấp nháy | Stable segment reconciliation |
| AI-018 | P1 | Offline model không đủ RAM | Capability benchmark trước download/run |
| AI-019 | P1 | Model download hỏng | Checksum, resume, atomic install |
| AI-020 | P1 | Model chiếm quá nhiều storage | Hiển thị size, removable model packs |
| AI-021 | P1 | Thermal throttling làm máy nóng | Rate control, background mode, warning |
| AI-022 | P2 | Battery drain cao | Battery benchmark và user-controlled processing |
| AI-023 | P2 | Local inference bị hệ thống kill | Checkpoint progress |
| AI-024 | P1 | Transcript không khớp timestamp | Word/segment alignment validation |
| AI-025 | P2 | Punctuation kém tiếng Việt | Post-processing có benchmark, không sửa nội dung tùy tiện |
| AI-026 | P1 | Tên riêng bị sửa sai | Custom vocabulary + user dictionary |
| AI-027 | P1 | Code-switch vi/en thất bại | Mixed-language test corpus |
| AI-028 | P1 | Provider outage | Multi-provider adapter hoặc graceful retry |
| AI-029 | P1 | Kết quả AI tới sau khi recording đã xóa | Job cancellation + tombstone check |
| AI-030 | P2 | Người dùng không hiểu nội dung đã gửi cloud | Disclosure theo từng tác vụ |

## 3.12 Mạng, API và đồng bộ

| ID | Mức | Lỗi/rủi ro | Cách phòng ngừa/kiểm thử |
|---|---|---|---|
| NET-001 | P1 | Timeout trên mạng yếu | Timeout theo loại request, retry backoff |
| NET-002 | P1 | Retry storm | Exponential backoff + jitter |
| NET-003 | P1 | Upload qua mobile data ngoài ý muốn | Setting Wi‑Fi only/default consent |
| NET-004 | P1 | Đổi Wi‑Fi/4G làm mất upload | Resume + range/multipart |
| NET-005 | P1 | Captive portal báo có mạng nhưng không Internet | Validate endpoint |
| NET-006 | P1 | TLS/certificate lỗi do ngày giờ máy sai | Error rõ, không bypass TLS |
| NET-007 | P0 | Chấp nhận certificate không an toàn | Không trust-all SSL |
| NET-008 | P1 | API version không tương thích app cũ | Versioned API + backward compatibility |
| NET-009 | P1 | Server trả 200 nhưng payload lỗi | Schema validation |
| NET-010 | P1 | Token hết hạn khi offline | Signed offline entitlement + refresh khi có mạng |
| NET-011 | P1 | Đồng bộ ghi đè dữ liệu local mới hơn | Conflict version/timestamp/user choice |
| NET-012 | P1 | Duplicate recording sau reconnect | Client-generated UUID + idempotency |
| NET-013 | P2 | Clock skew ảnh hưởng expiry | Server-issued epoch + grace window |
| NET-014 | P1 | Logout khi upload đang chạy | Policy rõ: cancel hoặc complete under session |
| NET-015 | P1 | API trả dữ liệu user khác | Authorization kiểm tra owner ở mọi endpoint |

## 3.13 Guest mode, đăng nhập và nâng cấp tài khoản

| ID | Mức | Lỗi/rủi ro | Cách phòng ngừa/kiểm thử |
|---|---|---|---|
| ACC-001 | P0 | App bắt đăng nhập trước khi ghi | Automated cold-start offline test |
| ACC-002 | P1 | Login lỗi làm mất dữ liệu guest | Link account, không replace database |
| ACC-003 | P1 | Merge guest/cloud duplicate | Conflict preview và dedup |
| ACC-004 | P1 | Đăng xuất xóa local trái ý | Local data độc lập account |
| ACC-005 | P1 | Xóa account không xóa cloud | Deletion job + confirmation status |
| ACC-006 | P1 | Email verification link mở sai app | App links + fallback web |
| ACC-007 | P1 | Token lưu plaintext | Keystore-backed encrypted storage |
| ACC-008 | P1 | Session fixation/replay | Short-lived access token, rotate refresh token |
| ACC-009 | P2 | Đổi email mất license | License theo account ID, support recovery |
| ACC-010 | P1 | Một license chia sẻ vô hạn | Device limit + revoke management, tránh fingerprint xâm phạm |

## 3.14 Trial và license offline

| ID | Mức | Lỗi/rủi ro | Cách phòng ngừa/kiểm thử |
|---|---|---|---|
| LIC-001 | P1 | Xóa app reset trial | Chấp nhận rủi ro hoặc server/device attestation khi online |
| LIC-002 | P1 | Chỉnh clock kéo dài subscription | Signed expiry theo server epoch, periodic verification |
| LIC-003 | P0 | Private signing key nằm trong app | Chỉ public key trong app; private key ở server/offline signer |
| LIC-004 | P1 | License code bị đoán/brute force | Random entropy cao, rate limit, one-time redemption |
| LIC-005 | P1 | Token bị copy sang máy khác | Bind installation public key/device installation ID |
| LIC-006 | P1 | Reset máy làm mất Pro | Khôi phục qua account hoặc support/license receipt |
| LIC-007 | P1 | Offline quá lâu nhưng thuê bao đã hoàn tiền | Grace window phù hợp và revoke khi reconnect |
| LIC-008 | P1 | Server license downtime khóa khách hợp lệ | Cached entitlement + grace period |
| LIC-009 | P2 | UI hiển thị Pro nhưng feature gate vẫn Free | Central entitlement repository |
| LIC-010 | P1 | Hai source billing xung đột | Canonical entitlement ledger server-side |
| LIC-011 | P1 | Trial dùng cloud gây chi phí spam | Device/app attestation khi online + quota per installation/IP/account |
| LIC-012 | P2 | License vĩnh viễn nhầm thành subscription | Product type explicit trong signed claims |
| LIC-013 | P1 | Key rotation làm token cũ vô hiệu | `kid` và nhiều public keys |
| LIC-014 | P1 | Token parser chấp nhận alg `none` | Thuật toán cố định, strict verification |
| LIC-015 | P1 | Root/hook bypass gate | Không thể chống tuyệt đối; bảo vệ server resources và phát hiện bất thường |

## 3.15 Thanh toán trên website

| ID | Mức | Lỗi/rủi ro | Cách phòng ngừa/kiểm thử |
|---|---|---|---|
| PAY-001 | P0 | Cấp Pro trước khi thanh toán xác nhận | Chỉ webhook/server verified cấp entitlement |
| PAY-002 | P0 | Webhook giả | Verify signature và timestamp |
| PAY-003 | P1 | Webhook gửi trùng | Idempotent event processing |
| PAY-004 | P1 | Thanh toán thành công nhưng chưa cấp license | Reconciliation job + trang tra cứu đơn |
| PAY-005 | P1 | Refund nhưng Pro không thu hồi | Handle refund/chargeback events |
| PAY-006 | P1 | Đơn vị tiền/decimal sai | Integer minor units |
| PAY-007 | P1 | Giá website khác backend | Product catalog canonical server-side |
| PAY-008 | P1 | Coupon bị lạm dụng | Scope, expiry, usage limit |
| PAY-009 | P0 | Lưu dữ liệu thẻ trực tiếp | Dùng cổng thanh toán; không tự xử lý card data |
| PAY-010 | P1 | Hóa đơn/thuế không phù hợp | Quy trình kế toán và điều khoản địa phương |
| PAY-011 | P1 | Email license vào spam | Trang receipt + account lookup |
| PAY-012 | P1 | Người dùng nhập sai email | Confirm email và order lookup |
| PAY-013 | P1 | Chuyển khoản thủ công nhầm nội dung | Unique order code và admin reconciliation |
| PAY-014 | P1 | Mua từ web nhưng app Play Store vi phạm policy vùng | Tách chiến lược distribution và rà soát chính sách trước khi lên Play |
| PAY-015 | P2 | Contact-only purchase không scale | Admin dashboard/order automation roadmap |

## 3.16 Website marketing, blog và tải APK

| ID | Mức | Lỗi/rủi ro | Cách phòng ngừa/kiểm thử |
|---|---|---|---|
| WEB-001 | P0 | Link APK bị thay bằng file độc hại | Immutable release, checksum SHA-256, HTTPS, access control |
| WEB-002 | P0 | Website bị chiếm quyền và thay download | MFA, least privilege, deploy protection |
| WEB-003 | P1 | APK chưa ký release | CI verify signature trước publish |
| WEB-004 | P1 | APK website và version app không khớp | Manifest release JSON |
| WEB-005 | P1 | Không hiển thị checksum/version/date | Download page bắt buộc metadata |
| WEB-006 | P1 | Browser cache giữ APK cũ | Versioned filename và cache headers |
| WEB-007 | P1 | MIME type APK sai | `application/vnd.android.package-archive` |
| WEB-008 | P1 | Vercel giới hạn/băng thông tốn kém khi host APK lớn | Dùng object storage/CDN cho binary |
| WEB-009 | P2 | Contact form spam | CAPTCHA/rate limit khi thêm backend |
| WEB-010 | P1 | Form gửi dữ liệu nhưng không có consent | Privacy notice + minimal fields |
| WEB-011 | P2 | SEO trùng title/description | Metadata theo trang |
| WEB-012 | P2 | Blog không có sitemap/robots | Generate sitemap và robots |
| WEB-013 | P1 | Dependency Next.js có lỗ hổng | Dùng Active LTS patched và cập nhật định kỳ |
| WEB-014 | P1 | Secret payment/email lộ client-side | Chỉ `NEXT_PUBLIC_*` cho dữ liệu công khai |
| WEB-015 | P2 | Link mua hàng dùng placeholder | Deploy checklist bắt buộc thay env/contact |
| WEB-016 | P2 | Layout mobile vỡ | Responsive test 320px–desktop |
| WEB-017 | P2 | Dark mode flash | Theme script/class trước paint hoặc chấp nhận minimal FOUC |
| WEB-018 | P1 | Analytics thu thập quá mức | Consent và privacy-friendly analytics |
| WEB-019 | P1 | Nội dung quảng cáo “100% chính xác” | Không cam kết tuyệt đối; ghi AI có thể sai |
| WEB-020 | P1 | Tải APK bị hệ thống cảnh báo cài nguồn không xác định | Hướng dẫn minh bạch; không hướng dẫn bỏ qua cảnh báo an toàn |
| WEB-021 | P1 | Developer/package chưa xác minh khi quy định mới áp dụng | Hoàn thành Android Developer Verification và đăng ký package |
| WEB-022 | P2 | Domain hết hạn | Auto-renew + MFA + recovery contact |
| WEB-023 | P1 | DNS hijack | Registrar lock, DNSSEC nếu hỗ trợ |
| WEB-024 | P2 | Open Graph ảnh/link sai | Preview test |
| WEB-025 | P1 | CSP quá lỏng | Security headers và tránh inline script không cần thiết |

## 3.17 Vercel và quy trình deploy

| ID | Mức | Lỗi/rủi ro | Cách phòng ngừa/kiểm thử |
|---|---|---|---|
| VER-001 | P1 | Build khác local do Node version | `engines.node`, `.nvmrc`, CI same version |
| VER-002 | P1 | Environment variables thiếu | Startup/build validation |
| VER-003 | P1 | Preview dùng dữ liệu production | Env tách Preview/Production |
| VER-004 | P1 | Serverless function timeout | Không xử lý file audio/AI dài trên Vercel function |
| VER-005 | P1 | Upload APK vượt giới hạn phù hợp | Binary ở object storage, website chỉ link |
| VER-006 | P2 | Caching trang giá cũ | Revalidate hoặc static redeploy rõ ràng |
| VER-007 | P1 | Rollback không biết release nào | Git tag + deployment notes |
| VER-008 | P1 | Domain trỏ nhầm project | Domain ownership/MFA/checklist |
| VER-009 | P2 | Build log lộ secret | Không echo env, scrub logs |
| VER-010 | P1 | Next.js security patch chậm | Monthly dependency maintenance |

## 3.18 Bảo mật ứng dụng

| ID | Mức | Lỗi/rủi ro | Cách phòng ngừa/kiểm thử |
|---|---|---|---|
| SEC-001 | P0 | Exported Activity/Service/Receiver ngoài ý muốn | Explicit `android:exported`, manifest audit |
| SEC-002 | P0 | Intent injection điều khiển recorder | Signature/internal permission, validate extras |
| SEC-003 | P0 | FileProvider path quá rộng | Chỉ share export dir cụ thể |
| SEC-004 | P0 | SQL injection | Room parameterized queries |
| SEC-005 | P0 | Path traversal khi import/export | Canonical path và URI APIs |
| SEC-006 | P0 | Log chứa transcript/token/path | Production redaction |
| SEC-007 | P0 | Clipboard chứa license/token | Không copy secret mặc định |
| SEC-008 | P1 | Screenshot hiển thị nội dung nhạy cảm | Tùy chọn secure screen cho transcript |
| SEC-009 | P0 | Backup Android chứa token/license nhạy cảm | Backup rules và encrypted token |
| SEC-010 | P0 | WebView mua hàng cấu hình nguy hiểm | Ưu tiên external browser/custom tab |
| SEC-011 | P0 | Deep link open redirect | Allowlist host/path |
| SEC-012 | P0 | Insecure deserialization | Strict schema, no arbitrary class |
| SEC-013 | P1 | Rooted device can tamper | Threat model rõ; protect server assets, không hứa tuyệt đối |
| SEC-014 | P0 | Native library memory corruption | Fuzzing input, cập nhật libs |
| SEC-015 | P1 | Clipboard/notification lộ title nhạy cảm | Privacy setting “Hide sensitive content” |
| SEC-016 | P0 | PendingIntent mutable không cần thiết | Immutable flags |
| SEC-017 | P1 | Old TLS/API endpoint vẫn dùng | Network security config |
| SEC-018 | P0 | Server IDOR đọc recording khác | Object-level authorization tests |
| SEC-019 | P0 | Signed URL sống quá lâu | Short TTL, method/content constraints |
| SEC-020 | P0 | Không xóa temp decrypted file | Secure cleanup và app-private storage |

## 3.19 Quyền riêng tư và pháp lý

| ID | Mức | Lỗi/rủi ro | Cách phòng ngừa/kiểm thử |
|---|---|---|---|
| LEG-001 | P0 | Ghi âm người khác không có sự đồng ý | Consent reminder và điều khoản sử dụng |
| LEG-002 | P0 | Chính sách quyền riêng tư không mô tả audio/transcript | Data inventory và cập nhật policy |
| LEG-003 | P0 | Cloud xử lý ở quốc gia khác không thông báo | Vendor/subprocessor disclosure |
| LEG-004 | P0 | Không có chức năng xóa dữ liệu cloud | Delete/export workflow |
| LEG-005 | P0 | Retention vô hạn | Retention policy và lựa chọn auto-delete |
| LEG-006 | P1 | Thu thập analytics khi chưa consent nơi yêu cầu | Consent management |
| LEG-007 | P0 | Dùng giọng để nhận dạng sinh trắc học ngoài phạm vi | Không suy danh tính; review riêng nếu thêm voiceprint |
| LEG-008 | P0 | Quảng cáo gây hiểu nhầm “AI chính xác tuyệt đối” | Disclaimer và human review |
| LEG-009 | P1 | Dùng logo/font/image không có license | Asset provenance registry |
| LEG-010 | P0 | Tên thương hiệu xâm phạm trademark | Tra cứu chuyên nghiệp trước đăng ký/marketing lớn |
| LEG-011 | P1 | Điều khoản refund mơ hồ | Terms và quy trình hỗ trợ rõ |
| LEG-012 | P1 | Bán account dùng chung trái điều khoản | License terms cụ thể |
| LEG-013 | P0 | Không có thông tin pháp nhân/người bán | Website footer, invoice/contact phù hợp |
| LEG-014 | P1 | Trẻ em sử dụng và ghi âm dữ liệu nhạy cảm | Age positioning và policy review |
| LEG-015 | P0 | Không khai báo data safety nếu lên store | Store declaration khớp hành vi thực tế |

## 3.20 Hiệu năng, pin và nhiệt

| ID | Mức | Lỗi/rủi ro | Cách phòng ngừa/kiểm thử |
|---|---|---|---|
| PERF-001 | P1 | ANR khi finalize file dài | IO background, progress |
| PERF-002 | P1 | OOM khi render waveform toàn file | Downsample/tile waveform |
| PERF-003 | P1 | OOM khi load transcript dài | Paging/virtualized list |
| PERF-004 | P1 | Battery drain khi ghi màn hình tắt | Profiling nhiều thiết bị |
| PERF-005 | P1 | Máy nóng khi denoise/transcribe local | Thermal API/queue/throttle |
| PERF-006 | P2 | Startup chậm vì scan toàn bộ file | Incremental index/background reconciliation |
| PERF-007 | P1 | Database vacuum/block khi recording | Maintenance khi idle/charging |
| PERF-008 | P2 | Compose jank waveform | Macrobenchmark và frame metrics |
| PERF-009 | P2 | Download model làm app lag | Background streaming và progress |
| PERF-010 | P1 | WorkManager jobs cạnh tranh | Unique work và constraints |

## 3.21 OEM, phiên bản Android và khả năng tương thích

| ID | Mức | Lỗi/rủi ro | Cách phòng ngừa/kiểm thử |
|---|---|---|---|
| OEM-001 | P1 | Xiaomi/Oppo/Vivo kill service | Real-device soak tests, checkpoint |
| OEM-002 | P1 | Samsung codec/audio effect khác | Device matrix |
| OEM-003 | P2 | API tồn tại nhưng implementation lỗi | Capability + runtime fallback |
| OEM-004 | P1 | Android 8 notification/foreground khác API mới | Version branches và test |
| OEM-005 | P1 | Android 14+ foreground rules | Target SDK regression tests |
| OEM-006 | P1 | Android 17 background audio hardening | Theo dõi preview và test sớm |
| OEM-007 | P2 | Foldable/multi-window vỡ UI | Adaptive layout |
| OEM-008 | P2 | Tablet landscape recorder khó dùng | Responsive design |
| OEM-009 | P2 | Battery saver làm xử lý chậm | Persist job và thông báo |
| OEM-010 | P1 | ROM tùy biến trả audio timestamp sai | Sample-count source of truth |

## 3.22 Cài đặt APK trực tiếp và cập nhật

| ID | Mức | Lỗi/rủi ro | Cách phòng ngừa/kiểm thử |
|---|---|---|---|
| APK-001 | P1 | Người dùng chưa bật quyền cài từ nguồn này | Hướng dẫn theo Settings, không che giấu cảnh báo |
| APK-002 | P1 | APK mới ký key khác nên không update được | Một signing lineage/key ổn định |
| APK-003 | P0 | APK giả mạo cùng tên | Website HTTPS, checksum, verification, package registration |
| APK-004 | P1 | Update làm mất dữ liệu | Migration + backup/recovery tests |
| APK-005 | P1 | Downgrade schema hỏng | Chặn downgrade hoặc hỗ trợ rõ |
| APK-006 | P1 | In-app updater tải file không an toàn | HTTPS, signature/hash verify trước install intent |
| APK-007 | P2 | Download bị browser đổi tên `.zip` | MIME/filename headers |
| APK-008 | P1 | Version check server bị giả | Signed release manifest |
| APK-009 | P1 | Update bắt buộc khi offline | Free local không được khóa; chỉ cảnh báo trừ lỗ hổng nghiêm trọng |
| APK-010 | P1 | Android developer verification chưa hoàn tất | Đăng ký developer và package trước rollout toàn cầu |
| APK-011 | P2 | Người dùng không biết cài bản nào | Một nút latest + archive/changelog |
| APK-012 | P1 | Không có rollback khi release lỗi | Giữ bản stable trước và quy trình hotfix |

## 3.23 Testing và QA

| ID | Mức | Lỗi/rủi ro | Cách phòng ngừa/kiểm thử |
|---|---|---|---|
| QA-001 | P0 | Chỉ test ghi âm vài phút | Soak test 1h/4h/8h |
| QA-002 | P1 | Chỉ test emulator | Real devices/OEM matrix |
| QA-003 | P1 | Không test mất pin/process kill | Fault injection |
| QA-004 | P1 | Không test storage full | Fill-disk instrumentation |
| QA-005 | P1 | Không test quyền bị thu hồi | Permission automation |
| QA-006 | P1 | Không test release/R8 | Release smoke suite |
| QA-007 | P1 | Golden audio corpus quá nhỏ | Corpus yên tĩnh/ồn/giọng vùng miền/code-switch |
| QA-008 | P1 | Không đo WER/DER | Benchmark định lượng |
| QA-009 | P1 | Không test billing/license clock skew | Time simulation |
| QA-010 | P1 | Không test offline cold start | Airplane mode automated scenario |
| QA-011 | P2 | Screenshot test không font scale/dark | Matrix snapshots |
| QA-012 | P1 | Migration test thiếu version cũ | Export schema và test all paths |
| QA-013 | P1 | Không test accessibility | TalkBack, switch access, contrast |
| QA-014 | P2 | Flaky tests bị bỏ qua | Quarantine có deadline và owner |
| QA-015 | P1 | Không test website download integrity | CI hash/signature verification |

## 3.24 Observability và hỗ trợ khách hàng

| ID | Mức | Lỗi/rủi ro | Cách phòng ngừa/kiểm thử |
|---|---|---|---|
| OBS-001 | P1 | Crash log không có state recorder | Structured non-sensitive state breadcrumbs |
| OBS-002 | P0 | Log gửi transcript/audio | Redaction mặc định |
| OBS-003 | P1 | Không phân biệt lỗi codec/OEM/storage | Error taxonomy và device metadata tối thiểu |
| OBS-004 | P1 | Support không xác minh version/license | Diagnostics screen cho người dùng copy |
| OBS-005 | P2 | Không có export log riêng tư | User-consented sanitized diagnostic bundle |
| OBS-006 | P1 | Không cảnh báo cost/provider outage | Operational dashboard |
| OBS-007 | P1 | Không biết tỷ lệ recording failure | Local counters opt-in/aggregate privacy-safe |
| OBS-008 | P2 | Contact email không được theo dõi | Ticketing hoặc shared mailbox |
| OBS-009 | P1 | Không có incident/rollback playbook | Runbook release, key compromise, outage |
| OBS-010 | P1 | Bug mất dữ liệu không ưu tiên P0 | Severity SLA rõ |

---

# 4. State machine ghi âm bắt buộc

```text
IDLE
  -> PREPARING
  -> RECORDING
  -> PAUSED
  -> RECORDING
  -> FINALIZING
  -> SAVED

PREPARING/RECORDING/PAUSED/FINALIZING
  -> FAILED_RECOVERABLE
  -> RECOVERING
  -> SAVED_PARTIAL hoặc FAILED_FATAL
```

Mọi lệnh Start/Pause/Resume/Stop phải idempotent. UI không tự đoán trạng thái; UI quan sát state từ recorder service/repository.

# 5. Offline behavior matrix

| Tình huống | Hành vi đúng |
|---|---|
| Không mạng, chưa đăng nhập | Free hoạt động đầy đủ cục bộ |
| Không mạng, đang có Pro token hợp lệ | Pro local tiếp tục hoạt động |
| Không mạng, trial cloud | Hiển thị cần mạng; không trừ lượt |
| Mất mạng lúc upload | Giữ file local, resume sau |
| Server lỗi | Không ảnh hưởng recorder/library/editor local |
| Token hết hạn khi offline | Grace period; không khóa ngay dữ liệu người dùng |
| Đăng xuất | Giữ bản ghi local |
| Xóa account | Hỏi riêng xóa cloud và/hoặc local |

# 6. Kiến trúc module đề xuất

```text
:app
:core:model
:core:common
:core:designsystem
:core:database
:core:datastore
:core:audio
:core:media
:core:network
:core:security
:core:license
:core:ai
:feature:home
:feature:recorder
:feature:library
:feature:playback
:feature:editor
:feature:transcript
:feature:insights
:feature:pricing
:feature:settings
```

Không cần tách hết ngay MVP; có thể gộp module nhỏ nhưng giữ ranh giới interface.

# 7. Sàng lọc tên thương hiệu sơ bộ

## Tên làm việc được đề xuất: **Loomora**

- Ngắn: 7 ký tự.
- Gợi liên tưởng “voice/keynote/note”.
- Dễ thiết kế logo chữ V + waveform/note.
- Phát âm đề xuất: **Vây-nốt** / **Vay-note**.
- Tagline: **Record locally. Remember clearly.**
- Tagline tiếng Việt: **Ghi âm riêng tư. Ghi nhớ rõ ràng.**

Kết quả tìm kiếm web sơ bộ ngày 25/07/2026 chưa thấy ứng dụng ghi âm/AI nổi bật dùng chính xác “Loomora”. Tuy nhiên đây **không phải kết luận pháp lý**. Trước khi mua domain, in ấn hoặc chạy quảng cáo lớn cần:

1. Tra cứu nhãn hiệu Việt Nam theo nhóm 9, 38, 42 và có thể 35.
2. Tra cứu WIPO Global Brand Database.
3. Tra cứu USPTO/EUIPO nếu bán quốc tế.
4. Tra Google Play, App Store, GitHub, mạng xã hội và domain.
5. Nhờ đại diện sở hữu trí tuệ đánh giá khả năng tương tự về âm, chữ và ngành hàng.

## Các tên đã loại hoặc rủi ro cao khi sàng lọc nhanh

- HushNote: đã có nhiều sản phẩm ghi âm/transcription local-first.
- EchoBrief: đã có meeting recorder và dịch vụ AI summary.
- VoiceMint: đã có nhiều sản phẩm TTS/voice.
- Audivo/Audexa/Sonivo: đã có app/công ty audio.
- Mivox/Voxelo/Voxari/Notevia/Recory: đã được sử dụng bởi app hoặc doanh nghiệp.
- Vokira: có hồ sơ nhãn hiệu phần mềm AI tại EU.

## Tên dự phòng cần tra cứu sâu hơn

- Loomora
- MinuteMic
- Voxune
- VocaNest
- NotePulse

Không đăng ký package/domain dựa trên tên dự phòng trước khi tra cứu chính thức.

# 8. Website marketing cần có

- Home.
- Features.
- Local-first & Privacy.
- Pricing/Buy Pro.
- Download APK.
- Blog.
- FAQ.
- Contact.
- Privacy Policy.
- Terms.
- Changelog/checksum.

CTA chính:

- Download for Android.
- Try Free — No account required.
- Buy Pro / Contact Sales.

# 9. Checklist trước khi phát hành APK trên website

- [ ] Chốt tên pháp lý và trademark clearance.
- [ ] Chốt package name.
- [ ] Tạo và backup release signing key.
- [ ] Signed release APK.
- [ ] Verify signature trong CI.
- [ ] Tạo SHA-256 checksum.
- [ ] Đăng version, ngày phát hành và changelog.
- [ ] Privacy Policy và Terms.
- [ ] Consent reminder khi ghi âm.
- [ ] Android Developer Verification/package registration theo lộ trình hiện hành.
- [ ] Domain HTTPS, MFA, registrar lock.
- [ ] APK lưu ở object storage/CDN đáng tin cậy.
- [ ] Website download không dùng link có thể bị người khác thay đổi.
- [ ] Offline cold-start test.
- [ ] Storage-full, process-kill, 8-hour recording test.
- [ ] Migration test từ bản cũ.
- [ ] License restore/revoke/refund test.

# 10. Nguồn kỹ thuật cần theo dõi

- Android foreground-service restrictions: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- Foreground service microphone type: https://developer.android.com/about/versions/14/changes/fgs-types-required
- Android developer verification: https://developer.android.com/developer-verification/guides
- Next.js security releases: https://nextjs.org/blog
- Vercel Next.js deployment: https://vercel.com/docs/frameworks/full-stack/nextjs

---

## Kết luận

Kiến trúc nên coi **ghi âm và dữ liệu local là hệ thống cốt lõi**, còn cloud, tài khoản và AI là lớp tăng cường. Một sự cố mạng, server, đăng nhập hoặc license không được phép làm người dùng mất khả năng ghi âm, phát lại hoặc truy cập dữ liệu miễn phí đã lưu trên máy.

