# ADR-002 — Media3-backed Non-Destructive Audio Export

Date: 2026-07-26

## Status

Accepted for P1.4.

## Context

Loomora needs a production non-destructive audio editor/export path that:
- never overwrites the original recording;
- exports real edited audio content for trim/delete/concatenate flows;
- stays local/offline;
- fits the current Android/Kotlin/Compose architecture.

The previous exporter only copied the original file and changed metadata, which did not satisfy production requirements.

## Decision

- Introduce an `AudioEditEngine` boundary in `:core:audio`.
- Use Media3 Transformer/Composition as the production implementation behind that boundary.
- Export to a temp file first, validate real output metadata, then publish the final file.
- Keep unsupported operations explicit:
  - `Split` is rejected in this slice.
  - speech-clarity export is rejected in this slice.
- Pin Media3 to `1.9.1` for now because it is stable and compatible with the repo's current AGP/compileSdk combination.

## Consequences

Positive:
- Real audio export replaces the fake copy-file path.
- Export orchestration is testable without forcing Media3 into every unit test.
- Future engine swaps or platform-specific workarounds stay behind one boundary.

Trade-offs:
- Some advanced editing operations remain intentionally unsupported.
- Media3 behavior still depends on device codec support and needs physical-device validation.
- The repo does not move to the latest Media3 stable line until compileSdk/AGP are upgraded in a separate task.
