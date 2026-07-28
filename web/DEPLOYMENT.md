# Loomora Vercel & Production Deployment Guide

This guide details the complete process for deploying the Loomora Web Business Portal to Vercel, configuring PostgreSQL, setting up Google OAuth, and establishing license signing material.

---

## 1. Owner Credentials Checklist

Before initiating deployment, the project owner must provide the following infrastructure resources:

- [ ] **Vercel Account & Project**: Connected to the Git repository.
- [ ] **PostgreSQL Database**: Provisioned via Vercel Marketplace (Neon, Supabase) or external PostgreSQL 15+.
- [ ] **Google Cloud Console Access**: OAuth 2.0 Web Application client.
- [ ] **Auth Secret**: 32+ character random string (`openssl rand -hex 32`).
- [ ] **Vercel Blob / S3 Storage**: Token for Android APK and license envelope storage.
- [ ] **License Signing Key Material**: Key ID, public key, and encrypted private key with decryption secret.

---

## 2. Google OAuth Configuration Steps

1. Log in to [Google Cloud Console](https://console.cloud.google.com/).
2. Create a project named **`Loomora Portal`** (or select existing).
3. Navigate to **APIs & Services > OAuth consent screen**:
   - User Type: **External**.
   - App Name: `Loomora`.
   - User support email: `giolanhluc@gmail.com`.
   - Developer contact email: `giolanhluc@gmail.com`.
   - Scopes: `openid`, `https://www.googleapis.com/auth/userinfo.email`, `https://www.googleapis.com/auth/userinfo.profile`.
4. Navigate to **APIs & Services > Credentials > Create Credentials > OAuth client ID**:
   - Application type: **Web application**.
   - Name: `Loomora Web Portal`.
   - **Authorized JavaScript origins**:
     - Local: `http://localhost:3000`
     - Production: `https://<your-vercel-domain>.vercel.app` (and custom domain if applicable)
   - **Authorized redirect URIs**:
     - Local: `http://localhost:3000/api/auth/callback/google`
     - Production: `https://<your-vercel-domain>.vercel.app/api/auth/callback/google`
5. Copy **Client ID** (`AUTH_GOOGLE_ID`) and **Client Secret** (`AUTH_GOOGLE_SECRET`).
6. Set `SUPER_ADMIN_EMAIL=giolanhluc@gmail.com`.

---

## 3. Vercel Deployment Checklist (19-Step Pipeline)

1. [ ] Connect Git repository to Vercel.
2. [ ] Set Framework Preset to **Next.js**.
3. [ ] Set Root Directory to `web` (or `./` if working from web directory).
4. [ ] Configure Environment Variables for Production & Preview:
   - `DATABASE_URL`
   - `AUTH_SECRET`
   - `AUTH_GOOGLE_ID`
   - `AUTH_GOOGLE_SECRET`
   - `SUPER_ADMIN_EMAIL=giolanhluc@gmail.com`
   - `APP_BASE_URL=https://<your-domain>.com`
   - `LICENSE_SIGNING_MODE=encrypted-env`
   - `LICENSE_SIGNING_KEY_ID`
   - `LICENSE_PUBLIC_KEY`
   - `LICENSE_PRIVATE_KEY_ENCRYPTED`
   - `LICENSE_PRIVATE_KEY_DECRYPTION_SECRET`
   - `BLOB_READ_WRITE_TOKEN`
5. [ ] Do NOT use production license signing keys in Preview environments.
6. [ ] Run Prisma code generation: `npx prisma generate`.
7. [ ] Run safe database migrations: `npx prisma migrate deploy`.
8. [ ] Execute build command: `npm run build`.
9. [ ] Deploy to Vercel Preview environment.
10. [ ] Verify OAuth login flow on Preview URL with `giolanhluc@gmail.com`.
11. [ ] Confirm bootstrap user is assigned `SUPER_ADMIN` role automatically.
12. [ ] Test Admin Portal access (`/admin`) on Preview.
13. [ ] Place a test order on Customer Portal (`/pricing` -> `/account/orders`).
14. [ ] Admin confirms manual payment (`/admin/orders`).
15. [ ] Admin issues a signed test license (`/admin/licenses`).
16. [ ] Customer downloads `.license` file (`/account/licenses`).
17. [ ] Verify `.license` signature using Ed25519 public key.
18. [ ] Promote Preview build to **Production**.
19. [ ] Run full smoke test on Production live domain.

---

## 4. Local Verification Status

- `npm run prisma:generate`: PASS
- `npm run test`: PASS (53 unit & integration tests)
- `npm run typecheck`: PASS (0 errors)
- `npm run lint`: PASS
- `npm run build`: PASS
- `npm audit --omit=dev`: PASS (0 vulnerabilities)

*Current Status*: **BLOCKED BY OWNER CREDENTIALS**. Code and build are fully production-ready. Deployment will proceed as soon as owner environment variables are populated.
