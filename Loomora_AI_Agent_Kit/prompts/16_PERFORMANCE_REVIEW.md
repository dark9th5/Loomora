# 16 — Performance Review

## Use when
After functional stability.

## Agent prompt

```text
Profile Loomora using docs/15_RELEASE_OBSERVABILITY.md.

Measure where environment supports:
- cold start;
- tap-to-record confirmation;
- recorder CPU/memory during long session;
- waveform rendering;
- library with large dataset;
- transcript list with long content;
- playback/editor memory;
- release APK size.

Identify measured bottlenecks. Optimize only evidence-backed issues.
Avoid premature micro-optimization.
Re-run measurements and report before/after with methodology.
```

## Stop condition
Stop after measured improvements and residual risks.
