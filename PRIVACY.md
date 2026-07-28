# Privacy

Date: 2026-07-28

Loomora is local-first.

- Recording, playback, library, and editor state are stored on the user's device.
- Current AI processing for transcription, diarization, and extractive insights runs on device after required local models are installed.
- Current architecture does not upload audio, transcripts, or insights for AI processing.
- Share/export flows use Android content URI or SAF-style handoff where implemented.
- License verification is offline and uses signed envelopes. The app must not log raw license envelopes, signatures, device binding values, transcript text, or sensitive file paths.
- Offline license revocation is not immediate; expiration and signed validity windows are enforced locally.

## Permissions

- Microphone is required for recording.
- Storage/file access is scoped through app storage or explicit user-selected SAF/import/export flows.
- Network is not required for current offline AI processing.

## Limits

- Transcript accuracy depends on model, audio quality, language mix, and device runtime behavior.
- Speaker labels are generic/probabilistic and are not identity verification.
- Local offline state can be tampered with on a compromised device; client-side DRM is not absolute.
