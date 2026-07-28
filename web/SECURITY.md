# Loomora Security Policy & Threat Model

## Threat Model

The Loomora Business Portal handles license issuance, customer accounts, and software downloads for the Loomora Android application. Primary security risks and mitigation controls are summarized below:

| Threat / Risk | Impact | Mitigation / Control |
|---|---|---|
| **Account Takeover** | Impersonation of customer or admin | Google OAuth with mandatory email verification (`email_verified == true`). Session cookies signed with `AUTH_SECRET`. |
| **Privilege Escalation** | Customer accessing admin functions | Server-side role guards (`requireAdmin()`, `requireSuperAdmin()`). Browser-supplied roles are ignored. Final Super Admin protection prevents accidental or malicious demotion. |
| **IDOR Across Customer Resources** | Customer reading/downloading another user's license/order | All resource access methods enforce `assertCanAccessOwnedResource(actorUserId, ownerUserId)`. Server checks ownership before serving `.license` envelopes. |
| **Fake Payment / License Issuance** | Unauthorized Pro license generation | Buy button does not auto-grant Pro. Payment confirmation is manual by Admin with mandatory reason + audit log. License issuance requires server Ed25519 signing. |
| **Private License Key Leakage** | Forgery of offline licenses | Private keys are NEVER stored in Git, browser bundles, database plaintext, or APKs. Server uses `encrypted-env` or external KMS/HSM. |
| **Unsafe Software Downloads** | Sideloading malicious or altered APKs | APK downloads stream via Edge CDN with published SHA-256 checksums and file sizes for verification. |
| **Support Form Spam / Abuse** | DoS / DB pollution | In-memory rate limiting (`checkRateLimit()`) applied to all public mutation endpoints (5-10 requests/min per IP/user). |
| **Misleading Revocation Claims** | False sense of security | Suspending a web license record CANNOT instantly disable an already issued offline license on a disconnected Android device. Expiry windows are the sole offline control. |

---

## Auth Model & RBAC Hierarchy

```
SUPER_ADMIN (Bootstrap: giolanhluc@gmail.com)
  └── ADMIN (Customer/Order/License Management)
        └── SUPPORT (Ticket resolution)
              └── CUSTOMER (Default for signed-in users)
```

- **Bootstrap Super Admin**: Hardcoded email lookup (`giolanhluc@gmail.com`) checked against normalized Google verified email during sign-in.
- **Server-Side Guards**: All API handlers and server components invoke `requireSession()`, `requireAdmin()`, or `requireSuperAdmin()`.
- **Final Super Admin Protection**: `assertFinalSuperAdminProtected()` prevents demoting or disabling the last active Super Admin.

---

## Secret Handling Matrix

| Secret Name | Location | Exposure Boundary | Purpose |
|---|---|---|---|
| `AUTH_SECRET` | Vercel Env (Sensitive) | Server runtime only | Auth.js JWT / session encryption |
| `AUTH_GOOGLE_SECRET` | Vercel Env (Sensitive) | Server runtime only | OAuth 2.0 client authentication |
| `DATABASE_URL` | Vercel Env (Sensitive) | Server runtime only | PostgreSQL connection pool |
| `LICENSE_PRIVATE_KEY_ENCRYPTED` | Vercel Env (Sensitive) | Server runtime only | Ed25519 license payload signing |
| `LICENSE_PRIVATE_KEY_DECRYPTION_SECRET` | Vercel Env (Sensitive) | Server runtime only | Decrypting signing key at startup |
| `BLOB_READ_WRITE_TOKEN` | Vercel Env (Sensitive) | Server runtime only | Private blob storage operations |
| `LICENSE_PUBLIC_KEY` | Public / Android APK | Public | Verifying Ed25519 license signatures in Android app |

---

## Key Rotation & Incident Response

### Key Rotation Procedure
1. Generate a new Ed25519 key pair with a unique `keyId` (e.g. `loomora-2026-v2`).
2. Update the Android app with the new public key alongside the old key (supporting dual verification during transition).
3. Update server environment variables with the new `LICENSE_SIGNING_KEY_ID` and encrypted private key.
4. Future license reissues will use the new key ID automatically.
5. Keep old public key in Android until all licenses signed under it expire.

### Incident Response Checklist
1. **Compromised Google OAuth Secret**: Rotate secret in Google Cloud Console & Vercel.
2. **Compromised Auth Secret**: Rotate `AUTH_SECRET` in Vercel (forces re-authentication for all users).
3. **Compromised Private License Key**:
   - Immediately rotate the server private key and update `keyId`.
   - Release Android APK update containing only the new public key.
   - Re-issue active licenses for legitimate customers under the new key ID.

---

## Offline License Limitations

> [!WARNING]
> An offline Ed25519 license file, once imported into the Android application, operates completely offline without network checks. Suspending or revoking a license in the web portal cannot instantly invalidate a license file on a disconnected device. Short validity windows (`expiresAt`) and device binding digests are the primary mechanisms for offline control.
