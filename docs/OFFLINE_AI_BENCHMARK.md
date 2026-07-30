# Offline AI Benchmark

Date: 2026-07-30  
Status: high-quality bundle host and Android device smoke tests passed

## Required Matrix

| Clip | Language | Duration | Status |
|---|---|---:|---|
| Short speech | English | 30 seconds | Not measured |
| Short speech | Vietnamese | 30 seconds | Not measured |
| Long speech | English | 5 minutes | Not measured |
| Long speech | Vietnamese | 5 minutes | Not measured |

## Metrics To Capture

- device model, Android version, RAM, and CPU/SoC;
- source format and file size;
- selected transcription language and diarization state;
- model id, version, and model-load memory;
- fingerprint, decode/resample, speech-window detection, and recognizer load time;
- transcription, diarization, alignment, insight generation, and cleanup time;
- time to first partial result and total wall time;
- peak memory, output segment count, and failure/error code.

## Current Evidence

Device: OPPO CPH2339, Android 12, physical 720 x 1612 display at 320 dpi.

The connected Sherpa smoke suite passed 4/4 cases on the device: Whisper tiny-int8 transcription, pyannote 3D speaker diarization, AAC/M4A streaming decode, and extractive insights. The suite completed in 25.22 seconds, but these fixture timings are not presented as benchmark results because the fixtures do not match the required 30-second and 5-minute EN/VI matrix.

No benchmark values are reported because approved representative English/Vietnamese clips were not available. Build or instrumentation wall times are intentionally excluded.

## Instrumentation Implemented

Analysis jobs now persist a structured timing payload containing fingerprint, decode/resample, speech detection, recognizer load, transcription, diarization, alignment, heuristic insight, optional enhancement, and total durations. The payload contains numeric performance metadata only and never transcript text.

The queue persists the source fingerprint and the worker forwards it to the coordinator, so the processing path does not hash the source again. The Sherpa recognizer is cached by model files, language and thread profile, and VAD speech windows are consumed directly instead of always transcribing the full PCM file. The pipeline publishes a base transcript before speaker labeling and insights and preserves it if a later optional stage fails.

Unit coverage verifies resource reuse/release, processing-option round trips, fingerprint reuse, incremental publishing, and that persisted timing JSON does not contain fixture transcript text.

## Physical Model Comparison

The retained 69-second Vietnamese recording was also used for a device-only model-selection smoke test. Whisper Base int8 (153.2 MB) downloaded, verified, and completed full analysis in about 50 seconds on the OPPO. Its raw output repeated a short phrase across several timestamp segments and was less accurate than Tiny for this low-signal sample. Explicit Vietnamese language selection did not materially change that result.

The earlier Tiny/Base comparison is retained as historical evidence only. Loomora now defaults to the Vietnamese-specific `sherpa-onnx-zipformer-vi-30m-int8-2026-02-09`, accompanied by Silero VAD and the existing pyannote/3D-Speaker diarization bundle. A queued job persists the selected model id, and the worker fails model validation instead of silently substituting a different model.

The transcript hallucination guard now detects exact phrase loops across timestamp boundaries, preserves the raw model output for audit, and collapses the displayed transcript before speaker fusion. On the same device sample, the long repeated output was reduced to three speaker-aligned fragments in the detected speech interval. This is a safety improvement, not evidence that the underlying words are correct.

## High-Quality Bundle Host Smoke Test

The exact Zipformer files and Sherpa ONNX 1.13.4 runtime configured by the Android app were tested against the first 45 seconds of the retained Vietnamese meeting WAV. Greedy-search decoding completed in 1.86 seconds on the development PC, produced a coherent Vietnamese transcript, and returned 148 token timestamps.

The exact Silero model and app thresholds detected 14 speech regions covering 33.52 seconds of the same 45-second clip. The Android preprocessor adds 300 ms of pre-roll and 700 ms of post-roll, and now merges only overlapping padded regions so distinct utterances are not joined merely because their pauses are short.

This host test proves that the downloaded model files, checksums, and recognizer/VAD configuration are usable.

## High-Quality Bundle Android Smoke Test

The recommended-bundle action was exercised on the OPPO CPH2339. It retained the ready diarization bundle, downloaded and verified Zipformer and Silero, and selected Zipformer in preferences. Settings reported all three models ready and Zipformer in use.

The retained seven-second Vietnamese fixture was then force-reanalyzed with speaker labeling. The resulting complete revision used `sherpa-onnx-zipformer-vi-30m-int8-2026-02-09+sherpa-onnx-pyannote-3-0-3dspeaker-int8`, language `vi`, and two distinct segments: `MAI ĐI LÀM` at 1 second and `TÔI BUỒN NGỦ` at 4 seconds. This directly covers the short-utterance regression that Whisper previously missed.

Persisted stage timings for the successful Android run were 3,595 ms decode/resample, 236 ms speech detection, 3,992 ms transcription, 1,076 ms diarization, 76 ms heuristic insights, and 9,220 ms total. The job completed with no error code.
