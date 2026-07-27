# Prompt đầu tiên

Dán nguyên khối sau vào AI coding agent:

```text
Bạn đang tiếp quản repository Android Loomora hiện tại.

Mục tiêu dài hạn:
- hoàn thiện app ghi âm cuộc họp production-quality;
- toàn bộ xử lý âm thanh, transcription, speaker diarization, summary và action items chạy local/offline;
- không gọi API và không upload audio/transcript để xử lý AI;
- giữ file gốc;
- không tạo implementation giả.

Trước khi sửa code, hãy đọc:
1. AGENTS.md tại root repo nếu có;
2. Loomora_Offline_Completion_Kit/AGENTS.md;
3. Loomora_Offline_Completion_Kit/docs/00_EXECUTION_MAP.md;
4. Loomora_Offline_Completion_Kit/docs/01_CURRENT_REPO_FINDINGS.md;
5. Loomora_Offline_Completion_Kit/docs/02_TARGET_ARCHITECTURE.md;
6. Loomora_Offline_Completion_Kit/docs/03_OFFLINE_AI_PIPELINE.md;
7. Loomora_Offline_Completion_Kit/docs/04_DATA_AND_JOB_STATES.md;
8. Loomora_Offline_Completion_Kit/docs/05_MODEL_MANAGEMENT.md;
9. Loomora_Offline_Completion_Kit/docs/06_DEFINITION_OF_DONE.md;
10. PROJECT_STATUS.md, CURRENT_TASK.md và KNOWN_ISSUES.md nếu tồn tại.

Sau đó:
- inspect toàn bộ module và git status;
- chạy baseline build/test phù hợp;
- xác nhận các phát hiện trong docs/01_CURRENT_REPO_FINDINGS.md còn đúng hay đã thay đổi;
- không tin tài liệu nếu source hiện tại mâu thuẫn;
- ghi audit ngắn và task tiếp theo vào CURRENT_TASK.md;
- không sửa feature code trong bước audit;
- không bịa kết quả build/test.

Output bắt buộc:
1. module/toolchain đã phát hiện;
2. lệnh đã chạy và kết quả thật;
3. lỗi đầu tiên làm CI/build thất bại;
4. chênh lệch giữa source hiện tại và target architecture;
5. đề xuất bắt đầu bằng đúng prompt `prompts/00_AUDIT_AND_BASELINE.md` hoặc task P0 sớm nhất chưa đạt;
6. danh sách file dự kiến tác động;
7. rủi ro và điều cần test trên thiết bị thật.

Không triển khai toàn bộ P0/P1/P2 trong một lần.
Không thêm cloud provider, Retrofit AI endpoint hoặc API key.
```
