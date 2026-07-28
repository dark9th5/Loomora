# Project Status

Date: 2026-07-28

- Added Auth.js Google sign-in boundary with verified-email requirement.
- Bootstrap Super Admin is read from `SUPER_ADMIN_EMAIL`; production value is `giolanhluc@gmail.com`.
- Added server-side RBAC helpers and protected `/account` and `/admin` route guards.
- Added Prisma PostgreSQL schema for users, auth, customers, products, editions, capabilities, orders, licenses, releases, support, content, audit logs, and settings.
- Added deterministic license canonicalization, SHA-256 payload hashing, Ed25519 signing, and verification helpers using product capability names only.
- Added account and admin portal pages with truthful empty/pending states, not fake revenue or fake paid licenses.
- Added route handlers for contact, support, orders, license signing boundary, and license download ownership boundary.
- Generated the initial Prisma migration SQL under `prisma/migrations/0001_portal_foundation/migration.sql`.
- Verified `prisma:generate`, `test`, `typecheck`, `lint`, and production `build` locally.
- `npm audit --omit=dev` passes with `found 0 vulnerabilities` after pinning patched `postcss` and `sharp` through direct dependency/overrides.
- Deployment is not complete because owner Vercel/Google/database/signing credentials are unavailable in this workspace.
