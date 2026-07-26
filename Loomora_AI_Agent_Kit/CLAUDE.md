# Claude Code Instructions

The repository contract is `AGENTS.md`.

Use a plan-first workflow:
1. Inspect.
2. Plan.
3. Implement one vertical slice.
4. Verify.
5. Update status files.

Do not create generic Clean Architecture boilerplate without mapping it to Loomora's actual domain. Avoid unnecessary interfaces, use cases and modules.

When using subagents, delegate bounded work such as test review, accessibility audit or documentation consistency. The main agent remains responsible for integration and real build evidence.
