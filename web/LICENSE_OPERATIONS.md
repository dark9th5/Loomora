# Loomora License Operations Guide

This document details the operational procedures for creating keys, issuing, renewing, reissuing, suspending, and verifying Loomora offline licenses using canonical JSON payloads and Ed25519 digital signatures.

---

## 1. Key Creation & Management

Ed25519 key pairs are generated using Node.js `crypto.generateKeyPairSync('ed25519')` or OpenSSL.

```bash
# Generate private key
openssl genpkey -algorithm Ed25519 -out private_key.pem

# Extract public key
openssl pkey -in private_key.pem -pubout -out public_key.pem
```

- Each key pair must have a unique **`keyId`** string (e.g., `loomora-2026-v1`).
- The **Private Key** MUST NEVER be committed to Git, bundled into APKs, or rendered in web client bundles.

---

## 2. Public Key Export to Android

The public key is exported in SPKI PEM or raw base64 format and embedded into the Loomora Android application's asset/security package (`core:model` / `core:network`):

- **Android role**: Verification ONLY. Android contains NO signing code or private keys.
- **Dual-key support**: Android app maintains a list of trusted public keys indexed by `keyId` to allow seamless key rotation.

---

## 3. Production Private Key Storage Strategy

1. **Preferred (Production)**: External KMS/HSM (AWS KMS, GCP Cloud KMS, HashiCorp Vault) where the private key never leaves secure hardware.
2. **Encrypted Env (Supported Foundation)**: Private key stored in Vercel Sensitive Environment Variable (`LICENSE_PRIVATE_KEY_ENCRYPTED`), decrypted in server memory at boot using `LICENSE_PRIVATE_KEY_DECRYPTION_SECRET`.
3. **Offline CLI (Air-Gapped)**: Generate canonical payload JSON on server, export payload to air-gapped machine, sign via CLI, and import signature back to portal database.

---

## 4. Key Rotation Procedure

1. Generate a new key pair with a new `keyId` (e.g., `loomora-2027-v1`).
2. Add the new public key to the Android app manifest/config alongside existing active keys.
3. Update server environment variables with the new key ID and encrypted private key.
4. Newly issued and reissued licenses will automatically use the new key ID.
5. Retain old public keys in Android until all licenses issued under them reach `expiresAt`.

---

## 5. License Issuance Workflow

1. Customer purchases a Pro edition order.
2. Admin verifies payment confirmation (`POST /api/admin/orders/[orderId]/confirm`).
3. Admin triggers license issuance (`POST /api/admin/licenses/sign`):
   - Select Customer ID, Edition ID, Capability IDs, Effective Date (`notBefore`), Expiry (`expiresAt`), and optional Device Binding digest.
   - Server constructs `LicensePayload` with `licenseVersion = 1`.
   - Server validates payload through `licensePayloadSchema`.
   - Server canonicalizes payload (alphabetical key sorting) and hashes with SHA-256.
   - Server signs canonical payload via Ed25519.
   - Database creates `License` + initial `LicenseRevision` (revision = 1) atomically inside a Prisma transaction.
   - Audit log `LICENSE_ISSUED` is created.

---

## 6. License Renewal

1. Customer orders a renewal before or after expiry.
2. Admin confirms payment and calls `reissueLicense()`.
3. Server extends `expiresAt` timestamp, increments `licenseVersion` to `currentRevision + 1`, and creates a new immutable `LicenseRevision`.
4. Previous revisions remain preserved in database history for audit purposes.

---

## 7. License Reissue

When a customer changes devices or capabilities are modified:

1. Admin updates the license specification.
2. Server calls `reissueLicense()`, incrementing `currentRevision`.
3. Server generates a new Ed25519 signature for the updated canonical payload.
4. Database records the new `LicenseRevision`. Old revisions cannot be modified or overwritten.

---

## 8. Device Binding

- Android app generates a hardware-backed device fingerprint digest (`SHA-256(android_id + board + hardware)`).
- Digest is passed during license issuance/reissue in `payload.deviceBinding`.
- Android app verifies imported license digest matches local device digest.
- Setting `deviceBinding = null` permits multi-device floating usage.

---

## 9. License Expiry Enforcement

- Payload contains mandatory `notBefore` and `expiresAt` ISO-8601 timestamps.
- Canonicalization enforces `expiresAt > notBefore`.
- Android app compares device system clock (with network-time drift protection) against license bounds:
  - If `now < notBefore`: License is not yet valid.
  - If `now > expiresAt`: License is expired. Core recording continues; Pro capabilities disabled.

---

## 10. Offline Revocation Limitations

> [!IMPORTANT]
> Because Loomora is 100% offline-first, an Android device without internet connectivity cannot query the web portal for revocation status. Suspending a license in the admin portal prevents downloading new `.license` envelopes, but existing offline files on disconnected devices remain valid until `expiresAt`. Short validity periods (e.g., 30-90 days with auto-renewal) are recommended for high-security environments.

---

## 11. Customer Download & Ownership Verification

- Route: `GET /api/account/licenses/[licenseId]/download`
- Security checks:
  1. `requireSession()` verifies authenticated user.
  2. Server queries DB: `license.customerUserId === session.user.id`.
  3. Returns `403 Forbidden` if customer ID does not match.
  4. Returns `404 Not Found` if license is not signed.
- Headers:
  - `Content-Type: application/json`
  - `Content-Disposition: attachment; filename="<licenseId>.license"`
  - `Cache-Control: no-store`

---

## 12. Verification Before Delivery & Lost-Key Recovery

### Verification Before Delivery
Before returning any `.license` file to a customer, the server automatically executes `verifyLicenseEnvelope()` against the matching public key to guarantee signature integrity.

### Lost Private Key Recovery
If the server private key is lost or destroyed:
1. Re-issuing under the old `keyId` becomes impossible.
2. Generate a new key pair with a new `keyId`.
3. Deploy an updated Android APK containing the new public key.
4. Re-sign customer license payloads using the new key. Existing active customer licenses can be re-generated deterministically from database payload records.
