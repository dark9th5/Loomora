# Backend API Contracts

Exact transport may evolve; semantics must remain stable.

## Principles

- Versioned `/v1`.
- Auth optional for Free local app, required for activation/cloud quota.
- Idempotency keys for jobs and purchase/license operations.
- Stable machine-readable error codes.
- Signed upload URLs.
- No provider secrets in mobile app.

## Endpoints

```text
POST /v1/auth/login-or-activate
POST /v1/licenses/activate
GET  /v1/entitlements/me
POST /v1/entitlements/refresh

POST /v1/recordings/{id}/upload-session
POST /v1/recordings/{id}/complete-upload
POST /v1/recordings/{id}/processing-jobs
GET  /v1/processing-jobs/{jobId}
POST /v1/processing-jobs/{jobId}/cancel
DELETE /v1/recordings/{id}/remote-data

GET  /v1/usage
POST /v1/trials/reserve
POST /v1/trials/{operationId}/complete
POST /v1/trials/{operationId}/release
```

## Processing job state

- queued
- uploading
- transcribing
- analyzing
- completed
- failed
- cancelled
- deletion_pending

## Standard error shape

```json
{
  "code": "TRIAL_EXHAUSTED",
  "message": "Localized by client using code",
  "retryable": false,
  "requestId": "..."
}
```

Do not display backend English messages directly as final UI copy.
