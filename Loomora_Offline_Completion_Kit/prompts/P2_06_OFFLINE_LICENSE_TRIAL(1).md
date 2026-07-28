# P2.6 — Offline Signed License, Capability Entitlements and Durable Trial

```text
Implement task P2.6 only. Không xây AI backend hoặc activation API.

Bối cảnh:
- Free recording/playback/library không bị khóa.
- P2.4 hiện có heuristic/extractive insights local.
- Generative LLM enhancement chưa bắt buộc.
- P2.5 đã cung cấp persistent job và trial reservation boundary.
- License phải theo product capability, không theo runtime LiteRT-LM/llama.cpp.
- Offline license không thể revoke tức thời; phải document đúng.

Mục tiêu:
Thay fake entitlement/in-memory trial bằng signed offline license và durable trial accounting.

## 1. Audit
Inspect:
- valid-pro-token, contains("PRO"), isPro, in-memory entitlement;
- subscription/settings/paywall UI;
- Room/DataStore;
- P2.5 trial port;
- feature gates;
- Keystore/logging;
- migration hiện có.

Liệt kê mọi nơi đang dùng Boolean/string làm Pro authority.

## 2. Product capabilities
Dùng capability độc lập runtime, ví dụ:
- CORE_RECORDING
- AUDIO_EDITOR
- OFFLINE_TRANSCRIPTION
- SPEAKER_DIARIZATION
- SMART_INSIGHTS
- LLM_ENHANCED_INSIGHTS
- MODEL_PACK_STANDARD
- MODEL_PACK_ADVANCED

Quy tắc:
1. Không dùng LITERT_LM_ACCESS, LLAMA_CPP_ACCESS, GGUF_PRO.
2. Heuristic và generative enhancement có thể cùng hoặc khác tier, nhưng policy phải tập trung và test được.
3. Thay runtime/model không làm license payload mất nghĩa.
4. Free capabilities rõ ràng và luôn hoạt động khi license invalid.

## 3. Signed license envelope
Dùng canonical versioned payload, ví dụ:
{
  "schemaVersion": 1,
  "licenseId": "lic_...",
  "product": "loomora",
  "edition": "pro",
  "capabilities": ["OFFLINE_TRANSCRIPTION", "SMART_INSIGHTS"],
  "issuedAt": "...",
  "notBefore": "...",
  "expiresAt": "...",
  "deviceBinding": null,
  "licenseVersion": 1
}

Envelope:
{
  "payload": {...},
  "signatureAlgorithm": "Ed25519",
  "keyId": "loomora-prod-2026-01",
  "signature": "base64..."
}

Requirements:
1. Ed25519 hoặc modern algorithm phù hợp target Android.
2. Canonical serialization deterministic.
3. Verify bằng public key/keyId trong app.
4. Private key không ở repo/APK/test production/CI log.
5. Hỗ trợ key rotation bằng keyId.
6. Validate schema, product, signature, notBefore, expiry, capabilities, optional binding.
7. Sửa một byte payload phải invalid.
8. Malformed/unknown schema fail closed cho Pro nhưng app vẫn Free.
9. Không cần network.

## 4. Import và persistence
1. Import license qua paste/QR/file SAF tùy UI.
2. Verify trước persist.
3. Persist envelope và derived entitlement state bền vững.
4. Có thể dùng Keystore để bảo vệ local state, nhưng document offline client DRM không tuyệt đối.
5. Không log raw license/signature/device identifier.
6. Invalid/expired hiển thị user-friendly reason.
7. Remove license trở về Free, không xóa user data.
8. Restart giữ entitlement.
9. Reinstall/backup policy phải document, không hứa chống reset tuyệt đối nếu không có server.

## 5. Optional device binding
Nếu dùng:
- app-scoped installation identifier;
- không IMEI/serial;
- canonical/hash phù hợp privacy;
- document chuyển máy/reinstall;
- có quy trình cấp lại ngoài app.

Nếu chưa có requirement rõ, giữ deviceBinding null.

## 6. Durable trial state machine
Statuses:
- AVAILABLE
- RESERVED
- COMMITTED
- RELEASED
- EXPIRED

Trial operation fields:
- trialOperationId;
- logicalJobKey;
- capability;
- reservedAt/committedAt/releasedAt;
- status;
- resultRevisionId.

Flow:
check entitlement/trial → reserve exactly once → P2.5 runs → validate/publish → commit exactly once

Rules:
1. Retry/reopen/worker rerun không reserve/commit thêm.
2. Cancel/failure trước publish release reservation.
3. Process death giữ reservation và reconciliation xử lý.
4. Crash sau publish trước commit phải commit đúng một lần từ result revision.
5. Failure/cancel không tiêu trial.
6. Invalid/empty result không commit.
7. Policy quy định rõ transcription, diarization, heuristic insights, LLM-enhanced insights loại nào tiêu trial.
8. Optional LLM fail nhưng heuristic success chỉ commit theo result/capability thật được publish, không trừ hai lượt.
9. Restart không reset trial.
10. Clock rollback không khôi phục lượt đã dùng.

## 7. Clock/offline limitations
1. Dùng wall clock + monotonic/last-seen best effort.
2. Detect significant rollback thành typed suspicious-clock state.
3. Không khóa free recording.
4. Không tuyên bố chống clock tampering tuyệt đối.
5. Offline license không revoke tức thời: dùng expiry/short validity; online refresh out of scope.
6. UI/website phải nói đúng giới hạn.

## 8. Entitlement authority
Tạo authority duy nhất, ví dụ:
interface EntitlementRepository {
  fun observeEntitlements(): Flow<EntitlementSnapshot>
  suspend fun canUse(capability: Capability): EntitlementDecision
}

Decisions:
- GRANTED_FREE
- GRANTED_LICENSED
- GRANTED_TRIAL
- DENIED_EXPIRED
- DENIED_INVALID_LICENSE
- DENIED_TRIAL_EXHAUSTED
- DENIED_UNSUPPORTED_DEVICE

Rules:
- UI chỉ render decision.
- Worker/use case kiểm tra lại authority.
- Không plain mutable isPro authority.
- Capability không phụ thuộc runtime.
- Device/model incompatibility không đồng nghĩa license invalid.

## 9. License generator
Nếu cần tạo CLI/spec riêng:
- đọc canonical payload;
- ký bằng private key từ secure path/env;
- xuất envelope;
- test key chỉ trong test resources và ghi rõ non-production;
- document backup/rotation.

Không đưa chức năng ký vào APK.

## 10. Migration
1. Remove/migrate fake token state.
2. Fake key cũ không tự thành valid Pro.
3. Dev/test migration chỉ ở debug hoặc reset rõ.
4. Room/DataStore migrations có tests.
5. Remove valid-pro-token production path.
6. Old insight revisions vẫn đọc được dù runtime/capability đổi.

## 11. Tests
- valid signed license;
- payload tamper;
- wrong signature/keyId;
- expired/not-yet-valid;
- malformed/unknown schema/product mismatch;
- capability subset;
- restart persistence;
- invalid license falls back Free;
- remove license preserves data;
- reserve once;
- duplicate worker no double reserve/commit;
- cancel/failure releases;
- publish-then-crash reconciliation commits once;
- heuristic success + optional LLM failure commits once;
- clock rollback;
- device binding match/mismatch if implemented;
- legacy fake token rejected;
- no network;
- no plain Boolean authority.

## Acceptance
1. Payload modification invalidates license.
2. Valid fixture enables only declared capabilities.
3. Free recording/playback/library work without valid license.
4. Trial persists across restart.
5. Retry/cancel/process death do not miscount.
6. Heuristic fallback is not charged twice due to optional LLM failure.
7. Capability names contain no runtime names.
8. No private key in repo/APK.
9. No network required.
10. Tracking docs explain revocation, clock and tamper limitations.
```

## Quy trình bắt buộc
1. Inspect source/git status and P2.5 trial boundary.
2. Update CURRENT_TASK.md before code.
3. Do not build activation backend.
4. Run tests/build and report actual results.
5. Update tracking docs.
6. Stop after P2.6.

## Cấm
- Fake validation/in-memory isPro authority.
- Runtime-named entitlements.
- Private signing key in app/repo.
- Claim immediate offline revocation.
