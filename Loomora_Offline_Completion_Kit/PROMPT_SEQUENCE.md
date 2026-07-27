# Prompt Sequence

Chạy theo thứ tự. Chỉ bỏ qua khi agent cung cấp bằng chứng acceptance criteria đã đạt.

## Audit

1. `prompts/00_AUDIT_AND_BASELINE.md`

## P0 — Làm project ổn định

2. `prompts/P0_01_FIX_CI.md`
3. `prompts/P0_02_RELEASE_SIGNING.md`
4. `prompts/P0_03_RECORDING_SESSION_MARKERS.md`
5. `prompts/P0_04_RECORDER_UX_DURATION_SAVE.md`
6. `prompts/P0_05_CLEANUP_LOCALIZATION.md`

**P0 exit gate:** `check`, unit tests, debug build và release compile đều thành công; recorder không auto-start; marker gắn đúng recording; stop-while-paused đúng duration; UI chỉ rời màn hình sau save success.

## P1 — Hoàn thiện sản phẩm ghi âm local

7. `prompts/P1_01_RECOVERY_CORRUPT_FILES.md`
8. `prompts/P1_02_LIBRARY_OPERATIONS_STORAGE.md`
9. `prompts/P1_03_REAL_WAVEFORM.md`
10. `prompts/P1_04_REAL_AUDIO_EDITOR.md`
11. `prompts/P1_05_SHERPA_ENHANCEMENT.md`

**P1 exit gate:** recording/playback/library/editor/enhancement dùng file thật; original preserved; process interruption được phát hiện/phục hồi hợp lý; không có metadata giả.

## P2 — AI offline và thương mại hóa

12. `prompts/P2_01_OFFLINE_AI_FOUNDATION.md`
13. `prompts/P2_02_LOCAL_TRANSCRIPTION.md`
14. `prompts/P2_03_SPEAKER_DIARIZATION.md`
15. `prompts/P2_04_LOCAL_LLM_INSIGHTS.md`
16. `prompts/P2_05_JOB_QUEUE_RESILIENCE.md`
17. `prompts/P2_06_OFFLINE_LICENSE_TRIAL.md`
18. `prompts/P2_07_RELEASE_WEBSITE.md`

## Final

19. `prompts/99_FINAL_AUDIT.md`

Không dán tất cả prompt cùng lúc.
