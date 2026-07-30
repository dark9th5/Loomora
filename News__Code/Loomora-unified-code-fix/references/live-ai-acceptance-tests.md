# Acceptance scenarios

## A. Task tự động

Transcript:
1. “Giao cho Lan cập nhật báo cáo trước thứ Sáu.”
2. “Ai sẽ gửi báo cáo?”
3. “Không cần gửi bản nháp.”
4. “Minh phụ trách kiểm thử trước ngày 12/08.”
5. “Lan, please send the final report tomorrow.”

Expected:
- 3 tasks: Lan update report, Minh testing, Lan send final report.
- No task from lines 2 and 3.
- Assignee and due date populated where explicit.
- Re-run analysis does not duplicate tasks.
- User-edited task remains unchanged.
- Archived task is not recreated.

## B. Language routing

- VI selected + VI model ready -> VI specialized model.
- EN selected + EN model ready -> EN specialized model.
- EN selected + only VI model ready -> request compatible model or use multilingual; never use VI-only.
- AUTO -> multilingual.
- Manual incompatible model -> visible validation error.

## C. Live captions

- Start recording with airplane mode and installed model.
- Captions appear by utterance after a short pause.
- Partial text may update; finalized rows do not mutate.
- Recording remains valid if ASR engine is deliberately made to throw.

## D. Live translation

- EN source, VI target.
- Original caption appears first.
- Translation appears only when utterance is final.
- Swap to VI→EN before next recording.
- Source and target cannot be identical.
- Missing model produces “download required”, not silent network use.
- Recording still succeeds when translation fails.

## E. Audio

- DB metadata matches actual 48k mono.
- Raw recording playable.
- Enhanced output playable.
- One hour enhanced recording does not create an unbounded permanent WAV.
- RNNoise OFF/LIGHT/STRONG produce distinguishable, non-clipping results.
