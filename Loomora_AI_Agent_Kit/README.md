# Loomora AI Agent Kit

**Product:** Loomora  
**Marketing line:** Smart Voice Recorder & AI Notes  
**Platform:** Android, Kotlin, Jetpack Compose  
**Product direction:** Local-first, offline-friendly, commercially extensible

Bộ tài liệu này được thiết kế để dùng với Gemini, Claude Code, Codex, Cline hoặc AI coding agent tương đương. Mục tiêu là ngăn agent tạo “app demo có giao diện nhưng nghiệp vụ giả”, đồng thời giữ kiến trúc, UX và chất lượng nhất quán xuyên suốt dự án.

## Cách dùng nhanh

1. Giải nén toàn bộ thư mục này vào **root của repository Android**.
2. Mở `START_HERE.md`.
3. Cho agent đọc theo thứ tự trong `docs/00_INDEX.md`.
4. Dán prompt đầu tiên trong `START_HERE.md`.
5. Không yêu cầu agent tạo toàn bộ app trong một lần.
6. Chỉ chạy prompt milestone kế tiếp khi milestone hiện tại đã build và đạt quality gate.
7. Sau mỗi milestone, agent phải cập nhật:
   - `PROJECT_STATUS.md`
   - `CURRENT_TASK.md`
   - `CHANGELOG.md`
   - `KNOWN_ISSUES.md`
   - `docs/20_DECISIONS.md` khi có quyết định kiến trúc

## Nguồn sự thật

Thứ tự ưu tiên khi có mâu thuẫn:

1. `AGENTS.md`
2. `docs/01_PRODUCT_VISION.md`
3. `docs/02_SCOPE_FEATURE_MATRIX.md`
4. `docs/03_USER_FLOWS.md`
5. `docs/04_UX_UI_SPEC.md`
6. `docs/05_DESIGN_SYSTEM.md`
7. Các đặc tả kỹ thuật tương ứng
8. `docs/20_DECISIONS.md`
9. Prompt milestone hiện tại

Không được âm thầm thay đổi sản phẩm để làm code dễ hơn.

## Nguyên tắc quan trọng nhất

> Một tính năng chỉ được coi là hoàn thành khi có luồng thật, trạng thái lỗi, lưu dữ liệu thật, build thành công, test phù hợp và bằng chứng kiểm tra.

Mock/fake data chỉ được phép trong Compose Preview, test fixture và sample riêng; tuyệt đối không được trở thành nguồn dữ liệu production.
