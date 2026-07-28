# LOOMORA WEB BUSINESS PORTAL — BUILD & DEPLOY PROMPT

```text
Implement the Loomora Marketing Website, Customer Portal, Admin Portal and Offline License Management Service, then deploy the production website to Vercel.

This is a separate web/business system for the Loomora Android application.
Do not rewrite or tightly couple the Android app to this website beyond the agreed offline license format and download links.

==================================================
0. FIXED PRODUCT CONTEXT
==================================================

Product:
- Name: Loomora
- Android smart recorder application
- Kotlin + Jetpack Compose
- Offline-first/local processing
- Free recording, playback and library remain usable without account or internet
- Paid capabilities are unlocked through signed offline license files
- Android verifies licenses locally using a public key
- Website manages marketing, customers, orders, support and license issuance

Initial Super Admin Google account:

  giolanhluc@gmail.com

Rules:
- Use this exact email only as the bootstrap Super Admin allowlist.
- Admin login must use Google OAuth.
- No password login for the bootstrap admin.
- Require a verified Google email.
- Compare the normalized email server-side.
- Do not expose this email unnecessarily in public UI.

==================================================
1. TARGET ARCHITECTURE
==================================================

Build one production-ready Next.js application containing:

1. Public marketing website
2. Blog/content pages
3. Customer Portal
4. Admin Portal
5. License management/signing service
6. Contact and support ticket system
7. Android release/download management
8. Payment-ready order model with manual payment flow initially
9. Audit logs
10. Vercel deployment configuration

Recommended stack:
- Current stable Next.js with App Router
- TypeScript strict mode
- React Server Components by default
- Tailwind CSS
- Auth.js with Google provider
- PostgreSQL through a Vercel Marketplace provider
- Prisma or Drizzle; choose one and document why
- Zod validation
- Vercel Blob or another explicitly configured object store
- Vitest or equivalent
- Playwright for critical E2E flows

Pin compatible dependency versions. Use official documentation at implementation time.

==================================================
2. REPOSITORY STRUCTURE
==================================================

Prefer:

loomora-web/
├── app/
│   ├── (marketing)/
│   ├── (auth)/
│   ├── account/
│   ├── admin/
│   ├── api/
│   ├── blog/
│   ├── download/
│   ├── pricing/
│   ├── privacy/
│   ├── terms/
│   └── support/
├── components/
├── features/
│   ├── auth/
│   ├── customers/
│   ├── licenses/
│   ├── orders/
│   ├── downloads/
│   ├── support/
│   └── audit/
├── lib/
│   ├── auth/
│   ├── db/
│   ├── license/
│   ├── security/
│   └── validation/
├── prisma/ or drizzle/
├── scripts/
├── tests/
├── docs/
├── CURRENT_TASK.md
├── PROJECT_STATUS.md
├── DEPLOYMENT.md
├── SECURITY.md
├── PRIVACY.md
├── THIRD_PARTY_NOTICES.md
└── .env.example

Never put a production private license-signing key in the repository.

==================================================
3. AUTHENTICATION AND ROLES
==================================================

Use Google login through Auth.js.

Roles:
- CUSTOMER
- SUPPORT
- ADMIN
- SUPER_ADMIN

Bootstrap rule:
- A verified Google account whose normalized email exactly equals
  `giolanhluc@gmail.com` becomes SUPER_ADMIN.
- Read this value from `process.env.SUPER_ADMIN_EMAIL`.
- Execute authorization server-side.
- Do not trust browser-supplied roles.
- Protect every admin page, server action and route handler server-side.

After bootstrap:
- Super Admin can promote/demote users.
- The final active Super Admin cannot be removed or demoted accidentally.
- Role changes create audit logs.
- Other Google accounts default to CUSTOMER.
- Customers only access their own records.

Required pages:
- Sign in
- Access denied
- Account disabled
- First-login onboarding
- Unauthorized admin state

Session requirements:
- Secure cookies
- CSRF protection according to Auth.js/current framework guidance
- Sensible session lifetime
- Sign-out
- Account-disabled state
- Last-login tracking
- No tokens/secrets in logs

==================================================
4. PUBLIC MARKETING WEBSITE
==================================================

Required pages:
1. Home
2. Features
3. Pricing
4. Download
5. Blog index
6. Blog article
7. FAQ
8. Contact
9. Privacy Policy
10. Terms of Service
11. Model/third-party attributions
12. Supported devices and limitations
13. Changelog/release notes

Home sections:
- Hero
- Product value
- Main capabilities
- Local/offline privacy
- Workflow
- Feature-status matrix
- Device/model requirements
- Pricing preview
- Download CTA
- FAQ preview
- Contact CTA

Truthfulness rules:
- Use only Available, Beta, Coming Soon.
- Do not describe heuristic/extractive insights as deep generative LLM understanding.
- Deep generative summary remains Coming Soon or Experimental until accepted separately.
- Do not claim perfect transcription or speaker identification.
- Do not claim support for every Android device.
- Do not advertise cloud sync/API when absent.
- State that AI models may require separate download/import and storage.
- State that Android AI processing occurs on-device in the current architecture.

Default language is English; prepare or implement Vietnamese localization.

==================================================
5. CUSTOMER PORTAL
==================================================

Protected routes under `/account`.

Required screens:
- Dashboard
- Profile
- My licenses
- License details
- Download license
- Download Android app
- Orders
- Order details
- Support tickets
- Ticket details
- Create ticket
- Business inquiry
- Account settings

Dashboard shows:
- License status
- Edition
- Capabilities
- Issue and expiry dates
- Device binding status if used
- Latest app release
- Recent order
- Open tickets
- Download actions

Authorization:
- Customer only sees owned licenses/orders/tickets.
- Download ownership is checked server-side.
- Use expiring signed URLs where appropriate.
- Do not expose private storage URLs.
- Customer cannot choose arbitrary capabilities or expiry.
- Customer cannot generate licenses or promote roles.

==================================================
6. ADMIN PORTAL
==================================================

Protected routes under `/admin`.

Required modules:
1. Dashboard
2. Customers
3. Users and roles
4. Licenses
5. License issuance
6. Products and editions
7. Capabilities
8. Orders
9. Manual payment confirmation
10. App releases/downloads
11. Support tickets
12. Contact leads
13. Blog/content workflow
14. Audit logs
15. Settings

Admin dashboard:
- Total customers
- Active/expiring/expired licenses
- New customers
- Pending orders
- Revenue only from real order data
- Open tickets
- Latest releases

Order statuses:
- DRAFT
- PENDING_PAYMENT
- PAID_MANUALLY
- PAID
- CANCELLED
- REFUNDED

License actions:
- Create draft
- Select customer
- Select product/edition
- Select capabilities
- Set issue/not-before/expiry dates
- Optional device binding
- Generate signed license
- Download
- Reissue
- Renew
- Suspend in web records
- Mark expired
- View immutable revision history

Offline limitation:
Suspending a web record cannot instantly disable an already issued offline license on a disconnected Android device. State this clearly in admin UI and docs. Use expiry for offline control; future online refresh/revocation is out of scope.

==================================================
7. LICENSE CONTRACT
==================================================

Use product capabilities, never runtime names.

Example capabilities:
- CORE_RECORDING
- AUDIO_EDITOR
- OFFLINE_TRANSCRIPTION
- SPEAKER_DIARIZATION
- SMART_INSIGHTS
- LLM_ENHANCED_INSIGHTS
- MODEL_PACK_STANDARD
- MODEL_PACK_ADVANCED

Forbidden capability names:
- LITERT_LM_PRO
- LLAMA_CPP_PRO
- GGUF_ACCESS

Recommended signed envelope:

{
  "payload": {
    "schemaVersion": 1,
    "licenseId": "lic_...",
    "product": "loomora",
    "edition": "pro",
    "customerId": "cus_...",
    "capabilities": ["OFFLINE_TRANSCRIPTION", "SMART_INSIGHTS"],
    "issuedAt": "ISO-8601 UTC",
    "notBefore": "ISO-8601 UTC",
    "expiresAt": "ISO-8601 UTC",
    "deviceBinding": null,
    "licenseVersion": 1
  },
  "signatureAlgorithm": "Ed25519",
  "keyId": "loomora-prod-YYYY-NN",
  "signature": "base64url-or-base64"
}

Requirements:
1. Deterministic canonical serialization.
2. Ed25519 signing.
3. Android receives the public verification key only.
4. Private key never enters Git, APK, browser bundle, logs, database plaintext or public assets.
5. Signing is server-side only.
6. Customer cannot call signing endpoints.
7. Validate all payload fields before signing.
8. Store payload, signature, keyId, payload hash, actor and timestamps.
9. Reissue creates a new immutable revision.
10. Never mutate an already signed payload.
11. Add server-side verification and tests.
12. Export an Android-compatible `.license` or JSON file.
13. Add a shared contract test using a non-production test key.

Production signing strategy:
Preferred:
- External KMS/HSM-backed signer.

Acceptable for controlled initial release:
- Encrypted private key in a Vercel Sensitive Environment Variable.
- Separate decryption secret.
- Strict server-only access.
- Key rotation documentation.

If secure server signing cannot be completed:
- Export an unsigned canonical payload.
- Sign using an external offline CLI.
- Import the signed envelope back into the portal.
- Do not fake a signature.

==================================================
8. DATABASE DESIGN
==================================================

Create migrations for at least:
- User
- Account
- Session
- VerificationToken if required
- CustomerProfile
- RoleAssignment or safe role fields
- Product
- Edition
- Capability
- EditionCapability
- License
- LicenseRevision
- LicenseCapability
- DeviceBinding
- Order
- OrderItem
- PaymentRecord
- AppRelease
- DownloadArtifact
- SupportTicket
- SupportMessage
- ContactLead
- BlogPost/content metadata
- AuditLog
- SystemSetting

Important constraints:
- Unique normalized email.
- Immutable issued license revisions.
- Transactions for issuance and order approval.
- Database constraints for ownership/integrity.
- Do not rely only on UI validation.

==================================================
9. PURCHASE FLOW
==================================================

Initial flow:
Pricing
→ choose edition
→ Google sign-in
→ create order
→ payment/contact instructions
→ admin confirms payment
→ admin issues license
→ customer downloads license

Rules:
- Start with manual payment/contact unless a real gateway is explicitly configured.
- Do not simulate payment success.
- Do not hard-code PAID.
- Buy button must not grant Pro automatically.
- Admin override requires a reason and audit record.
- Keep a modular payment-provider adapter for future integration.

==================================================
10. DOWNLOAD MANAGEMENT
==================================================

Admin can publish Android releases.

AppRelease fields:
- versionName
- versionCode
- channel: INTERNAL/BETA/STABLE
- status
- release notes
- minimum Android version
- supported ABIs
- checksum
- file size
- artifact storage reference
- publishedAt

Requirements:
- Only published artifacts appear publicly.
- Show checksum and file size.
- Validate upload type and size.
- Only authorized admins upload APKs.
- Use secure storage and download links.
- Public APK download is explicit, not accidental.
- Explain Android sideload warnings and signature verification.
- Never upload the Android signing key.

==================================================
11. SUPPORT AND CONTACT
==================================================

Public contact form:
- name
- email
- company
- topic
- message
- consent

Ticket statuses:
- OPEN
- IN_PROGRESS
- WAITING_CUSTOMER
- RESOLVED
- CLOSED

Requirements:
- Anti-spam/rate limiting
- Server-side validation
- Safe attachments if implemented
- No raw HTML injection
- Ownership checks
- Timestamped admin replies
- Email notification only if genuinely configured

==================================================
12. SECURITY REQUIREMENTS
==================================================

Mandatory:
1. Server-side RBAC.
2. Verified Google email.
3. Exact Super Admin allowlist.
4. Zod or equivalent validation.
5. Parameterized ORM queries.
6. CSRF protection per framework/auth guidance.
7. Secure headers.
8. Rate limiting for contact, support, downloads, admin mutations and license generation.
9. Audit sensitive actions.
10. No secrets in client bundles.
11. No private license key in source.
12. No raw secrets/licenses in logs.
13. Safe file upload restrictions.
14. Safe error messages.
15. Prevent IDOR.
16. Prevent open redirects.
17. Prevent privilege escalation.
18. Protect final Super Admin.
19. Document backups.
20. Run dependency audit.
21. Use Vercel server-only Sensitive Environment Variables.
22. Ignore real `.env*` files.
23. `.env.example` contains placeholders only.
24. Disable dev/test routes in production.
25. Production seed must not create fake paid users/licenses.

Create `SECURITY.md` containing threat model, auth model, secret handling, key rotation, incident response and offline-license limitations.

==================================================
13. UI/UX
==================================================

- Modern, readable, responsive and accessible
- Familiar readable font such as Inter, Geist or system sans
- Light/dark mode
- English default, Vietnamese-ready
- Keyboard navigation
- Good contrast
- Proper loading/empty/error states
- Destructive-action confirmation
- Search/filter/pagination for admin tables
- Mobile admin usability
- License status not indicated by color alone
- No fake charts or fake revenue

==================================================
14. SEO
==================================================

Implement:
- Metadata API
- Open Graph
- Twitter cards
- Sitemap
- robots.txt
- canonical URLs
- structured data where appropriate
- clean blog slugs
- image optimization

Do not index:
- `/admin`
- private `/account`
- auth callbacks
- private license/download URLs

==================================================
15. ENVIRONMENT VARIABLES
==================================================

Create `.env.example`:

DATABASE_URL=
AUTH_SECRET=
AUTH_GOOGLE_ID=
AUTH_GOOGLE_SECRET=
SUPER_ADMIN_EMAIL=giolanhluc@gmail.com
APP_BASE_URL=
LICENSE_SIGNING_MODE=
LICENSE_SIGNING_KEY_ID=
LICENSE_PUBLIC_KEY=
LICENSE_PRIVATE_KEY_ENCRYPTED=
LICENSE_PRIVATE_KEY_DECRYPTION_SECRET=
BLOB_READ_WRITE_TOKEN=
RATE_LIMIT_PROVIDER_URL=
RATE_LIMIT_PROVIDER_TOKEN=
EMAIL_PROVIDER_API_KEY=
SUPPORT_FROM_EMAIL=

Rules:
- Do not prefix secrets with `NEXT_PUBLIC_`.
- Validate required variables.
- Separate Development, Preview and Production values.
- Never use the production signing key in Preview deployments.

==================================================
16. GOOGLE OAUTH SETUP
==================================================

Document exact steps in `DEPLOYMENT.md`:
1. Create/select Google Cloud project.
2. Configure OAuth consent screen.
3. Create Web application OAuth client.
4. Add authorized origins if required.
5. Add exact local callback URI.
6. Add exact production callback URI based on final Vercel/custom domain.
7. Add test users while OAuth app is in testing mode.
8. Store Client ID and Client Secret in Vercel environment variables.
9. Never commit Client Secret.

Verify the callback route from the installed Auth.js version and official docs; do not guess it.

Bootstrap comparison:

process.env.SUPER_ADMIN_EMAIL

Production value:

giolanhluc@gmail.com

==================================================
17. VERCEL DEPLOYMENT
==================================================

Deploy after checks pass:
1. Inspect Git status.
2. Scan for secrets/private keys.
3. Connect Git repository to Vercel.
4. Connect PostgreSQL from Vercel Marketplace.
5. Configure Development/Preview/Production variables.
6. Configure Google OAuth callback for final domain.
7. Run safe production migrations.
8. Run local/CI build.
9. Deploy Preview.
10. Run Preview smoke tests.
11. Deploy Production.
12. Run Production smoke tests.
13. Confirm Google login.
14. Confirm `giolanhluc@gmail.com` becomes SUPER_ADMIN.
15. Confirm another Google account becomes CUSTOMER.
16. Confirm customer cannot access `/admin`.
17. Confirm license signing is server-only.
18. Confirm customer download ownership.
19. Record the real URL and evidence in `DEPLOYMENT.md`.

Do not claim deployment success unless the URL was actually opened and tested.

If Vercel/Google credentials are unavailable:
- Finish code and documentation.
- Run local checks.
- Mark `BLOCKED BY OWNER CREDENTIALS`.
- Provide exact remaining owner steps.
- Do not invent a URL.

==================================================
18. TESTS
==================================================

Unit/integration:
- email normalization
- verified-email requirement
- Super Admin bootstrap
- customer default role
- RBAC
- final Super Admin protection
- customer ownership
- admin license permissions
- canonicalization
- Ed25519 test fixture
- tampered payload rejection
- wrong key rejection
- capability validation
- expiry/not-before
- immutable revisions
- audit logs
- order/payment gating
- secure downloads
- rate limiting
- input validation
- production secret validation

E2E:
1. Public pages load.
2. Sign-in flow using safe test strategy.
3. Customer cannot access admin.
4. Admin dashboard protected.
5. Super Admin creates customer/license draft.
6. Customer cannot choose arbitrary capabilities.
7. License download ownership.
8. Support ticket flow.
9. Release publication/download.
10. Responsive navigation.
11. Privacy/terms/attribution pages.
12. No secret in rendered HTML/client JS.

==================================================
19. DOCUMENTATION
==================================================

Create:
- README.md
- CURRENT_TASK.md
- PROJECT_STATUS.md
- DEPLOYMENT.md
- ADMIN_GUIDE.md
- CUSTOMER_GUIDE.md
- LICENSE_OPERATIONS.md
- SECURITY.md
- PRIVACY.md
- THIRD_PARTY_NOTICES.md
- BACKUP_AND_RECOVERY.md
- KNOWN_ISSUES.md
- .env.example

`LICENSE_OPERATIONS.md` must explain:
- key creation
- public-key export to Android
- production private-key storage
- key rotation
- issuance
- renewal
- reissue
- device binding
- expiry
- offline revocation limitation
- download
- lost-key recovery
- license verification before delivery

==================================================
20. IMPLEMENTATION PHASES
==================================================

W1 — Foundation
W2 — Google Auth and RBAC
W3 — Marketing and Blog
W4 — Customer Portal
W5 — Admin Portal
W6 — License Service
W7 — Security/quality hardening
W8 — Vercel deployment and production smoke tests

Do not skip phases. Keep `CURRENT_TASK.md` and `PROJECT_STATUS.md` updated.

==================================================
21. ACCEPTANCE CRITERIA
==================================================

Complete only when:
1. Public website is responsive and truthful.
2. Google sign-in works.
3. `giolanhluc@gmail.com` is initial SUPER_ADMIN.
4. Other users default to CUSTOMER.
5. Customers cannot access admin or other customers' data.
6. Admin manages customers, products, orders, releases, tickets and licenses.
7. Licenses are canonical and signed server-side.
8. Android-compatible license downloads work.
9. Private signing key is absent from Git/browser/APK.
10. Offline revocation limitation is documented.
11. Manual purchase flow does not fake payment.
12. Sensitive actions have audit logs.
13. Migrations/tests/build pass.
14. Preview deployment is tested.
15. Production deployment is tested or honestly blocked by credentials.
16. OAuth callbacks match deployed domain.
17. Privacy, terms and attribution pages exist.
18. Admin/license operation docs are complete.

==================================================
22. MANDATORY WORK PROCESS
==================================================

Before editing:
1. Inspect source and Git status.
2. Write plan to `CURRENT_TASK.md`.
3. List acceptance criteria, migrations, security risks and tests.
4. Preserve unrelated owner changes.
5. Never commit secrets.

After implementation:
1. Run format, lint, typecheck, tests, E2E and production build.
2. Record actual commands/results.
3. Audit built client assets for secrets.
4. Update tracking/docs.
5. Report architecture, changed files, schema, migrations, auth, roles, signing mode, deployment URL/status, required variables, test results, risks and owner actions.
6. Never fabricate a passing test or deployment.
7. Never expose the private signing key.
```

## Owner notes

- Initial Super Admin Google account: `giolanhluc@gmail.com`
- Android remains offline-first.
- Website accounts are for buying, downloads, license history, support and administration.
- Android must not require login every time it opens.
- Deploy Preview before Production.
- A custom domain can be connected after the first verified Vercel deployment.
