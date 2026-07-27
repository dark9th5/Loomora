# Audio Editor Limitations

Date: 2026-07-26

## Current production path

- Real non-destructive export uses Media3 Transformer/Composition through `AudioEditEngine`.
- Pinned Media3 version: `1.9.1`.
- Reason for `1.9.1` instead of newer stable `1.10.1`:
  - repository toolchain is currently AGP `8.8.0` with `compileSdk = 35`;
  - Media3 `1.10.1` requires `compileSdk 36`;
  - upgrading AGP/compileSdk is outside P1.4 scope.

## Supported operations in P1.4

- Trim to a selected range.
- Delete a middle range.
- Concatenate the remaining kept ranges into one new exported file.

## Explicitly unsupported in this slice

- `Split` export as a first-class operation.
- Speech clarity enhancement during export.
- Arbitrary DSP/filter chains.
- Guaranteed identical output codec/container on every device/OS combination.

Unsupported operations fail explicitly through the editor/export path. They do not report fake success.

## Device / codec limitations

- Export relies on Android device codec support exposed through Media3/MediaCodec.
- Different devices may vary in:
  - supported input codecs/containers;
  - output muxing/encoding behavior;
  - timing drift tolerance on long compressed files.
- Output is validated after export by re-reading actual metadata from the generated file.

## Follow-up hardening

- Revisit Media3 version when the repo upgrades to an AGP/compileSdk combination that supports the latest stable line.
- Add physical-device coverage for long recorder-produced AAC/M4A files across multiple Android versions/OEMs.
- Design and validate a real offline speech-clarity DSP path before enabling that toggle in export.
