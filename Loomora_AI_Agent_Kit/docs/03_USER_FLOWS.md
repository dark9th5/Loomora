# User Flows

## 1. First launch

```text
Launch
→ language follows app default (English) or user selects Vietnamese
→ concise value proposition
→ privacy/local-first explanation
→ consent reminder
→ Continue
→ Home without login
```

Do not request microphone permission during onboarding. Request it when the user taps Record.

## 2. Start recording

```text
Home → Record
→ preflight checks
→ microphone permission if needed
→ foreground service starts
→ recording state becomes active
→ waveform/timer use real recorder data
→ user can add marker, pause, resume or stop
```

Preflight:
- permission;
- available storage;
- audio input availability;
- no conflicting finalization job;
- output directory writable.

## 3. Stop and save

```text
Stop tapped
→ confirmation only when accidental-stop risk is high
→ recorder state Finalizing
→ close encoder/muxer safely
→ validate playable output
→ persist metadata
→ create waveform summary
→ open Recording Detail
```

Never navigate away while finalization is unresolved without a visible recoverable status.

## 4. Interrupted recording

Possible interruption:
- incoming call/audio focus loss;
- permission revoked;
- Bluetooth route change;
- service killed;
- storage failure;
- app process death.

Expected behavior:
- preserve completed segments;
- show clear state;
- attempt safe continuation only when technically valid;
- create a recoverable recording entry;
- never claim recording continued if it did not.

## 5. Playback

```text
Library → recording
→ Detail Overview
→ Play
→ persistent mini-player where appropriate
→ seek, speed, ±10 seconds
→ transcript follows playback when available
```

## 6. Basic edit

```text
Detail → Edit
→ load waveform/proxy
→ select range
→ preview
→ save edit recipe
→ export to new file when requested
```

Original remains unchanged.

## 7. Smart Insights trial

```text
Detail → Smart Insights
→ show data processing disclosure + remaining trials
→ user explicitly continues
→ upload/processing or on-device job
→ processing status
→ result: title, summary, key points, decisions, tasks
→ each item links to evidence where available
→ consume trial only after successful usable result
```

## 8. Upgrade/activation

```text
Premium feature → Paywall
→ compare Free and Pro
→ Buy on website / Contact sales
→ user receives account or license
→ Activate in app
→ backend validates
→ signed entitlement cached locally
→ Pro works offline for validity window
```

## 9. Offline use

- Home, recorder, library, playback, local editing and settings remain available.
- Network actions show “Requires internet” without trapping the user.
- Pending jobs are explicit; no fake progress.
- Expired cached entitlement enters a grace/explanation state, never hides local data.
