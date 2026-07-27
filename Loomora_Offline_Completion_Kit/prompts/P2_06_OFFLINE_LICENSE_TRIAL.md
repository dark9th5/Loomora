# P2.6 — Offline Signed License and Durable Trial

```text
Implement task P2.6 only. Không xây AI backend hoặc activation API.

Design:
- signed license payload verified locally;
- Ed25519 or another modern signature scheme available safely on target Android;
- public verification key in app;
- private signing key exists only in an external offline license-generation tool/environment, never repo/APK.

Example payload fields:
- licenseId;
- product;
- edition/capabilities;
- issuedAt;
- expiresAt;
- optional device binding;
- model/pipeline entitlements;
- signature/version.

Requirements:
1. Replace string-contains-PRO validation and in-memory `isPro`.
2. Verify canonical payload + signature.
3. Store accepted entitlement and trial state durably.
4. Protect local state with Android Keystore where useful, while documenting that offline client DRM cannot be unbreakable.
5. Free recording/playback/library never blocked.
6. Trial accounting:
   reserve → run → validate output → commit success;
   failure/cancel releases reservation.
7. App restart does not reset trial.
8. Expired/invalid license gives clear UI and continues Free.
9. Handle clock rollback best-effort and document limitation.
10. Pure offline licenses cannot be revoked instantly. Do not claim immediate revocation:
    - use expiry;
    - optional short validity;
    - future online refresh is explicitly out of scope.
11. Do not store raw private keys or secrets.
12. Provide a separate small license-generator CLI/spec if needed, but never bundle private key.
13. Tests for tampering, wrong signature, expiry, malformed payload, duplicate trial completion, cancellation, restart and free fallback.

Acceptance:
- modifying payload invalidates license.
- valid signed fixture enables only declared capabilities.
- no network required.
- no plain Boolean is treated as authority.
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
