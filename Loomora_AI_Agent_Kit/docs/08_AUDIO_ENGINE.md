# Audio Engine Specification

## Goals

- Reliable microphone recording.
- Real pause/resume.
- Safe recovery.
- Real waveform/level data.
- Long-session stability.
- Foundation for transcription and enhancement.
- Playable final output.

## Recommended pipeline

```text
AudioRecord
→ PCM frames
→ level/waveform sampling
→ optional lightweight real-time preprocessing
→ AAC encoder through MediaCodec
→ MediaMuxer M4A segment
→ segment checkpoint metadata
→ final logical recording
```

Alternative implementations may be accepted if they satisfy all acceptance criteria and are recorded as an ADR.

## Recorder state machine

```text
Idle
→ Preparing
→ Recording
↔ Paused
→ Stopping
→ Finalizing
→ Completed

Any active state
→ RecoverableError
or
→ FatalError
```

Invalid transitions must be rejected. A single boolean `isRecording` is insufficient.

## Ownership

- Foreground service owns the active recording.
- UI sends commands through a stable interface.
- Service publishes authoritative state.
- Notification reflects valid commands.
- ViewModel never owns the microphone directly.

## File strategy

- Store files in app-controlled storage by default.
- Use stable recording IDs, not titles, in filenames.
- Persist each completed segment and checksum/size metadata.
- Keep temporary and finalized states distinguishable.
- Finalize atomically where possible.
- Never overwrite the original during editing.
- Provide explicit export to user-selected location.

## Pause/resume

Pause may:
- pause codec/muxing where robustly supported; or
- close a segment and begin another on resume.

The chosen strategy must produce gap-correct playback and be device-tested.

## Waveform

- Derived from real PCM amplitude/RMS samples.
- Downsample for display and persist a compact waveform cache.
- UI waveform must not fabricate motion.
- Handle silence without displaying a failure.

## Foreground service

- Show persistent notification during recording.
- Expose pause/resume/stop actions.
- Start only from valid user-initiated context.
- Handle notification permission behavior appropriately by Android version.
- Document foreground service type and manifest requirements.

## Interruptions

Define behavior for:
- audio focus changes;
- phone call;
- other recorder app;
- route change;
- wired/Bluetooth disconnect;
- permission revoked;
- low battery;
- thermal pressure;
- storage full;
- process death.

The app must show what actually happened.

## Recovery

On launch:
1. inspect unfinished metadata and temp segments;
2. validate files;
3. offer or automatically construct a recoverable recording;
4. never delete recoverable content silently;
5. mark irrecoverable corruption clearly.

## Audio quality presets

- Standard: balanced size/quality.
- High: higher bitrate.
- Storage saver: lower bitrate.
- Custom only if supported and tested.

Record actual codec, sample rate, channel count and bitrate in metadata.

## Enhancement

Basic:
- conservative high-pass/noise reduction where available;
- loudness normalization;
- speech clarity EQ.

Advanced:
- offline or backend processing behind provider interface.

Never label unverified processing as “studio quality”. Provide before/after preview and keep original.

## Required recorder tests

- Permission denied/don't ask again.
- 1-minute, 30-minute and multi-hour sessions.
- Pause/resume repeatedly.
- Screen off.
- App UI process killed while service remains.
- Service killed/restarted behavior.
- Storage nearly full/full.
- Route changes.
- Incoming call.
- Rapid start/stop.
- Double-tap commands.
- Final file is playable and duration is reasonable.
