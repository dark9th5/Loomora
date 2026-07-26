# 00 — Repository Audit

## Use when
First action in a new or existing workspace.

## Agent prompt

```text
Read AGENTS.md and all orientation files listed in START_HERE.md.

Inspect the actual repository. Do not modify source code.

Create an audit covering:
- Gradle, Kotlin, Android plugin and dependency setup;
- modules and dependency direction;
- current build status;
- current UI/data/audio implementation;
- tests and CI;
- deviations from Loomora documentation;
- security/privacy concerns;
- top ten delivery risks.

Run only non-destructive inspection commands. You may run the existing build if dependencies/environment allow, but do not fix anything yet.

Update CURRENT_TASK.md and PROJECT_STATUS.md with a proposed milestone plan. Record unresolved toolchain/version decisions in docs/20_DECISIONS.md as Pending.

Report exact commands and real results.
```

## Stop condition
Stop before implementation and wait for approval.
