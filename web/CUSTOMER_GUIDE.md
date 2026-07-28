# Loomora Customer Guide

Welcome to Loomora. This guide explains how to use the Loomora web portal to purchase Pro licenses, download signed license files, get Android APK releases, and manage your account.

---

## 1. Offline-First Philosophy

- **No Web Account Required for Recording**: The Loomora Android app records audio, organizes notes, and plays back recordings completely offline without creating a web account.
- **Web Account Purpose**: Web accounts are used exclusively for purchasing Pro licenses, downloading signed `.license` envelopes, obtaining official APK updates, and accessing technical support.

---

## 2. Customer Journey Workflow

```
1. Download App -> 2. Explore Free/Trial -> 3. Purchase Pro Order -> 4. Admin Payment Confirmation -> 5. Download .license -> 6. Import to Android
```

### Step 1: Free Download
- Download the official APK from [Loomora Download](/download) or your [Customer Dashboard](/account/downloads).
- Free features include unlimited local recording, Media3 ExoPlayer playback, non-destructive editing, and English/Vietnamese localization.

### Step 2: Included In-App Trial
- Android app includes 3 free trial uses of premium features (offline transcription, extractive insights).
- Trial usage is tracked idempotently on your local device.

### Step 3: Purchase Pro License
- Navigate to [Pricing](/pricing) or [Buy Pro](/pro).
- Select a Pro edition plan and submit an order (`POST /api/orders`).
- Order is created with status `PENDING_PAYMENT`.

### Step 4: Payment Confirmation
- Follow payment instructions or contact support with your payment reference.
- An administrator confirms payment manually (`PAID_MANUALLY`).
- Admin issues your signed Ed25519 license.

### Step 5: Download License File
- Sign in to your account at `/account`.
- Go to **My Licenses** (`/account/licenses`).
- Click **Download .license file**.
- A signed `lic_xxx.license` JSON file will download to your device.

### Step 6: Import into Android App
- Open Loomora on your Android device.
- Navigate to **Settings > License Management**.
- Select **Import License File** and choose the downloaded `lic_xxx.license` file.
- The app verifies the digital signature offline using its built-in Ed25519 public key and unlocks Pro capabilities immediately.

---

## 3. Support & Account Management

- **Support Tickets**: Open a ticket via `/account/support/new`. View status and replies in `/account/support`.
- **Account Settings**: Update company, phone, or country details at `/account/settings`.
- **Privacy & Data**: Your voice recordings never leave your Android device. Web account data is managed under our [Privacy Policy](/privacy).
