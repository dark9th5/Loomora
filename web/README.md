# Loomora Web Business Portal

The official web platform and customer/admin portal for **Loomora** — Smart Voice Recorder & AI Notes for Android.

Loomora is built on a 100% **local-first** architecture. The Android application records, plays back, and manages audio completely offline without requiring web accounts or cloud connections. This web portal provides marketing, customer license management, manual payment confirmation, Android APK release distribution, support ticketing, and admin operations.

---

## Technical Stack

- **Framework**: Next.js 16 (App Router, Server Components, TypeScript strict mode)
- **Styling**: Tailwind CSS with custom Loomora design system tokens & glassmorphism components
- **Database & ORM**: PostgreSQL via Prisma ORM (24 models, 0 unapplied changes)
- **Authentication**: Auth.js (NextAuth v5) with Google OAuth 2.0 & server-side RBAC guards
- **License Engine**: Canonical JSON payload normalization + SHA-256 + Ed25519 digital signatures
- **Security**: IDOR ownership guards, rate-limiting, Zod input validation, audit logging
- **Testing**: Vitest unit & integration test suite (53 tests passing)

---

## Core Capabilities

### Public Marketing & Content
- Home, Features, Pricing, Download, Blog, Privacy, Terms, Data Deletion, Contact.
- Full English & Vietnamese localization toggle.
- Edge CDN APK streaming with SHA-256 checksum verification.

### Customer Portal (`/account`)
- Google OAuth login with automatic account creation.
- Dashboard with active license status, edition capabilities, pending orders, open support tickets, and latest releases.
- Ownership-verified `.license` envelope downloads (`/api/account/licenses/[id]/download`).
- Support ticket creation and message tracking (`/account/support`).
- Customer profile settings (`/account/settings`).

### Admin Portal (`/admin`)
- Server-side role protection (`ADMIN` and `SUPER_ADMIN`).
- Real order revenue stats & customer metrics (no placeholder metrics).
- Searchable/filterable customer and user tables.
- Role management with final Super Admin protection (`SUPER_ADMIN` only).
- Transactional Ed25519 license issuance, reissue (immutable revision history), renewal, and suspension.
- Manual payment confirmation requiring admin actor, mandatory reason, and audit log.
- Android app release management (version code, channel, ABI, SHA-256, release notes).
- Product catalog, edition pricing, and capability management.
- Immutable audit log inspection (`/admin/audit`).

---

## Local Development Setup

1. Clone repository and navigate to `web` directory:
   ```bash
   cd web
   ```
2. Copy environment template:
   ```bash
   cp .env.example .env.local
   ```
3. Populate `.env.local` with development values:
   - `DATABASE_URL` (PostgreSQL connection string)
   - `AUTH_SECRET` (Generated secret)
   - `AUTH_GOOGLE_ID` & `AUTH_GOOGLE_SECRET` (Google OAuth client credentials)
   - `SUPER_ADMIN_EMAIL=giolanhluc@gmail.com`
4. Install dependencies:
   ```bash
   npm install
   ```
5. Generate Prisma Client:
   ```bash
   npm run prisma:generate
   ```
6. Apply database migrations:
   ```bash
   npm run prisma:migrate
   ```
7. Start local development server:
   ```bash
   npm run dev
   ```
8. Open [http://localhost:3000](http://localhost:3000).

---

## Verification Commands

```bash
# Run unit & integration tests (53 tests)
npm run test

# Run TypeScript strict typecheck
npm run typecheck

# Run ESLint check
npm run lint

# Build production bundle
npm run build

# Dependency security audit
npm audit --omit=dev
```

---

## Documentation Index

- [SECURITY.md](SECURITY.md) — Threat model, RBAC hierarchy, secret handling, key rotation, offline limitations.
- [LICENSE_OPERATIONS.md](LICENSE_OPERATIONS.md) — Key creation, issuance, reissue, renewal, device binding, expiry, offline limitations, downloads.
- [DEPLOYMENT.md](DEPLOYMENT.md) — Owner credentials checklist, Google OAuth setup, 19-step Vercel deployment checklist.
- [ADMIN_GUIDE.md](ADMIN_GUIDE.md) — Module overview, workflows (confirm payment, issue license, publish release, manage roles).
- [CUSTOMER_GUIDE.md](CUSTOMER_GUIDE.md) — Offline-first philosophy, complete user journey from download to license import.
- [BACKUP_AND_RECOVERY.md](BACKUP_AND_RECOVERY.md) — Database PITR, key material recovery, disaster recovery plan.
- [KNOWN_ISSUES.md](KNOWN_ISSUES.md) — Current platform status and deployment prerequisites.

---

## License & Copyright

© 2026 Loomora Audio. All rights reserved.
