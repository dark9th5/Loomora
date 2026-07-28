# Third-Party Notices

Date: 2026-07-28

This report is a release-candidate attribution checklist, not a replacement for final legal review.

## App Dependencies

- AndroidX / Jetpack Compose / WorkManager / Room / DataStore / Navigation / Hilt.
- Kotlin, Kotlin coroutines, Kotlin serialization.
- Media3 for audio playback/export paths.
- sherpa-onnx `1.13.4` AAR for offline ASR/diarization runtime.
- LiteRT-LM `0.14.0` remains packaged as an experimental dependency but deep generative meeting insights are not release-available.

## Models

- Whisper tiny multilingual int8 model pack generated locally from recorded mirror files for P2.2 smoke testing.
- sherpa pyannote + 3D-Speaker model pack generated locally from sherpa release assets for P2.3 smoke testing.

## Website Dependencies

- Next.js, React, Tailwind CSS, lucide-react, gray-matter, date-fns, clsx, tailwind-merge.

## Release Requirements

- Include upstream license texts/notices before public distribution.
- Keep source/version/checksum/license metadata for native binaries and model packs.
- Do not include production private signing keys or license-signing private keys in repo, APK, CI logs, or website artifacts.
