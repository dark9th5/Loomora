# Offline License Limitations

P2.6 verifies signed license envelopes fully on device and does not require a network request.

Important limits:

- Offline licenses cannot be revoked immediately. The app can enforce `notBefore` and `expiresAt`; server-side refresh/revocation is out of scope for the offline path.
- Clock rollback detection is best effort. Loomora records the last seen wall clock and treats significant backwards movement as suspicious for paid capabilities, but it cannot prove clock integrity without an online trusted time source.
- Device binding is currently not enabled. License payloads should use `deviceBinding: null` until Loomora has a user-visible transfer/reissue policy.
- Local persistence is not tamper-proof DRM. DataStore/Room persistence can be protected further with Android Keystore in a later hardening task, but a fully offline client cannot make absolute anti-tamper guarantees.
- Reinstall or backup/restore behavior is device/platform dependent. Without an activation server, Loomora must not promise that trial state cannot be reset by reinstalling.
- Free recording, playback, and library access must continue to work when a license is missing, invalid, expired, malformed, or for a different product.

Private signing keys must never be stored in the app, repository, production test fixtures, CI logs, or APK. The app stores only public verification keys by `keyId`.
