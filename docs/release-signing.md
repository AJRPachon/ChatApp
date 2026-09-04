# Release signing (upload keystore)

ChatApp's `release` build type is signed with a dedicated **upload keystore**
(`signingConfigs.release` in `app/build.gradle.kts`). This is required to
produce a signed AAB/APK for Google Play — without it, `assembleRelease` /
`bundleRelease` fail cleanly at the `validateSigningRelease` task with
`Keystore file not set for signing config 'release'`.

The signing config reads credentials in this order per field:

1. `local.properties` (local dev) — see `local.properties.example`.
2. Environment variables (CI fallback) — no secrets need to live in the repo.

All fields are optional at the Gradle configuration level, so the project
still configures and builds (debug, tests, detekt, etc.) for anyone who
doesn't have the keystore. Only `assembleRelease`/`bundleRelease` actually
need real values.

## 1. Generate the upload keystore (one-time, per maintainer/CI)

```bash
keytool -genkeypair -v \
  -keystore upload-keystore.jks \
  -alias upload \
  -keyalg RSA -keysize 2048 \
  -validity 9125 \
  -storetype JKS
```

- `-validity 9125` ≈ 25 years, matching Google's recommendation for upload
  keys (Play App Signing keeps the *real* app signing key on Google's side;
  this upload key only needs to stay valid long enough to keep publishing
  updates — but a long validity avoids ever having to rotate it under normal
  circumstances).
- Keep the resulting `upload-keystore.jks` **out of git**. It's covered by
  `.gitignore` (`*.jks`, `*.keystore`). Store it in a password manager /
  secrets vault, plus a backup — if it's lost and Play App Signing was never
  enrolled, you can never update the app again under the same listing.
- This repo's convention is to keep the file at the project root as
  `upload-keystore.jks` (matches `RELEASE_STORE_FILE` in
  `local.properties.example`), but any path works as long as
  `RELEASE_STORE_FILE` points to it (relative paths resolve from the project
  root).

## 2. Local development

Copy `local.properties.example` to `local.properties` (already gitignored)
and fill in the real values:

```properties
RELEASE_STORE_FILE=upload-keystore.jks
RELEASE_STORE_PASSWORD=<upload-keystore-password>
RELEASE_KEY_ALIAS=upload
RELEASE_KEY_PASSWORD=<upload-key-password>
```

Leave these blank/commented if you don't have the keystore — every other
build (debug, unit tests, detekt, `assembleDebug`, etc.) works fine without
them. Only `assembleRelease`/`bundleRelease` need them.

## 3. CI (future — not wired up yet)

No CI signing logic exists yet; this section documents what a future GitHub
Actions release job would need. The signing config already supports these
environment variables as a fallback when `local.properties` isn't present:

| Env var | Purpose |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | The `upload-keystore.jks` file, base64-encoded. Decoded to a temp file under `build/release-signing/` at configure time. Lets CI avoid checking in or mounting a raw `.jks`. |
| `RELEASE_KEYSTORE_PASSWORD` | Keystore password (maps to `storePassword`). |
| `RELEASE_KEY_ALIAS` | Key alias (maps to `keyAlias`, e.g. `upload`). |
| `RELEASE_KEY_PASSWORD` | Key password (maps to `keyPassword`). |

Suggested GitHub Actions repo secrets when this is implemented:

- `RELEASE_KEYSTORE_BASE64` — `base64 -w0 upload-keystore.jks` (or
  `certutil -encode`/`[Convert]::ToBase64String` on Windows), stored as a
  secret.
- `RELEASE_KEYSTORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

The workflow would decode the secret into an env var (or a file) before
invoking `./gradlew bundleRelease`; no workflow changes have been made as
part of this task.

## 4. Google Play Console — App Signing

This project has not yet enrolled in **Play App Signing**. Before the first
upload to Play Console, a human needs to:

1. Create the app listing in Play Console.
2. Upload the first signed AAB (signed with the upload keystore above) — Play
   Console will offer to enroll in Play App Signing at that point (opt-in is
   effectively required for new apps as of the current Play Console policy).
3. Confirm enrollment in Play App Signing so Google holds/manages the actual
   app signing key, and this project's key becomes the *upload* key used only
   to authenticate uploads (which is what `signingConfigs.release` here is
   for).

Nothing in this repo change performs that enrollment — it's a manual,
one-time step in Play Console by whoever owns the developer account.
