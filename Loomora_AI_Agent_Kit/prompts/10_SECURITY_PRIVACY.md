# 10 — Security and Privacy Review

## Use when
Before release candidate.

## Agent prompt

```text
Perform milestone M10 security/privacy work using docs/13_SECURITY_PRIVACY.md.

Inspect:
- manifest/exported components;
- intents, URIs and FileProvider;
- pending intents;
- local file paths;
- logs;
- secrets and BuildConfig;
- entitlement storage;
- upload URLs;
- deletion;
- temporary files;
- analytics payloads;
- backup behavior;
- network security.

Fix confirmed issues without unrelated rewrites.
Add tests/checks where practical.
Update privacy data-flow documentation and known issues.
Report what was inspected and what could not be verified.
```

## Stop condition
Stop before claiming legal compliance; identify legal-review items separately.
