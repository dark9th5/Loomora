# Loomora Offline Completion Kit

Bộ prompt này dùng để hoàn thiện repository Android hiện tại của Loomora theo hướng:

- Production-quality, không phải UI demo.
- Ghi âm, quản lý file, chỉnh sửa và phân tích cuộc họp theo kiểu local-first.
- Không gọi API để xử lý âm thanh, transcription, diarization, summary hoặc action items.
- `sherpa-onnx` là runtime chính cho VAD, speech enhancement, ASR và speaker diarization.
- `LiteRT-LM` là runtime chính cho tóm tắt và trích xuất nội dung cuộc họp bằng LLM trên thiết bị.
- Media3 Transformer/Composition được ưu tiên cho trim, ghép và export audio thật.
- WorkManager + Room quản lý pipeline dài, retry, cancellation và idempotency.
- License Pro có thể hoạt động offline bằng payload được ký số. Không tuyên bố hỗ trợ thu hồi tức thời khi thiết bị không kết nối mạng.

## Vì sao cần bộ này

Repository hiện đã có `Loomora_AI_Agent_Kit`, nhưng phần AI cũ định hướng hybrid/cloud, upload và backend job. Bộ này thay thế hướng đó bằng kiến trúc offline-only.

Các tài liệu cũ cần coi là superseded đối với AI:

- `Loomora_AI_Agent_Kit/docs/11_AI_PIPELINE.md`
- phần M7 trong `Loomora_AI_Agent_Kit/docs/17_ROADMAP.md`
- `Loomora_AI_Agent_Kit/prompts/07_TRANSCRIPT_AI_INSIGHTS.md`
- mọi yêu cầu upload/provider API cho transcription và meeting insights

## Cách đặt vào repo

Cách an toàn:

1. Copy nguyên thư mục `Loomora_Offline_Completion_Kit` vào root repository.
2. Mở `PASTE_FIRST.md`.
3. Dán prompt trong đó vào Cline, Claude Code, Codex hoặc agent đang dùng.
4. Sau audit, chạy từng file trong `prompts/` theo thứ tự trong `PROMPT_SEQUENCE.md`.
5. Chỉ giao **một prompt tại một thời điểm**.
6. Không chạy P1 khi P0 chưa qua exit gate.
7. Không chạy P2 khi file local, editor và recovery chưa ổn định.

Cách tích hợp sâu hơn:

- Đưa các quy tắc trong `AGENTS.md` vào `AGENTS.md` tại root repo.
- Giữ `CURRENT_TASK.md`, `PROJECT_STATUS.md`, `KNOWN_ISSUES.md`, `CHANGELOG.md` tại root repo.
- Dùng template trong `templates/` nếu repo chưa có các file theo dõi.

## Quy tắc vận hành

Mỗi task phải kết thúc bằng:

- Danh sách file thay đổi.
- Mô tả state/data flow thật.
- Test đã thêm.
- Lệnh đã chạy và kết quả thực tế.
- Hạn chế còn lại.
- Hướng dẫn test thủ công trên thiết bị thật.
- Cập nhật trạng thái dự án.

Không chấp nhận:

- “Should compile”.
- “Likely works”.
- Fake success.
- Copy file gốc rồi chỉ sửa metadata.
- Transcript/summary hard-code.
- License chỉ kiểm tra chuỗi chứa `PRO`.
- Tắt test/lint để CI xanh.
- Dùng `latest.release` trong code production.
