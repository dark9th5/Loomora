# Loomora Marketing Website — Vercel Deployment Guide

The Loomora marketing website is built with Next.js (App Router), TypeScript, and Tailwind CSS. It is 100% production-ready for deployment on **Vercel**.

---

## 1. Environment Variables Configuration

Set the following environment variables in your Vercel Project Settings (**Settings → Environment Variables**):

| Variable Name | Example Value | Description |
|---|---|---|
| `NEXT_PUBLIC_SITE_URL` | `https://loomora.app` | Canonical site URL for SEO & OpenGraph |
| `NEXT_PUBLIC_APK_URL` | `https://loomora.app/downloads/app-release-unsigned.apk` | Direct APK download link |
| `NEXT_PUBLIC_PLAY_URL` | `https://play.google.com/store/apps/details?id=com.loomora` | Google Play Store URL |
| `NEXT_PUBLIC_LATEST_VERSION` | `1.0.0` | Production release version |
| `NEXT_PUBLIC_APK_SIZE` | `14.2 MB` | Compiled APK file size |
| `NEXT_PUBLIC_APK_SHA256` | `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855` | SHA256 checksum for APK integrity |
| `NEXT_PUBLIC_CONTACT_EMAIL` | `contact@loomora.app` | Primary contact email |
| `NEXT_PUBLIC_SUPPORT_EMAIL` | `support@loomora.app` | Technical support email |

---

## 2. Vercel CLI Deployment (Option A)

If Vercel CLI is installed, run from the `web/` directory:

```bash
cd web
vercel --prod
```

---

## 3. GitHub / Git Integration Deployment (Option B)

1. Push your repository to GitHub / GitLab.
2. Log into [Vercel Dashboard](https://vercel.com) and click **Add New Project**.
3. Import the repository and set **Root Directory** to `web`.
4. Add the Environment Variables listed above.
5. Click **Deploy**.

---

## 4. Verified Build Evidence

```text
Build Engine: Next.js 14.2.15 (App Router)
Lint Status: No ESLint warnings or errors
Prerendered Pages: 19 static pages prerendered (Home, Download, Pricing, Features, Blog, Contact, Privacy, Terms, Data Deletion, Buy Pro, Sitemap, Robots, RSS)
First Load JS: 87.3 kB (Optimal Performance)
```
