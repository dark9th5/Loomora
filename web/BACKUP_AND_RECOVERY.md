# Loomora Backup and Disaster Recovery Plan

This document outlines backup, restoration, and disaster recovery procedures for the Loomora Web Business Portal.

---

## 1. Critical Component Inventory

| Component | Storage Location | Backup Method | Recovery Time Objective (RTO) | Recovery Point Objective (RPO) |
|---|---|---|---|---|
| **PostgreSQL Database** | Vercel Marketplace / Managed PG | Automated daily snapshots + WAL archiving | < 1 hour | < 15 minutes |
| **License Key Material** | Vercel Sensitive Env / Air-Gapped Backup | Encrypted offline backup (KMS / GPG) | < 30 minutes | 0 (static secrets) |
| **Android APK Artifacts** | Vercel Blob / S3 Storage | Multi-region bucket replication | < 1 hour | < 1 hour |
| **Portal Env Config** | Vercel Project Environment | Exported `.env` backup stored in secret manager | < 15 minutes | 0 (static config) |
| **Google OAuth Client** | Google Cloud Console | Metadata backup document | < 30 minutes | 0 (static config) |

---

## 2. Database Backup & Restoration

### Automated Backups
- Production PostgreSQL must have automated point-in-time recovery (PITR) enabled.
- Daily full logical dumps generated via `pg_dump`:

```bash
pg_dump -h <host> -U <user> -d <database> -F c -b -v -f loomora_backup_$(date +%Y%m%d).dump
```

### Database Restoration Procedure
1. Provision target PostgreSQL instance.
2. Restore schema and data:
   ```bash
   pg_restore -h <host> -U <user> -d <database> -v loomora_backup_YYYYMMDD.dump
   ```
3. Run Prisma migration check:
   ```bash
   npx prisma migrate status
   ```
4. Verify record integrity (Users, Licenses, Orders, AuditLogs).

---

## 3. License Key Material Protection & Recovery

> [!CAUTION]
> Losing the server's private license-signing key makes issuing renewals or updates under the existing key ID impossible.

### Key Material Backup Rules
- Backup `LICENSE_SIGNING_KEY_ID`, `LICENSE_PUBLIC_KEY`, `LICENSE_PRIVATE_KEY_ENCRYPTED`, and `LICENSE_PRIVATE_KEY_DECRYPTION_SECRET`.
- Encrypt key backups using GPG or store in an air-gapped secret vault (e.g. AWS Secrets Manager, HashiCorp Vault).
- Test decryption process quarterly.

### Disaster Recovery: Private Key Loss
If the signing private key is unrecoverable:
1. Generate a new key pair with a new `keyId` (e.g. `loomora-2026-v2`).
2. Update Vercel environment variables with the new key material.
3. Build and publish an updated Android APK containing the new public key.
4. Regenerate signed license envelopes for active customers from database payload records using the new key.
5. Store updated revisions in `LicenseRevision`.

---

## 4. Environment & Storage Restoration

- **Vercel Project Setup**: In case of Vercel project deletion, re-link GitHub repo, set root directory to `web`, and import environment variable backup.
- **Blob Artifacts**: Re-upload Android APK binaries to storage bucket and update storage references in `AppRelease`.
- **Google OAuth**: Update Authorized Origins and Redirect URIs if domain changes.

---

## 5. Verification Checklist After Disaster Recovery

- [ ] Database connectivity verified via `/api/admin/settings`.
- [ ] Google OAuth sign-in flow succeeds for `giolanhluc@gmail.com`.
- [ ] Bootstrap Super Admin role verified.
- [ ] Customer dashboard loads licenses, orders, and tickets cleanly.
- [ ] Test license issuance produces valid signature verifiable with public key.
- [ ] Audit log records disaster recovery actions.
