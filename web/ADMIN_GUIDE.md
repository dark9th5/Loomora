# Loomora Admin Operations Guide

This guide describes the management workflows available to Administrators (`ADMIN`) and Super Administrators (`SUPER_ADMIN`) in the Loomora Web Business Portal.

---

## 1. Access & Role Requirements

- Admin portal routes (`/admin/*`) require an authenticated session with role `ADMIN` or `SUPER_ADMIN`.
- Role check is enforced server-side via `requireAdmin()` and `requireSuperAdmin()`.
- Bootstrap Super Admin email is `SUPER_ADMIN_EMAIL` (`giolanhluc@gmail.com`). Upon first Google login with a verified email, this account receives `SUPER_ADMIN` status automatically.

---

## 2. Admin Modules Overview

| Route | Module | Required Role | Primary Capabilities |
|---|---|---|---|
| `/admin` | Dashboard | ADMIN | Real order revenue stats, customer counts, ticket metrics, system status |
| `/admin/customers` | Customer Management | ADMIN | Search/filter customers, view profiles, order history, active licenses |
| `/admin/users` | Users & Roles | SUPER_ADMIN | List registered users, change roles (`CUSTOMER`, `SUPPORT`, `ADMIN`, `SUPER_ADMIN`), disable/enable users |
| `/admin/licenses` | License Management | ADMIN | Issue new licenses, reissue (immutable revision), renew, suspend, inspect payload hashes |
| `/admin/orders` | Order Management | ADMIN | List orders, view payment history, confirm manual payments with required reason |
| `/admin/products` | Catalog & Capabilities | ADMIN | Create products, editions, capabilities (product capability names only) |
| `/admin/releases` | App Release Management | ADMIN | Publish Android APK releases with SHA-256 checksums, ABIs, min Android, retire releases |
| `/admin/support` | Support Ticket Desk | ADMIN / SUPPORT | View customer tickets, update status (`OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`), send replies |
| `/admin/contact-leads` | Contact Form Leads | ADMIN | Review submitted contact inquiries from public website |
| `/admin/content` | Blog & CMS | ADMIN | Create, edit, publish, or draft blog posts for product updates |
| `/admin/audit` | Audit Logs | ADMIN | Inspect immutable audit log records (actor, action, entity, timestamp, metadata) |
| `/admin/settings` | System Settings | SUPER_ADMIN | System configuration overview, signing key mode, environment health |

---

## 3. Key Operational Workflows

### Confirming Manual Payment
1. Navigate to `/admin/orders`.
2. Locate order with status `PENDING_PAYMENT`.
3. Open confirmation dialog.
4. Enter payment reference and **mandatory reason** (min 5 characters).
5. Confirm payment (`POST /api/admin/orders/[orderId]/confirm`).
6. Order transitions to `PAID_MANUALLY`. An audit log is written.
7. *Note*: Confirming payment does NOT auto-issue a license. License issuance is a separate action.

### Issuing a Signed Pro License
1. Navigate to `/admin/licenses`.
2. Click **Issue License**.
3. Select Customer, Edition, Capabilities, Effective Date (`notBefore`), Expiry (`expiresAt`), and optional Device Binding digest.
4. Submit issuance request (`POST /api/admin/licenses/sign`).
5. Server signs canonical payload with Ed25519 and creates `License` + `LicenseRevision` (revision 1) in a transaction.
6. Customer can now download their `.license` file from `/account/licenses`.

### Changing User Roles
1. Navigate to `/admin/users` (Super Admin required).
2. Locate target user.
3. Select new role (`CUSTOMER`, `SUPPORT`, `ADMIN`, `SUPER_ADMIN`).
4. Submit change (`PATCH /api/admin/users`).
5. Server verifies that the final active Super Admin is protected (`assertFinalSuperAdminProtected`).
6. Audit log `USER_ROLE_CHANGED` is recorded.

### Publishing an Android Release
1. Upload APK artifact to storage (Vercel Blob / S3).
2. Compute SHA-256 checksum of the APK file.
3. Navigate to `/admin/releases`.
4. Enter Version Name (e.g. `1.2.0`), Version Code (e.g. `120`), Channel (`STABLE`/`BETA`), Min Android version, Supported ABIs (`arm64-v8a, armeabi-v7a`), SHA-256 hash, and Release Notes.
5. Submit release (`POST /api/admin/releases`). Artifact becomes available on `/download` and customer dashboard.

---

## 4. Safety Controls & Audit Log Policy

- **No Destructive Overwrites**: Licenses use immutable revision history (`LicenseRevision`). Reissuing a license increments `currentRevision` and stores a new signed envelope.
- **Audit Trails**: All mutations (role changes, license actions, payment confirmations, settings updates, user disablement) invoke `logAuditEvent()`.
- **Final Super Admin Lock**: The last active Super Admin account cannot be demoted or disabled.
