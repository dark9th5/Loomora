# Start Here

## Prompt đầu tiên để dán vào AI Agent

```text
You are taking ownership of an existing or new Android repository for Loomora.

Before writing any code, read these files in order:
1. AGENTS.md
2. docs/00_INDEX.md
3. docs/01_PRODUCT_VISION.md
4. docs/02_SCOPE_FEATURE_MATRIX.md
5. docs/03_USER_FLOWS.md
6. docs/04_UX_UI_SPEC.md
7. docs/05_DESIGN_SYSTEM.md
8. docs/06_ARCHITECTURE.md
9. docs/17_ROADMAP.md
10. docs/18_DEFINITION_OF_DONE.md
11. PROJECT_STATUS.md
12. CURRENT_TASK.md
13. KNOWN_ISSUES.md

Then inspect the repository and do not modify source code yet.

Produce:
- a concise repository audit;
- detected build environment and modules;
- gaps between the repository and Loomora specifications;
- risks and blockers;
- a milestone plan where each milestone is a complete vertical slice;
- exact commands you will use to verify each milestone.

Write the plan into CURRENT_TASK.md and update PROJECT_STATUS.md.
Do not implement anything until I explicitly approve the first milestone.
Do not invent successful build or test results.
```

## Sau khi agent lập kế hoạch

Dùng lần lượt các prompt trong `prompts/`. Không bỏ qua bước Foundation để nhảy thẳng vào Recorder.

## Quy tắc giao việc

Một prompt tốt chỉ giao **một mục tiêu có thể nghiệm thu**. Ví dụ tốt:

> Triển khai vertical slice Recorder từ permission đến file có thể phát lại.

Ví dụ xấu:

> Làm toàn bộ app Loomora đẹp và đầy đủ.

## Khi agent báo hoàn thành

Yêu cầu nó cung cấp:

- Danh sách file thay đổi.
- Kiến trúc/state flow đã triển khai.
- Lệnh build/test đã chạy.
- Kết quả thật của từng lệnh.
- Hạn chế còn lại.
- Test thủ công cần làm trên thiết bị thật.
- Ảnh chụp màn hình hoặc mô tả UI state nếu môi trường hỗ trợ.

Không chấp nhận câu “should compile”, “likely works”, hoặc “implementation is complete” nếu không có bằng chứng.
