# Release Signing

Loomora release builds must never use the Android debug key. Pull-request CI may compile an unsigned release artifact, but a publishable release requires a production keystore supplied outside the repository.

## Required Inputs

Use these exact names as environment variables or keys in local `keystore.properties`:

- `LOOMORA_STORE_FILE`
- `LOOMORA_STORE_PASSWORD`
- `LOOMORA_KEY_ALIAS`
- `LOOMORA_KEY_PASSWORD`

GitHub release builds also require `LOOMORA_STORE_FILE_BASE64`, a base64-encoded copy of the keystore stored as a repository secret.

## Create A Key

Create the production key once and store it outside the repo:

```bash
keytool -genkeypair \
  -v \
  -keystore loomora-release.jks \
  -storetype JKS \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -alias loomora-release
```

Choose strong unique passwords. Do not reuse debug, personal, or test keystores for production.

## Backup Policy

Back up the keystore and passwords in at least two protected locations, such as an organization password manager and encrypted offline storage. Losing the release key can permanently block updates for already published apps.

Do not rotate or replace the production signing key after publishing unless the distribution channel explicitly supports key upgrade and the release owner has approved it.

## Local Signed Build

Create an untracked `keystore.properties` file at the repo root:

```properties
LOOMORA_STORE_FILE=C:\\secure\\loomora-release.jks
LOOMORA_STORE_PASSWORD=<store-password>
LOOMORA_KEY_ALIAS=loomora-release
LOOMORA_KEY_PASSWORD=<key-password>
```

Then run:

```bash
./gradlew :app:validateReleaseSigning assembleRelease
```

If `keystore.properties` is absent, `./gradlew assembleRelease` still compiles an unsigned release artifact for CI validation.

## GitHub Secrets

Configure these repository secrets before running `.github/workflows/release.yml`:

- `LOOMORA_STORE_FILE_BASE64`
- `LOOMORA_STORE_PASSWORD`
- `LOOMORA_KEY_ALIAS`
- `LOOMORA_KEY_PASSWORD`

Create the base64 value locally from the production keystore. On Linux/macOS:

```bash
base64 -w 0 loomora-release.jks
```

On Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\\secure\\loomora-release.jks"))
```

The release workflow decodes the keystore into the runner workspace, validates all signing inputs, and fails before building if a required secret is missing.

## R8 Hardening

R8/minification remains disabled in P0.2 because the app does not yet have enough regression coverage for release shrinker hardening. Enable and verify R8 in a later hardening task with release install smoke tests.
