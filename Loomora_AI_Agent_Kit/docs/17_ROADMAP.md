# Delivery Roadmap

Each milestone must compile independently and end with a usable vertical slice.

## M0 — Audit and decisions

- Inspect repository/toolchain.
- Resolve stable versions.
- Record ADRs.
- Establish build commands.
- No feature code.

## M1 — Foundation

- Gradle/project setup.
- App variants.
- Hilt/DI.
- Room/DataStore foundations.
- Navigation shell.
- CI baseline.
- Build verified.

## M2 — Design system and app shell

- Theme/system/light/dark.
- Typography, spacing, components.
- Onboarding.
- Home and settings shells with real state.
- English/Vietnamese resources.
- Screenshot review.

## M3 — Local data and library foundation

- Recording metadata schema.
- DAOs/repository.
- Library empty/content/search.
- Trash/favorite/tag basics.
- Migration tests.

## M4 — Recorder vertical slice

- Permission.
- Foreground service.
- Real AudioRecord pipeline.
- Real waveform.
- Pause/resume/stop.
- Segment persistence.
- Recovery.
- Saved recording appears in library and plays.

## M5 — Playback and detail

- Media3 playback.
- Detail screen.
- Seek/speed/markers.
- Audio session persistence.
- Error handling.

## M6 — Editor

- Non-destructive trim/split/delete recipe.
- Preview.
- Export copy.
- Basic clarity.
- Original preservation.

## M7 — Transcript and insights

- Provider contracts.
- Backend job contracts.
- Explicit consent/upload.
- Transcript UI.
- Structured insights and evidence.
- Retry/cancel.

## M8 — Trial and Pro

- Trial state machine.
- Paywall.
- Website handoff.
- Activation.
- Signed offline entitlement.
- Quota/errors.

## M9 — Accessibility/localization

- English/Vietnamese completeness.
- Font scale.
- TalkBack.
- Touch targets.
- RTL readiness review even if not shipped.
- Large-screen review.

## M10 — Hardening

- P0/P1 tests.
- Long recordings.
- Process death/recovery.
- Security/privacy review.
- Performance profiling.
- OEM device matrix.

## M11 — Release candidate

- Release signing.
- R8.
- Store declarations.
- Closed test.
- Staged rollout plan.
- Support runbook.

## M12 — Marketing website

Can run in parallel after stable screenshots:
- Loomora rebrand.
- Pricing.
- Buy/activate.
- Download.
- Blog.
- Vercel deployment.
