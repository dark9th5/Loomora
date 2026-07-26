# Loomora Release Candidate (RC-1) Report

**Release Date:** 2026-07-25  
**Version Code:** 1  
**Version Name:** 1.0.0  
**Build Variant:** `release`  
**Status:** READY FOR DISTRIBUTION  

---

## 1. Release Verification Summary

- [x] Clean Release Build (`clean assembleRelease`) completed with zero errors.
- [x] ProGuard / R8 rules configured for Room, Media3 ExoPlayer, and Hilt.
- [x] Zero API keys or secrets embedded in APK package.
- [x] Room Database schema exported (`1.json`).
- [x] HTTPS enforced via `network_security_config.xml`.
- [x] Full dual-language localization (EN & VI) verified.

---

## 2. Artifact Details

| Artifact Name | Path | Build Type | Output Size |
|---|---|---|---|
| **Debug APK** | `app/build/outputs/apk/debug/app-debug.apk` | Debug | ~18.4 MB |
| **Release Unsigned APK** | `app/build/outputs/apk/release/app-release-unsigned.apk` | Release | ~14.2 MB |

---

## 3. Store Submission Checklist

- [x] Privacy Policy & Local Ownership Guarantee link verified (`https://loomora.app/privacy`).
- [x] Website Hand-off page configured (`https://loomora.app/pro`).
- [x] Automated store upload disengaged (requires user signing key for final production keystore).
