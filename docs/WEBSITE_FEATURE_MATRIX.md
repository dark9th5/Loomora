# Website Feature Matrix

Date: 2026-07-28

| Feature | Status | Device/model requirement | Evidence | Website wording |
| --- | --- | --- | --- | --- |
| Recording | Available | Android device with microphone permission | P0.3-P0.5 unit/build/device smoke evidence in `PROJECT_STATUS.md` | "Free local recording and playback remain available offline." |
| Library/playback | Available | Local app storage | P1.1-P1.2 unit/build coverage | "Core recordings remain accessible without Pro." |
| Non-destructive editor basics | Beta | Media3/device codec support | P1.4 unit/build coverage; physical listening still follow-up | "Trim and export edited copies without overwriting originals." |
| Speech clarity/noise reduction | Coming Soon | Validated DSP path required | P1.4 rejects speech clarity export | "Speech clarity remains future hardening." |
| Offline transcription | Beta | Installed sherpa Whisper multilingual model pack; compatible ABI/RAM | OPPO `CPH2339` device smoke `OK`; unit tests | "Offline transcription after required model install." |
| Speaker diarization | Beta | Installed diarization model pack; compatible ABI/RAM | OPPO `CPH2339` two-speaker smoke `OK`; unit fusion tests | "Generic speaker labels, probabilistic, not identity verification." |
| Smart extractive insights | Available/Beta | Transcript revision; no LLM model required | Heuristic unit/device smoke; evidence IDs validated | "Evidence-linked extractive insights from transcript." |
| Deep generative LLM summary | Coming Soon / Experimental | Future compatible runtime/model and benchmark | LiteRT-LM tests failed strict JSON or memory policy | "Experimental on selected compatible devices; not generally available." |
| Persistent queue | Beta | WorkManager/Room | P2.5 unit/build evidence; process-kill smoke follow-up | "Persistent offline processing queue." |
| Offline signed license | Beta | Signed envelope and app public key | P2.6 unit/build evidence | "Signed offline license; no instant offline revocation." |

Not allowed as Available:

- "Generative summary on all Android devices."
- "No hallucination."
- "Remote AI transcription."
- "Speaker identification is exact."
- "Immediate offline license revocation."
