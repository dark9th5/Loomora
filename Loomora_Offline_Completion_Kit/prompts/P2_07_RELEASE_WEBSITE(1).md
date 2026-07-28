# P2.7 — Release Hardening, Device Tiers and Website Truthfulness

```text
Implement task P2.7 only.

Bối cảnh:
- P2.4 production path hiện là extractive/heuristic insights: local, lightweight, deterministic, evidence-based.
- LiteRT-LM deep generative summary chưa ổn định trên reference device và không được quảng cáo Available.
- Future LLM-enhanced insights có thể dùng LiteRT-LM, llama.cpp hoặc runtime khác, nhưng là follow-up riêng.
- P2.5 queue hoàn tất được bằng heuristic result.
- P2.6 license dùng product capability, không runtime.

Mục tiêu:
Tạo release candidate trung thực; website chỉ quảng cáo feature có acceptance evidence.

## 1. Release gates
Run and record:
./gradlew clean check
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleRelease

Khi hỗ trợ:
./gradlew connectedDebugAndroidTest

Audit:
- CI/signing/migrations;
- recorder/service/recovery;
- library/storage/waveform/editor/enhancement;
- transcript/diarization/heuristic insights;
- persistent queue;
- license/trial;
- localization/accessibility;
- privacy/logging;
- model manager;
- website claims.

Không gọi READY nếu thiếu blocker-level evidence.

## 2. R8
1. Enable/test R8 chỉ khi regression suite và release smoke đủ.
2. Keep rules chính xác cho Room, Hilt, serialization/reflection, JNI sherpa-onnx; LiteRT-LM chỉ nếu dependency còn; llama.cpp chỉ nếu thực sự tích hợp.
3. Không blanket keep toàn package nếu tránh được.
4. Verify native methods, Room, license parsing, workers, result schema trên release.
5. Nếu chưa đủ evidence, để minify off và ghi deferred thay vì fake pass.

## 3. Signing/supply chain
1. Release không debug key.
2. CI unsigned compile hoặc sign bằng secrets đúng policy.
3. Không production private key/license-signing key trong repo/APK.
4. Pin exact dependency versions; không latest.release.
5. Native binaries/models có source/version/checksum/license.
6. Generate dependency/SBOM report nếu phù hợp.
7. Inspect APK/AAB không chứa secret, debug token, test license, unexpected model/path.

## 4. ABI, app size, model distribution
Audit:
- base APK/AAB size;
- native lib per ABI;
- model sizes;
- temp storage worst case;
- recording/export storage;
- DB growth.

Requirements:
1. Không bundle toàn bộ model lớn vào base APK nếu không có lý do rõ.
2. UI lấy model size từ manifest thật.
3. Storage preflight cho import/download/process/export.
4. Checksum + atomic install.
5. ABI support rõ.
6. Unsupported device typed error, không crash.
7. Remove model không xóa old transcript/insight revisions.
8. Core Free vẫn hoạt động khi không có AI model.

## 5. Device capability tiers
Định nghĩa từ benchmark thật:

Tier A — Core:
recording, playback, library, editor.

Tier B — Speech AI:
offline transcription; diarization nếu model/device phù hợp.

Tier C — Lightweight Insights:
heuristic/extractive summary, actions, evidence IDs.

Tier D — Experimental Generative Insights:
chỉ khi runtime/model đã benchmark; hiện không Available mặc định.

Requirements:
1. supported / supported-with-limitations / unsupported / not-tested.
2. Ghi device, Android, ABI, RAM, chipset, model, processing time, memory observation, thermal/battery, pass/fail.
3. Không nói “mọi điện thoại”.
4. UI nói rõ requirement trước model install/process.
5. Benchmark heuristic độc lập LLM.
6. LiteRT-LM blocker là deferred enhancement, không chặn app nếu heuristic đáp ứng product requirement.

## 6. Offline/failure tests
- airplane mode after models installed;
- process kill/reopen during job;
- reboot/resume where applicable;
- low storage;
- missing/corrupt model;
- missing/corrupt source;
- cancel/retry;
- optional LLM unavailable → heuristic still completes;
- invalid/expired license → Free works;
- release build smoke;
- long recording;
- background/foreground;
- native cleanup;
- workers have no network requirement.

Record actual evidence only.

## 7. Privacy/security/logging
1. State audio/transcript/insights process on-device.
2. State models may be imported/downloaded.
3. No AI processing upload in current architecture.
4. Do not log transcript, raw license/signature/device binding or sensitive paths.
5. Share via content URI/SAF.
6. Permissions minimal and explained.
7. Include model/third-party attribution.

## 8. Feature truth table
Create source-of-truth table with columns:
Feature | Status | Device/model requirement | Evidence | Website wording

At minimum:
- Recording: Available if gates pass.
- Editor/noise reduction/transcript: Available or Beta according to evidence.
- Diarization: usually Beta until sufficient device matrix.
- Heuristic insights: Available/Beta as “Smart extractive insights”.
- Deep generative summary: Coming Soon or Experimental, not Available.
- Offline license: status according to P2.6 evidence.

## 9. Website wording
Allowed for current P2.4:
- “Tóm tắt trích xuất trên thiết bị.”
- “Phát hiện ý chính và ứng viên công việc từ transcript.”
- “Insight liên kết về đoạn transcript làm bằng chứng.”
- “Hoạt động offline sau khi model cần thiết được cài.”
- “Chế độ phân tích nhẹ phù hợp với nhiều thiết bị hơn.”

Not allowed as Available:
- “LLM hiểu sâu toàn bộ cuộc họp.”
- “Generative summary trên mọi điện thoại.”
- “Gemma/LiteRT-LM chạy ổn định trên mọi máy.”
- “Tự động hiểu chính xác mọi quyết định/assignee/deadline.”
- “Không hallucinate.”
- “Speaker identification tuyệt đối chính xác.”

Generative enhancement phải ghi Coming Soon hoặc Experimental — selected compatible devices, chỉ khi có feature flag/build và evidence.

## 10. Website required content
1. Available / Beta / Coming Soon matrix.
2. On-device privacy.
3. Model size/import requirements.
4. Device/RAM/ABI limitations dựa evidence.
5. Transcript accuracy disclaimer.
6. Speaker labels probabilistic/generic, không phải identity verification.
7. Phân biệt heuristic/extractive với generative LLM.
8. Offline license expiry, no instant revocation, reinstall/transfer policy, Free fallback.
9. Không quảng cáo cloud sync/API nếu chưa có.
10. Links: privacy, terms/license, model attribution, supported devices, release notes.

## 11. Pricing/entitlement wording
1. Pricing map product capability P2.6.
2. Không bán tên runtime.
3. Nói rõ feature nào cần model.
4. Trial wording đúng durable accounting.
5. Failure/cancel wording khớp implementation.
6. Không hứa immediate revocation.
7. Không hứa lifetime model updates nếu chưa có policy.

## 12. Release docs
Create/update:
- FINAL_AUDIT_REPORT.md
- SUPPORTED_DEVICES.md
- MODEL_MANIFEST.md/generated report
- PRIVACY.md/privacy page
- THIRD_PARTY_NOTICES.md
- RELEASE_CHECKLIST.md
- KNOWN_ISSUES.md
- website feature matrix

FINAL_AUDIT_REPORT dùng PASS / FAIL / NOT TESTED và recommendation duy nhất:
NOT READY / INTERNAL TEST / CLOSED BETA / OPEN BETA / READY

## 13. Verification
- clean check;
- unit tests;
- debug/release build;
- migration tests;
- release smoke;
- airplane-mode AI;
- optional LLM fallback;
- license/trial regression;
- APK/AAB secret/model inspection;
- website claim-to-evidence audit;
- localization/accessibility smoke;
- low/mid device test if available;
- unsupported-device behavior.

## Acceptance
1. Release build gates pass hoặc report trung thực NOT READY.
2. No debug signing/private key/secret.
3. Website has Available/Beta/Coming Soon.
4. Heuristic insights described accurately.
5. LiteRT-LM deep summary not advertised Available.
6. Optional generative runtime does not block recorder/transcript/heuristic flow.
7. Device/model limits backed by evidence.
8. License wording matches P2.6.
9. Privacy/attributions complete.
10. Final audit has clear recommendation and actual command/device evidence.
```

## Quy trình bắt buộc
1. Inspect source/git status and P2.4–P2.6 actual state.
2. Update CURRENT_TASK.md before changes.
3. Do not add new LLM runtime in release-hardening task.
4. Run real checks and record actual outcomes.
5. Update tracking docs and final audit.
6. Stop after P2.7.

## Cấm
- Advertise generative summary as complete.
- Hide NOT TESTED/FAIL.
- Claim all-device support.
- Bundle secrets/private keys.
- Enable R8 without release regression evidence.
