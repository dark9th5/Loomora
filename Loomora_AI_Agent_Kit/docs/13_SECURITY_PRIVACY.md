# Security, Privacy and Legal Design

This is an engineering specification, not legal advice. Obtain jurisdiction-specific review before commercial release.

## Data categories

- Audio recording.
- Transcript.
- Speaker labels/names.
- AI-generated notes/tasks.
- Account and entitlement.
- Diagnostics.
- Optional cloud files.

## Core controls

- Explicit microphone permission at point of use.
- Persistent recording indicator/notification.
- Consent reminder before first recording and accessible later.
- Local storage by default.
- Explicit action before cloud upload.
- TLS for transit.
- Protected backend secrets.
- Android Keystore for sensitive local key material.
- Minimal retention.
- Delete local and remote data controls.
- Privacy policy and terms accessible before purchase.

## Logging

Never log:
- raw audio;
- transcript bodies;
- license codes;
- tokens;
- full file paths containing user-provided names;
- personal task content.

Use event IDs, error categories and redacted metadata.

## Threats

- API key extraction from APK.
- License tampering.
- Trial counter tampering.
- Malicious file/URI input during import/export.
- Path traversal.
- PendingIntent misuse.
- Exported component exposure.
- Backup leakage.
- Unencrypted temporary cloud files.
- Replay/idempotency abuse.
- Screenshot leakage on sensitive screens (optional protection decision).

## App component rules

- Export only components that require it.
- Validate all intents and URIs.
- Use immutable PendingIntent where possible.
- Use FileProvider/content URIs.
- Do not expose internal file paths.
- Verify backend TLS and authentication.
- Pinning is optional and must have rotation strategy; do not add blindly.

## Retention defaults

- Local original: until user deletes.
- Local temp chunks: delete after successful finalization, except recovery window.
- Cloud upload: delete after processing by default unless user enables sync.
- Diagnostics: short and documented.
- Account deletion: clear process and status.

## Privacy UX

At Smart Insights:
- what leaves device;
- why;
- provider category;
- retention;
- delete option;
- internet requirement.

Avoid blanket consent that attempts to cover every future use.
