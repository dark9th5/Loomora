# Architecture Decision Log

Only durable accepted decisions belong here. Use `templates/ADR_TEMPLATE.md` for detailed ADRs.

| ID | Date | Decision | Status | Notes |
|---|---|---|---|---|
| D001 | Initial | Product name is Loomora | Accepted | Marketing line: Smart Voice Recorder & AI Notes |
| D002 | Initial | Android microphone-only recording for initial release | Accepted | No two-way call recording |
| D003 | Initial | Core Free features work offline and without login | Accepted | Local recordings never gated by entitlement |
| D004 | Initial | Original audio is preserved; edits are non-destructive | Accepted | Export creates new output |
| D005 | Initial | Default resources are English; Vietnamese supported | Accepted | System/light/dark themes |
| D006 | Initial | Hybrid smart processing, explicit opt-in | Accepted | Local core; cloud provider abstraction |
| D007 | Initial | Trial consumed only on successful result | Accepted | Default three uses per premium capability |
| D008 | 2026-07-25 | Baseline toolchain: AGP 8.8.0, Gradle 8.14, Kotlin 2.1.0, JDK 17 | Accepted | Target SDK 35, Min SDK 26, Compile SDK 35 |
| D009 | 2026-07-25 | UI framework: Jetpack Compose + Material 3 (1.3.1) | Accepted | Single-activity architecture |
| D010 | 2026-07-25 | Dependency Injection: Hilt (2.55) | Accepted | Strict module boundaries without service locators |


