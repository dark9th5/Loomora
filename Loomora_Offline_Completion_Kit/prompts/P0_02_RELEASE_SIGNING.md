# P0.2 — Production Release Signing

```text
Implement task P0.2 only: loại debug signing khỏi release và tạo cấu hình signing production an toàn.

Inspect:
- app/build.gradle.kts
- .gitignore
- CI workflow
- existing release docs/scripts

Requirements:
1. `release` tuyệt đối không dùng debug signing config.
2. Production keystore/password không commit vào repo.
3. Hỗ trợ local release signing bằng `keystore.properties` hoặc environment variables.
4. Hỗ trợ GitHub release workflow bằng repository secrets khi được cấu hình.
5. Pull-request CI vẫn compile release an toàn khi không có production secrets; có thể tạo unsigned release artifact, không được dùng official key hoặc debug key.
6. Pin rõ tên env/property:
   - LOOMORA_STORE_FILE
   - LOOMORA_STORE_PASSWORD
   - LOOMORA_KEY_ALIAS
   - LOOMORA_KEY_PASSWORD
   hoặc naming tương đương được document.
7. Thêm fail-fast message dễ hiểu cho workflow phát hành thật khi thiếu secret.
8. Update `.gitignore`.
9. Viết tài liệu:
   - tạo key;
   - backup;
   - không đổi key sau khi phát hành;
   - cách build local;
   - cách cấu hình GitHub Secrets.
10. Chưa bật R8 nếu repo chưa đủ regression test; ghi rõ task hardening sau.

Acceptance:
- source không chứa password/keystore.
- release không tham chiếu `signingConfigs.getByName("debug")`.
- debug build pass.
- release compile pass không cần production key.
- signed release path được cấu hình và document, nhưng không bịa rằng đã ký nếu không có key.
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
