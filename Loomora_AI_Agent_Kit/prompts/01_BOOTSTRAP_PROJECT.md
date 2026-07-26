# 01 — Bootstrap Project Foundation

## Use when
After audit approval; for a new or broken foundation.

## Agent prompt

```text
Implement milestone M1 only.

Read:
- AGENTS.md
- docs/06_ARCHITECTURE.md
- docs/07_MODULE_STRUCTURE.md
- docs/14_TESTING_STRATEGY.md
- docs/17_ROADMAP.md
- docs/18_DEFINITION_OF_DONE.md

Tasks:
- establish Gradle Kotlin DSL and version catalog;
- select current stable official Android/Kotlin/Jetpack versions and record them in docs/20_DECISIONS.md;
- create only the justified initial modules;
- configure application variants and build constants without secrets;
- configure Hilt/DI foundation;
- configure Room schema export and DataStore foundation;
- create navigation shell without fake feature data;
- add a minimal CI workflow;
- add baseline unit test and build verification.

Do not implement microphone recording, AI, billing or complex screens.

Run supported format/static checks, unit tests, debug build and release compile. Fix actual failures. Do not silence them.

Update tracking files and report evidence.
```

## Stop condition
Stop when M1 passes its quality gate. Do not start design or recorder work.
