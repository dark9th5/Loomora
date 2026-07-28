# Loomora Privacy Policy

**Effective Date**: July 28, 2026

Loomora is designed from the ground up with a **100% Local-First** privacy philosophy. This document explains how data is handled across the Loomora Android application and the Loomora Web Business Portal.

---

## 1. Local-First Android Architecture

- **Voice Recordings**: All voice recordings, audio notes, edits, and metadata remain stored exclusively inside your Android device's private internal storage (`/data/data/com.loomora.app/`).
- **No Cloud Upload**: Audio recordings are NEVER uploaded to cloud servers for storage, processing, or training.
- **On-Device AI Processing**: Speech transcription, speaker diarization, and smart extractive summaries run locally on your device when compatible models are installed.
- **No Tracking or Analytics**: The Android application contains no third-party telemetry, tracking SDKs, or background behavioral analytics.

---

## 2. Web Portal Data Handling

When you interact with the Loomora Web Business Portal (`https://loomora.app`):

### Account Information
- Sign-in is handled exclusively through **Google OAuth 2.0**.
- We store your name, Google primary email address, profile image URL, and role (`CUSTOMER`, `SUPPORT`, `ADMIN`, `SUPER_ADMIN`).
- Browser-supplied roles are ignored; authorization is enforced strictly server-side.

### Licensing & Orders
- When placing an order, we store order records, edition selection, and order status (`PENDING_PAYMENT`, `PAID_MANUALLY`, `PAID`, `CANCELLED`, `REFUNDED`).
- Signed `.license` envelopes record customer ID, edition capabilities, issued date, expiry date, and optional hardware device binding digest.

### Support & Contact
- Support tickets and contact form submissions record name, email, company (optional), topic, subject, message body, and consent confirmation.
- Submissions are stored in PostgreSQL and accessible only to authorized support staff and administrators.

---

## 3. Data Ownership & Deletion

- **Android Audio Files**: You retain 100% ownership of all recorded audio. Deleting a recording in the app permanently deletes the file from local storage.
- **Web Account Deletion**: You can request complete deletion of your web portal account and support ticket history at `/data-deletion` or by contacting `support@loomora.app`.

---

## 4. Contact Information

For privacy questions or data deletion requests:
- **Email**: `privacy@loomora.app` / `support@loomora.app`
- **Address**: Loomora Audio Security & Privacy Team
