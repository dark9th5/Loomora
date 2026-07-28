# Known Issues & Deployment Status

This document tracks known operational limitations and deployment prerequisites for the Loomora Web Business Portal.

---

## 1. Deployment Prerequisites (Blocked by Owner Credentials)

- **Vercel Project & Credentials**: Deployment remains blocked until the project owner provides:
  - Connected Vercel project with domain.
  - PostgreSQL database URL (`DATABASE_URL`).
  - Google OAuth Web Client credentials (`AUTH_GOOGLE_ID`, `AUTH_GOOGLE_SECRET`).
  - Secure `AUTH_SECRET` value.
  - Storage token for Vercel Blob (`BLOB_READ_WRITE_TOKEN`).
  - License signing key material (`LICENSE_SIGNING_KEY_ID`, `LICENSE_PUBLIC_KEY`, `LICENSE_PRIVATE_KEY_ENCRYPTED`, `LICENSE_PRIVATE_KEY_DECRYPTION_SECRET`).
- **Production Google OAuth Callback**: Cannot be verified on live domain until Vercel preview/production URLs exist and are added to Google Cloud Console authorized redirect URIs (`/api/auth/callback/google`).
- **Database Migrations**: `prisma migrate deploy` has not been executed against a live remote database in this workspace. Local schema (`prisma/schema.prisma`) is complete with 24 models and 0 unapplied dev changes.

---

## 2. Platform Behavioral Controls

- **Manual Payment Confirmation Required**: The portal intentionally does NOT auto-grant Pro licenses upon order creation. Admin confirmation (`POST /api/admin/orders/[orderId]/confirm`) with a mandatory reason is required before license issuance.
- **Offline Revocation Limitation**: Suspending a web license record in the admin portal prevents downloading new `.license` files, but cannot instantly invalidate an existing offline license file on a disconnected Android device. Expiry windows (`expiresAt`) and hardware device binding digests are the primary offline controls.
- **Encrypted Env Signing Decryption**: Decryption of `LICENSE_PRIVATE_KEY_ENCRYPTED` via `LICENSE_PRIVATE_KEY_DECRYPTION_SECRET` is implemented in the server signing layer but inactive until owner key credentials are configured in environment variables.

---

## 3. Local Verification Results

- `npm run test`: **PASS** (53 unit & integration tests across 9 test files)
- `npm run typecheck`: **PASS** (0 TypeScript errors)
- `npm run lint`: **PASS**
- `npm run build`: **PASS** (Clean Next.js production build)
- `npm audit --omit=dev`: **PASS** (0 vulnerabilities)
