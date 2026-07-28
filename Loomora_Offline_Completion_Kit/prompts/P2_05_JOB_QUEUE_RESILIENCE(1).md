# P2.5 — Persistent, Idempotent Offline Processing Queue

```text
Implement task P2.5 only.

Bối cảnh đã chốt:
- P2.2 đã có transcription offline.
- P2.3 đã có speaker diarization/alignment offline.
- P2.4 đã có extractive/heuristic meeting insights chạy local, nhẹ, deterministic, có evidence IDs và không fake/hard-code.
- LiteRT-LM generative insights chưa phải production requirement vì chưa có `.litertlm` model đủ nhỏ và ổn định trên thiết bị thử nghiệm.
- Không thay P2.4 bằng llama.cpp trong task này.
- P2.5 phải hoạt động hoàn chỉnh với heuristic insights hiện tại và chừa abstraction cho optional LLM enhancement về sau.

Mục tiêu:
Dùng WorkManager cho persistent execution, Room làm source of truth; hỗ trợ process death, retry, cancel, checkpoint và idempotency. Heuristic result hợp lệ phải hoàn tất job ngay cả khi optional LLM enhancement không khả dụng.

## 1. Audit trước khi sửa
Inspect:
- recording/transcript/diarization/insight entities, DAOs, repositories;
- workers và WorkManager orchestration hiện tại;
- P2.4 insight engine, evidence validation, result metadata;
- model manager/runtime capability;
- trial/entitlement hooks;
- temp/checkpoint files;
- UI processing state;
- Room version và migration tests.

Ghi rõ:
- heuristic engine/interface hiện tại;
- heuristic result được persist thế nào;
- LiteRT-LM code/dependency còn ở đâu;
- nơi nào đang coi LLM failure là toàn job failure.

Không rewrite P2.4 nếu contract hiện tại đã dùng được.

## 2. Logical job identity
Tạo unique logical key từ:

recordingId + sourceFingerprint + pipelineVersion + canonicalRequestedOptions

Requested options phải canonicalize, ví dụ:
- enhanceAudio;
- transcriptionModelId;
- diarizationEnabled;
- insightsMode;
- outputLanguage.

Không đưa enqueue timestamp hoặc dữ liệu volatile vào key.

Yêu cầu:
1. Enqueue cùng logical key trả về/observe cùng job.
2. Worker chạy lại không duplicate transcript segments, speaker turns, insight revisions hoặc files.
3. Source fingerprint mới tạo job mới và đánh dấu result cũ stale, không ghi đè lịch sử.
4. Dùng database uniqueness/transaction phù hợp, không chỉ dựa WorkManager unique name.

## 3. Room job state
Room là source of truth; WorkInfo chỉ phản ánh execution.

Job status tối thiểu:
- QUEUED
- RUNNING
- CANCEL_REQUESTED
- CANCELLED
- COMPLETED
- RETRYABLE_FAILURE
- TERMINAL_FAILURE
- INVALIDATED

Stage tối thiểu:
- PREPARING_AUDIO
- ENHANCING
- DETECTING_SPEECH
- TRANSCRIBING
- DIARIZING
- ALIGNING
- GENERATING_HEURISTIC_INSIGHTS
- OPTIONAL_LLM_ENHANCEMENT
- VALIDATING
- PUBLISHING
- CLEANING_UP

OPTIONAL_LLM_ENHANCEMENT chỉ chạy khi user option yêu cầu, compatible engine/model sẵn sàng, device đạt capability và entitlement cho phép. Nếu không đạt, stage phải SKIPPED với typed reason, không làm job fail.

Checkpoint lưu:
- jobId;
- stage/status/progress/attempt;
- input fingerprint;
- output/checkpoint reference;
- startedAt/finishedAt;
- engine/model/pipeline version;
- typed error;
- skip/fallback reason.

## 4. Insight completion semantics
Chuẩn hóa metadata:

generationMode:
- HEURISTIC
- LLM_ENHANCED
- HEURISTIC_FALLBACK

completionQuality:
- EXTRACTIVE_ONLY
- ENHANCED
- DEGRADED_BUT_VALID

Quy tắc:
1. Heuristic output parse/validate/evidence-validate thành công là kết quả hoàn chỉnh hợp lệ.
2. Không ghi heuristic result là LLM_ENHANCED.
3. Optional LLM thành công thì tạo enhanced revision hoặc publish merged result theo contract.
4. LLM missing/incompatible/OOM/process-killed/invalid-output:
   - giữ heuristic result;
   - lưu fallback reason;
   - hoàn tất job với HEURISTIC_FALLBACK;
   - không retry vô hạn;
   - không terminal-fail toàn pipeline.
5. Chỉ fail insight pipeline nếu heuristic path và validation tối thiểu đều fail.
6. Evidence IDs phải trỏ transcript segments tồn tại trước publish.

Không cần thêm COMPLETED_WITH_FALLBACK nếu metadata trên đã đủ rõ.

## 5. Retry policy
Retryable examples:
- recoverable I/O interruption;
- worker/process interruption trước atomic publish;
- temporary file lock;
- temporary runtime initialization failure được chứng minh recoverable.

Terminal/fallback examples:
- source missing/corrupt;
- checksum mismatch;
- unsupported ABI/device incompatible;
- model missing;
- OOM sau safe cleanup/fallback;
- invalid configuration;
- repeated invalid model output quá giới hạn;
- unrecoverable evidence validation failure.

Yêu cầu:
1. Exponential backoff chỉ cho retryable errors.
2. Retry từ checkpoint hợp lệ gần nhất.
3. Không chạy lại ASR/diarization nếu checkpoint còn hợp lệ.
4. Optional LLM retry tối đa nhỏ; sau đó fallback heuristic.
5. Cancellation không biến thành retry.
6. Bảo toàn coroutine cancellation; không catch Throwable rồi nuốt cancellation.

## 6. Cancellation/resume
1. Persist CANCEL_REQUESTED.
2. Worker kiểm tra cancellation giữa chunks và trước publish.
3. Release native/model resources trong finally.
4. Cleanup unpublished temp output.
5. Không xóa valid checkpoint/result của stage trước vô lý.
6. Chỉ có Pause nếu thực sự checkpoint + cancel/resume; nếu chưa thì chỉ Cancel và Retry/Resume.
7. Resume dùng cùng logical job, tạo attempt mới, không duplicate result.

## 7. Process death/reboot/foreground work
1. WorkManager persistent work.
2. Room giữ state quan trọng.
3. Khi app mở lại, reconcile Room với WorkManager; RUNNING không còn execution phải về QUEUED/RETRYABLE theo policy, không tự COMPLETED.
4. Long-running work dùng foreground notification/service type đúng API.
5. Notification có stage, progress, Cancel.
6. Không require network.
7. Không phụ thuộc Activity/ViewModel sống.
8. UI observe Room Flow.

## 8. Atomic publication và cleanup
Flow:
write temp → close/flush → validate → DB transaction → atomic publish → mark complete

Yêu cầu:
- temp namespace theo jobId;
- cleanup abandoned temp có retention;
- idempotent cleanup;
- worker chạy hai lần không publish duplicate;
- DB không mark complete trước output hợp lệ;
- source thay đổi/xóa giữa job phải invalidate/cancel an toàn;
- model bị remove giữa stage trả typed error hoặc fallback phù hợp.

## 9. Trial integration boundary
P2.5 chưa triển khai full license nhưng phải có port:
reserve → run → validate/publish → commit

Quy tắc:
1. Retry cùng logical job không reserve thêm.
2. Cancel/failure trước publish release reservation.
3. Heuristic success có tiêu trial hay không do capability policy, không hard-code trong worker.
4. Optional LLM fallback không trừ thêm lượt.
5. P2.6 sẽ triển khai authority thật.

## 10. Tests bắt buộc
- duplicate enqueue;
- canonical options same key;
- source fingerprint change creates new job;
- worker executed twice;
- process recreation/reconciliation;
- retry from checkpoint;
- cancellation and cancellation-not-retry;
- source deletion/change;
- model missing/device incompatible;
- optional LLM unavailable → heuristic complete;
- optional LLM OOM/invalid output → heuristic fallback;
- heuristic failure → terminal failure;
- evidence validation before publish;
- temp cleanup;
- atomic transaction;
- no duplicate rows/files;
- trial reservation not duplicated;
- no network constraint;
- UI observes Room contract.

## Acceptance
Task chỉ Complete khi:
1. Kill process trong lúc processing, mở lại thấy đúng persisted state.
2. Retry từ checkpoint và không duplicate result.
3. Cancel/cleanup đúng.
4. Job không yêu cầu network.
5. Heuristic-only device hoàn tất insight hợp lệ.
6. Optional LLM unavailable không làm toàn pipeline fail.
7. Room là source of truth.
8. Build/tests liên quan pass.
9. Tracking docs ghi heuristic là production path; generative LLM là optional/future; LiteRT-LM blocker không chặn P2.5.
```

## Quy trình bắt buộc
1. Inspect source và git status.
2. Cập nhật CURRENT_TASK.md trước khi code.
3. Không sửa lại P0–P2.4 ngoài integration tối thiểu.
4. Chạy tests/build và ghi kết quả thật.
5. Cập nhật PROJECT_STATUS.md, CHANGELOG.md, KNOWN_ISSUES.md.
6. Dừng sau P2.5.

## Cấm
- Fake success, tắt test/lint, bịa command result.
- Coi heuristic là generative LLM.
- Coi optional LLM failure là failure toàn job khi heuristic hợp lệ.
- Tự tích hợp llama.cpp trong task này.
