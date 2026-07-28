# Supported Devices and Capability Tiers

Date: 2026-07-28

Status values: `supported`, `supported-with-limitations`, `unsupported`, `not-tested`.

## Device Evidence

| Device | Android | ABI | RAM/chipset | Tier A Core | Tier B Speech AI | Tier C Lightweight Insights | Tier D Experimental Generative | Evidence |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| OPPO CPH2339 | Android 12 class device reported by ADB as `CPH2339T2` | arm64-v8a | Exact chipset not recorded in P2 evidence | supported | supported-with-limitations | supported | unsupported/not release-available | USB direct instrumentation `OK (4 tests)` for sherpa Whisper ASR, sherpa diarization, AAC/M4A preprocessing, and heuristic insights. LiteRT-LM model tests failed strict JSON or were killed by OEM memory policy. |
| Other low/mid/high Android devices | not-tested | not-tested | not-tested | not-tested | not-tested | not-tested | not-tested | No release matrix evidence yet. |

## Tier Definitions

- Tier A Core: recording, playback, library, and non-destructive editor basics.
- Tier B Speech AI: offline transcription; diarization when model/device are compatible.
- Tier C Lightweight Insights: deterministic extractive insights, action candidates, and evidence segment IDs.
- Tier D Experimental Generative Insights: future LiteRT-LM/llama.cpp/other runtime work. Not advertised as Available.

## Device Requirement Notes

- Do not claim universal device support.
- Imported model manifests define real model file size, checksum, supported ABI/languages, and RAM requirement.
- Unsupported device/model states must surface typed errors instead of crashes.
- Free core recording and playback must work without any AI model installed.
