# Play Console Data Safety form — mapping from real code

Generated from a direct audit of the ChatApp codebase (branch `develop`, commit `2c9ff49` at time of
writing; updated 2026-09-05 after PRs #81/#83/#84/#86 added Firebase Analytics/Crashlytics and
account deletion — see §2, §3, §5.6). Every claim below is backed by a file path so it can be
re-verified. This is **not** a finished declaration — it is the field-by-field input a human
transcribes into Play Console (App content → Data safety), plus explicit flags on anything ambiguous.

**Do not transcribe this blind.** Re-run this audit before every release that adds a permission, a
dependency, or a new use of an existing sensitive permission — see the checklist at the end.

---

## 1. Permissions — declared vs. actually used

Source: `app/src/main/AndroidManifest.xml`

| Permission | Declared | Runtime request site | Real feature | Verdict |
|---|---|---|---|---|
| `INTERNET` | Yes | N/A (normal permission) | All network calls (Supabase, LiveKit, Giphy, Drive, FCM, ML Kit model download) | Clear |
| `POST_NOTIFICATIONS` | Yes | `MainActivity.kt:322-325` | Message/call push notifications | Clear |
| `READ_CONTACTS` | Yes | `ChatScreen.kt` (`contactPermissionLauncher`, for `ActivityResultContracts.PickContact()`) and `NewChatViewModel.kt:109` (bulk contact read via `GetDeviceContactsUseCase`) | Two distinct uses — see §5.1 | Clear, but two different sub-uses need separate justification text |
| `CAMERA` | Yes | `ChatScreen.kt:314-341` (photo/video capture buttons) | Send photo/video in chat; also used by LiveKit video calls (`CallViewModel.kt`) and ZXing QR scanning | Clear |
| `RECORD_AUDIO` | Yes | `ChatScreen.kt:324-331` (mic button), `CallViewModel.kt` (LiveKit calls) | Voice messages (`AudioRecorderRepositoryImpl.kt`), voice/video calls, on-device dictation (`AudioTranscriber.kt`) | Clear, but see §5.4 for a real bug in how it's used |
| `MODIFY_AUDIO_SETTINGS` | Yes | Not explicitly checked (normal permission, no runtime prompt) | Call audio routing (LiveKit) | Clear — normal permission, not part of the Data Safety form |
| `BLUETOOTH_CONNECT` | Yes | **None found anywhere in app code** | Presumed: LiveKit/WebRTC Bluetooth SCO audio routing during calls (Android 12+ requires this to enumerate/route to Bluetooth headsets) | **AMBIGUOUS — see §5.2** |
| `USE_FULL_SCREEN_INTENT` | Yes | Not explicitly checked (declared permission, used implicitly by notification builder for incoming-call full-screen UI) | Incoming call full-screen notification | Clear, not part of Data Safety form |
| `ACCESS_FINE_LOCATION` | Yes | `ChatScreen.kt:344-350` (`locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)`) | "Share my location" in chat | Clear |
| `ACCESS_COARSE_LOCATION` | Yes | **Never requested independently** — only appears because Android bundles it into the same permission group as `ACCESS_FINE_LOCATION` (the system dialog itself lets the user pick "Precise"/"Approximate") | Fallback only | **See §5.3 — confirm this is intentional, not leftover** |

### Location fetch mechanism (for the form's precision question)

`ChatQuickSendDelegate.kt:91-96` — one-shot fetch via `android.location.LocationManager
.getLastKnownLocation()`, tried against `GPS_PROVIDER` first, then `NETWORK_PROVIDER` as fallback. This
is **not continuous tracking** — no `requestLocationUpdates`, no background location, no
`ACCESS_BACKGROUND_LOCATION` permission requested or declared. Location is fetched only when the user
taps "share location" in a chat, and is sent once as message content to the conversation.

---

## 2. Third-party SDK / service catalog

Source: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/java/.../di/AppModule.kt`,
`SharedModules.kt`.

| SDK / service | Version | Wired in | What actually leaves the device | Notes |
|---|---|---|---|---|
| Supabase (Auth, Postgrest, Realtime, Storage, Functions) | 3.8.0 | `AppModule.kt` `networkModule` | Account credentials, message content, media files, device session info, FCM tokens — this is the app's own backend | Treat as first-party processor, not "third-party sharing" for the form, but every data type it touches must still be declared as *collected* |
| LiveKit Android SDK | 2.28.1 | `AppModule.kt` (`CallViewModel` lambda, `BuildConfig.LIVEKIT_URL`) | Live audio/video streams during calls, routed through a LiveKit server (self-hosted or LiveKit Cloud — confirm which in `local.properties`/infra) | Token minted server-side via Edge Function (`livekit-token`), not embedded in the app |
| Firebase Cloud Messaging | via `firebaseBom` 34.18.0 | `ChatFirebaseMessagingService.kt`, `FcmTokenRepository` | FCM registration token, synced to Supabase (`FcmTokenRepositoryImpl`) | |
| Firebase Analytics | via `firebaseBom` 34.18.0 (added since the last audit — was absent, now present) | `AppModule.kt` `analyticsModule`, `FirebaseAnalyticsTracker.kt`, `AnalyticsEvents.kt` | Event/type metadata only per the class's own doc comment ("Deliberately does NOT log message content, conversation ids or user-identifying values — only event/type metadata") — screen views, auth method, message *type* (not content), call type/status/duration, group participant count, invitation sent/accepted, status posted. `setUserId()` is called on login/logout, linking events to the app's internal user id while signed in | `com.google.android.gms.permission.AD_ID` is **not** declared in the manifest (confirmed via grep) — Firebase Analytics does not auto-collect the Advertising ID as a result. Declare "User IDs" as shared with Firebase for Analytics purpose |
| Firebase Crashlytics | via `firebaseBom` 34.18.0 (added since the last audit — was absent, now present) | `AppModule.kt` `analyticsModule`, `FirebaseCrashReporter.kt` | Crash/exception stack traces, device model/OS version, the app's internal user id (`setUserId()`, mirrors the Analytics one). `MessageE2EECoder.kt` also logs breadcrumb messages on E2EE encrypt/decrypt failures that include internal sender/recipient user ids (not message content) | Declare "Crash logs and diagnostics" and "Device or other IDs" as shared with Firebase for the app-functionality/diagnostics purpose |
| Google Sign-In / Credential Manager | `credentials` 1.6.0, `googleid` 1.2.0 | `AppModule.kt` (`CredentialManager.create`), `AuthViewModel.kt` | Email, display name, profile photo URL from the user's Google account, ID token | Standard OAuth sign-in |
| Giphy (REST API, **no bundled SDK**) | N/A — plain Ktor/OkHttp calls to `api.giphy.com` | `GiphyRemoteSource.kt` | GIF search query text + app API key (`GIPHY_API_KEY`) | No user PII sent — just the search string. Still counts as data sent to a third party under Play's rules |
| ML Kit Translation | `com.google.mlkit:translate:17.0.3` | `TranslationManager.kt` | **Nothing** — on-device model, downloaded once, translation runs locally (doc comment confirms: "On-device translation using ML Kit") | Verify no telemetry opt-out is needed; ML Kit's own model-download step does contact Google servers to fetch the language model file, not to transmit user text |
| Google Drive (REST API, **no bundled SDK** — no `play-services-drive`/Drive client library dependency) | N/A — raw OkHttp calls to `www.googleapis.com/drive/v3` | `BackupRepositoryImpl.kt` | **All locally stored messages** (`messageDao.getAllMessages()`), serialized to JSON and uploaded to the signed-in Google account's own Drive, scope `drive.file` (app-created-files only, not full Drive access) | See §5.5 — this is a major, easy-to-miss "Messages" sharing path |
| Play Integrity API | `com.google.android.play:integrity` 1.6.0 | `IntegrityChecker.kt` | An integrity token verified via the `verify-integrity` Edge Function (device/app attestation verdict) | Device attestation, not user content |
| ZXing (`com.journeyapps:zxing-android-embedded`) | 4.3.0 | QR code scanning | Nothing — processes camera frames locally | |
| qrcode-kotlin | 4.1.1 | QR code generation (e.g. "add me" QR) | Nothing — pure local generation | |
| Media3/ExoPlayer | 1.5.1 | In-app video playback | Nothing — plays already-fetched media URLs | |
| Coil | 3.6.1 | Image loading | Fetches media from Supabase Storage / Giphy CDN URLs the app already has | |
| Android `SpeechRecognizer` (OS API, not a bundled SDK) | N/A | `AudioTranscriber.kt` | Live mic audio, handed to the OS speech-recognition service (which may itself use Google's cloud STT depending on device/OS settings — outside the app's control) | **Bug**: per the file's own doc comment and `docs/audio-transcription-todo.md`, this is currently used to "transcribe a received voice message" but actually records the *user's own* mic instead — a functional bug, not a privacy one, but worth fixing before relying on the RECORD_AUDIO justification text being accurate |
| AI Assistant (Supabase Edge Function `ai-assistant`) | N/A | `AiAssistantRepository.kt` → `supabase/functions/ai-assistant/index.ts` | Message snippets / prompts sent to the Edge Function | **Currently returns mocked responses only** — `index.ts` line 4: `// TODO: replace mock responses with real LLM API calls (e.g. OpenAI, Anthropic)`. No third-party LLM is wired in yet. **This is a ticking time bomb for the Data Safety form**: the day a real LLM API call is added, "Messages" must be declared as shared with that LLM provider, purpose "App functionality", and this document must be regenerated |

---

## 3. Data type mapping (Play Console taxonomy)

Purpose values below use Play's exact category names: **App functionality, Analytics, Developer
communications, Advertising or marketing, Fraud prevention/security/compliance, Personalization,
Account management**.

### Personal info

| Data type | Collected? | Shared? | Purpose | Optional/Required | Encrypted in transit | User can request deletion |
|---|---|---|---|---|---|---|
| Name | Yes — display name/username (`SetUsernameUseCase.kt`, `ProfileViewModel.kt`) | No third-party sharing beyond Supabase (own backend) | Account management, App functionality | Required (needed to create/use account) | Yes (TLS, cert-pinned — see §4) | Yes — in-app account deletion (§5.6) |
| Email address | Yes — signup and Google Sign-In (`AuthRepository.kt`) | No | Account management | Required | Yes | Yes — in-app account deletion (§5.6) |
| User IDs | Yes — Supabase auth UUID | No | Account management, App functionality | Required | Yes | Yes — in-app account deletion (§5.6) |
| Address | Not collected | — | — | — | — | — |
| Phone number | **Read from device contacts, but NOT transmitted off-device** (`ContactRepositoryImpl.getContacts()` stays local, only rendered in `NewChatViewModel`'s contact list UI — confirmed no network call carries phone numbers) | No | — (local-only use, arguably still "collected" for Play's purposes since it's read into app memory — declare defensively) | Optional (contact-picker feature) | N/A (never leaves device) | N/A |
| Other personal info (profile photo) | Yes — avatar image uploaded to Supabase Storage | No | Account management, App functionality | Optional | Yes | Yes — in-app account deletion (§5.6) |

### Contacts

| Data type | Collected? | Shared? | Purpose | Optional/Required | Encrypted in transit | User can request deletion |
|---|---|---|---|---|---|---|
| Contacts | Yes — two distinct uses (see §5.1): (a) device contact **emails** read and sent to Supabase to find "suggested" app users (`ContactSyncManager.kt` → `NewChatViewModel.kt:239-244` → `UserRepositoryImpl.searchUsersByEmails` → Postgrest); (b) device contact **phone numbers/names** read for the local "new chat" picker list, never transmitted | Emails: sent to Supabase (declare as collected; Supabase is the app's own backend, not classic third-party ad-sharing, but still must be declared). Phone numbers: not shared | App functionality (find people you know who use the app) | Optional (feature only runs if READ_CONTACTS is granted; app functions without it) | Yes, for the email-matching call | No explicit way to purge matched-contact data server-side found — verify |

### Messages

| Data type | Collected? | Shared? | Purpose | Optional/Required | Encrypted in transit | User can request deletion |
|---|---|---|---|---|---|---|
| Messages (chat text, including 1:1 and group) | **Yes — always, regardless of E2EE.** Stored locally (SQLCipher-encrypted Room DB) and on the Supabase backend (`MessageRepositoryImpl.kt`, `messages` table). This must be declared as "Messages" collection even though 1:1 content is end-to-end encrypted — see §4 for why encryption-in-transit is a separate form field, not a substitute | No sharing with unrelated third parties today. (a) If the Google Drive backup feature is used, message content is copied to the user's own Google Drive — see §5.5. (b) The `ai-assistant` Edge Function receives message snippets when the AI features are used, but currently only reaches a mock, not a real third-party LLM (§2) | App functionality | Required (core feature — cannot be made "optional" and still be a chat app) | Yes (TLS, cert-pinned to `*.supabase.co`) | Per-message/per-conversation delete exists for individual messages; a full erasure of a user's own message history (content + attachments, across every conversation) now happens as part of in-app account deletion (§5.6) — there is still no standalone "wipe my message history but keep my account" action, only via full account deletion |

### Photos and videos

| Data type | Collected? | Shared? | Purpose | Optional/Required | Encrypted in transit | User can request deletion |
|---|---|---|---|---|---|---|
| Photos and videos | Yes — chat attachments, status/stories media, profile avatars, group photos, all uploaded to Supabase Storage (`MessageRemoteSource.kt`, `StatusRemoteSource.kt`, `GroupRemoteSource.kt`, `UserRemoteSource.kt`) | No third-party sharing (Supabase Storage is first-party backend). Copied to Google Drive if the user runs a backup **only for message metadata/URLs, not the raw media bytes** — confirm this, since `BackupRepositoryImpl.kt` backs up `MessageDBO` rows which include `imageUrl`/`videoUrl` strings, not the files themselves | App functionality | Optional (only when user attaches media) | Yes | No explicit bulk-delete-all-my-media flow found — verify |

### Audio files

| Data type | Collected? | Shared? | Purpose | Optional/Required | Encrypted in transit | User can request deletion |
|---|---|---|---|---|---|---|
| Audio files (voice messages) | Yes — recorded via `AudioRecorderRepositoryImpl.kt`, uploaded to Supabase Storage | No third-party sharing | App functionality | Optional (only when user records a voice message) | Yes | Verify retention/deletion |
| Audio files (live call audio) | Yes, transiently — routed through LiveKit during calls, not stored by the app itself (confirm LiveKit server-side recording is off unless a "call recording" feature is explicitly enabled — check `CallViewModel.kt` for a recording flag) | Yes — to the LiveKit server (self-hosted or LiveKit Cloud) | App functionality | Required for the calling feature specifically, optional overall (app works without ever placing a call) | Yes (LiveKit uses SRTP/DTLS + the app's own cert pinning to `*.livekit.cloud`) | N/A if not stored |

### Files and documents

| Data type | Collected? | Shared? | Purpose | Optional/Required | Encrypted in transit | User can request deletion |
|---|---|---|---|---|---|---|
| Files and docs | Yes — generic file attachments (`fileUrl`/`fileName`/`fileMimeType` fields on messages), uploaded to Supabase Storage | No | App functionality | Optional | Yes | Verify retention/deletion |

### App activity

| Data type | Collected? | Shared? | Purpose | Optional/Required | Encrypted in transit | User can request deletion |
|---|---|---|---|---|---|---|
| App interactions / in-app search history | Yes, narrowly — GIF search queries sent to Giphy's API (`GiphyRemoteSource.kt`); global in-app message search stays local (`GlobalSearchViewModel` — confirm it doesn't hit network) | Yes — Giphy (third party) receives search query text | App functionality | Optional (only if user searches GIFs) | Yes (HTTPS to `api.giphy.com`) | N/A (Giphy's own retention policy applies — not controlled by this app) |
| Analytics / product usage telemetry | **Yes, since PR #81** — Firebase Analytics event/type metadata (screen views, auth method, message *type*, call type/status/duration, group size, invitation/status events — never message content). See §2 Firebase Analytics row | Yes — Firebase (Google) | Analytics | Optional in the sense that it doesn't gate any feature, but there's no in-app opt-out found — verify whether one should be added | Yes | Deleted along with the account (Firebase Analytics data tied to the internal user id stops accumulating; historical aggregate data in Firebase's own retention window is Google's, not directly purgeable per-user from the app) |
| Crash logs / diagnostics | **Yes, since PR #81** — Firebase Crashlytics: stack traces, device model/OS version, internal user id. See §2 Firebase Crashlytics row | Yes — Firebase (Google) | Fraud prevention/security/compliance (Play's closest category for crash diagnostics) | Optional in the same sense as above — no in-app opt-out found | Yes | Same as Analytics above |

### Device or other IDs

| Data type | Collected? | Shared? | Purpose | Optional/Required | Encrypted in transit | User can request deletion |
|---|---|---|---|---|---|---|
| Device or other IDs | Yes, three distinct sources: (a) FCM registration token, synced to Supabase for push delivery (`FcmTokenRepositoryImpl`); (b) device model string (`Build.MANUFACTURER` + `Build.MODEL`) stored per login session for the "active sessions" security screen (`SessionAuditViewModel.kt:30`, `SessionRepositoryImpl.kt`); (c) Play Integrity attestation token, verified server-side (`IntegrityChecker.kt`) | (a),(b) sent to Supabase (own backend). (c) processed by Google Play Integrity API (Google) plus the app's own Edge Function | (a) App functionality; (b) Account management, Fraud prevention/security/compliance; (c) Fraud prevention/security/compliance | Required for push notifications and session security; Play Integrity check likely required to use the app at all — verify enforcement policy in `AuthViewModel.kt` | Yes | User can revoke individual sessions in-app (`SessionAuditViewModel` `RevokeSession`/`RevokeAllOtherSessions`) — this is a partial "deletion" capability worth citing in the form |

### Location

| Data type | Collected? | Shared? | Purpose | Optional/Required | Encrypted in transit | User can request deletion |
|---|---|---|---|---|---|---|
| Location — approximate | Effectively yes, as a fallback: `LocationManager.getLastKnownLocation(NETWORK_PROVIDER)` may return a coarse/network-derived fix (`ChatQuickSendDelegate.kt:93-96`) | Shared with the specific chat recipient(s) as message content (and transits Supabase like any message) | App functionality | Optional — only when the user explicitly taps "share location" | Yes | Deletable the same way any message is deletable (per-message delete) — verify |
| Location — precise | Yes — primary path via `GPS_PROVIDER` last-known fix | Same as above | App functionality | Optional | Yes | Same as above |

Location is **never** collected in the background, continuously, or for any purpose other than the
explicit "share my location" chat action.

### Health and fitness, Financial info, Web browsing

Not collected — no code path touches any of these categories.

---

## 4. Message content vs. "encrypted in transit" — do not conflate these two form fields

- `E2EEKeyManager.kt`: 1:1 conversations use ECDH (secp256r1) key exchange + HKDF (HMAC-SHA256) +
  AES-256-GCM, keys held in Android Keystore. This means the Supabase backend stores **ciphertext** for
  1:1 message bodies and cannot read them.
- **Group conversations are not end-to-end encrypted** (`SendMessageUseCase.kt` comment: "E2EE: pass the
  other user's ID for 1:1 conversations (null for group chats)"). Group message content is
  readable server-side (protected only by TLS in transit and Supabase's own at-rest storage encryption
  + RLS policies, not by E2EE).
- Regardless of any of the above, Play's Data Safety form has a **separate checkbox for "data is
  encrypted in transit"** from the **collection declaration for "Messages"**. Declaring "encrypted in
  transit: yes" does NOT let you skip declaring that Messages are collected — both the 1:1 (E2EE) and
  group (non-E2EE) cases must be declared as "Messages" collected/stored, because the app and its
  backend do receive, store, and process message data (Supabase stores it — ciphertext or plaintext —
  and the local Room DB caches it too, itself protected by SQLCipher).

---

## 5. Explicit ambiguity / rejection-risk flags

### 5.1 `READ_CONTACTS` has two different sub-uses that need two different justification statements

- Bulk contact **email** read + upload to Supabase to suggest people the user already knows
  (`ContactSyncManager.readContactEmails()` → `NewChatViewModel.loadSuggestedContacts()`). This is the
  higher-risk use — it transmits contact data off-device automatically once the user opens the "new
  chat" screen (not per-contact opt-in).
- Bulk contact **phone number** read (`ContactRepositoryImpl.getContacts()`), used only to render a
  local picker — never transmitted. Lower risk, but Play's reviewers may not distinguish sub-uses
  automatically; the in-app permission rationale text and the Play listing's permission justification
  should explicitly describe **both** uses, or a reviewer may assume the worse one applies to all
  contact access.
- Also uses Android's system `ActivityResultContracts.PickContact()` picker for "attach a contact to a
  message" — this is the lowest-risk pattern (OS-mediated, single-contact, explicit user pick) and
  technically doesn't even require the `READ_CONTACTS` permission by itself (the picker intent grants
  temporary URI access) — but the current code also requests `READ_CONTACTS` around it
  (`contactPermissionLauncher` in `ChatScreen.kt:254-260`), which may be broader than necessary for that
  specific flow. Worth a closer look at whether that particular permission check is even needed for the
  picker to work.

### 5.2 `BLUETOOTH_CONNECT` — no justifying code found anywhere in the app

Grepped the entire repository: the only occurrence of `BLUETOOTH_CONNECT` is the manifest declaration.
No `checkSelfPermission`, no `ActivityResultContracts.RequestPermission()` call, nothing. This is
almost certainly pulled in for LiveKit/WebRTC's Bluetooth SCO audio-routing during calls (needed on
Android 12+ to detect/route audio to a Bluetooth headset), which LiveKit's own manifest may declare via
manifest merging, or which this app declared defensively without knowing it's needed. **Before filling
the Play Console permissions declaration**: confirm whether LiveKit actually requires this at runtime
(check LiveKit's own docs/manifest, or test a call with a Bluetooth headset connected). If it's not
actually needed, remove it — an unused dangerous-adjacent permission with zero justifying code is
exactly the kind of thing Play's automated review flags.

### 5.3 `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION` declared together, only `FINE` requested at runtime

The app never calls `ActivityResultContracts.RequestPermission()` (or the multi-permission variant) with
`ACCESS_COARSE_LOCATION` on its own. Both permissions being declared together is standard/expected
Android practice — declaring `ACCESS_FINE_LOCATION` alone still lets Android 12+ show the user a system
dialog with a "Precise"/"Approximate" toggle, and the OS auto-grants `ACCESS_COARSE_LOCATION` alongside
whichever the user picks, **but only if both permissions are declared in the manifest** (which they are
here). So this pairing is correct engineering, not scope creep — but confirm this is what actually
happens on target OS versions before answering Play's "Does your app request one or more location
permissions" sub-questions, since the wording there specifically asks about precise vs. approximate.

### 5.4 `RECORD_AUDIO` is currently mis-wired for one code path

`AudioTranscriber.kt`'s own doc comment states that `ChatTranslationDelegate.transcribeAudio` "currently
(mis)uses this to 'transcribe' a received voice message, which actually records *your own* mic instead."
This is tracked in `docs/audio-transcription-todo.md`. It doesn't change the Data Safety declaration
(RECORD_AUDIO is still just RECORD_AUDIO, used locally), but it means the in-app UX may prompt for mic
access at a moment a reviewer or user wouldn't expect ("transcribe this message someone sent me" →
actually starts recording the current user). Worth fixing before submission so the permission's runtime
behavior matches its user-facing description.

### 5.5 Google Drive backup — the single biggest thing likely to be missed on the form

`BackupRepositoryImpl.kt` backs up **every message row** (`messageDao.getAllMessages()` — no filtering
by conversation, no filtering by E2EE status) to the signed-in Google account's Drive, via raw REST
calls with an OAuth token requested through `AccountManager.blockingGetAuthToken(account,
"oauth2:https://www.googleapis.com/auth/drive.file", true)`. Key points for the form:

- This is a **user-initiated, optional** feature (Backup screen), not automatic.
- Scope is `drive.file` (files created by the app only, not full Drive read access) — this is the
  narrower, less-sensitive Drive scope, which matters for Google's own OAuth verification requirements
  as well as the Data Safety form.
- The backed-up JSON includes message `content` fields as stored locally — for 1:1 messages that may be
  E2EE ciphertext (`isEncrypted` flag is preserved in the backup), for group messages it is plaintext.
  Either way, this is "Messages" (and potentially media URLs) leaving the app's own backend and landing
  in a **different Google product** the user controls — this needs its own line in the Data Safety form
  distinct from the Supabase collection, because Play treats "your own backend" and "a named third-party
  service" differently even when the destination is nominally "the user's own" storage.
- No dependency on `com.google.android.gms:play-services-drive` or the Drive Android client library
  exists — it's hand-rolled REST calls. Don't let a form-filler search the dependency list for "Drive"
  and conclude the feature doesn't exist.

### 5.6 Account deletion — RESOLVED (implemented since this audit, PRs #83/#84/#86)

The earlier version of this audit found no account-deletion feature at all (`AuthRepository` only
exposed `signOut()`/`signOutAll()`). This is now implemented and verified end-to-end against production:

- **App**: Profile → "Eliminar cuenta", with an explicit irreversible-action confirmation dialog
  (`ProfileScreen.kt`, `ProfileViewModel.kt`, `AuthRepository.deleteAccount()`).
- **Backend**: the `delete-account` Edge Function (`supabase/functions/delete-account/index.ts`),
  resolving the user strictly from their JWT. It: anonymizes the user's messages (content/attachments
  scrubbed, `is_deleted=true`, row kept so other participants' threads stay intact), deletes their
  `conversation_participants`/`invitations`/`blocked_users`/`user_status` rows, removes their Storage
  objects (avatar, status media, and their own message attachments), reassigns the handful of `NOT NULL`
  hard-FK columns that can't be null'd (`messages.sender_id`, `conversations.created_by`,
  `calls.caller_id`/`callee_id`, `call_signals.sender_id`) to a permanent "deleted user" placeholder
  account, then deletes the real `auth.users` row (cascades to `profiles`).
- Answer Play's "Does your app provide a way for users to request that their data be deleted?" **Yes**,
  in-app, self-service, immediate (not a support-ticket/manual process).
- This resolves what was flagged as a likely hard submission blocker — no longer applies.

---

## 6. Checklist — confirm/decide before filling the real Play Console form

- [x] **Account deletion**: implemented and verified end-to-end against production (§5.6). Answer Play's
      deletion question "Yes" — in-app, self-service.
- [ ] **BLUETOOTH_CONNECT**: confirm with LiveKit's docs/manifest whether it's actually required for
      Bluetooth audio routing during calls. Remove the permission if not, otherwise document the
      justification (§5.2).
- [ ] **Google Drive backup scope**: confirm `drive.file` is the intended, minimal scope, and decide
      whether the Data Safety form should describe backup as "sharing with Google Drive" or "user-
      directed transfer to a service the user controls" — get this right, it's a common review flank.
- [x] **Firebase Analytics/Crashlytics**: added since the last audit (PR #81) — declared above (§2, §3).
      `AD_ID` permission is not declared, so no Advertising ID is collected. If that ever changes, or if
      Analytics/Crashlytics start receiving anything beyond event/type metadata and crash traces,
      re-run this audit.
- [ ] **AI Assistant**: confirmed currently mocked (§2). The moment a real LLM API call replaces the
      `TODO` in `supabase/functions/ai-assistant/index.ts`, "Messages" must be declared as shared with
      that LLM provider, and this document regenerated.
- [ ] **Supabase data retention/deletion policy**: this audit only confirms what the *app* sends to
      Supabase. Confirm directly in the Supabase project (migrations under `supabase/migrations/`, RLS
      policies, and any scheduled cleanup jobs) what the actual retention period is for messages, media,
      session records, and matched-contact data, and whether "delete conversation" in the app performs a
      hard delete server-side or a soft delete/tombstone.
- [ ] **Media inside Google Drive backups**: confirm whether `imageUrl`/`videoUrl`/`audioUrl`/`fileUrl`
      fields in the backup JSON are just remote URLs (pointing back at Supabase Storage, which would
      still require the recipient — Google, in this case only as blob storage — to be able to resolve
      them) or ever embed raw bytes. Affects whether "Photos and videos"/"Audio files"/"Files and docs"
      need their own line under the Drive-sharing declaration, in addition to "Messages".
- [ ] **Contacts email-matching**: confirm this is described in the Play listing/permissions text as
      "used to suggest contacts who already use ChatApp," distinctly from the local-only phone number
      read (§5.1), since these have different risk profiles.
- [ ] **Call recording**: confirm whether the LiveKit call feature has any server-side or client-side
      recording capability enabled (checked `CallViewModel.kt` structurally but did not find a recording
      toggle — verify directly since call audio/video handling changes the Data Safety answers for
      "Audio files"/"Photos and videos" if recording exists).
- [ ] **Play Integrity enforcement**: confirm whether failing the integrity check (§ Device or other IDs)
      blocks app usage entirely or is soft-enforced — affects whether it's "Required" or "Optional" on
      the form.
- [ ] **LiveKit hosting**: confirm whether calls run through LiveKit Cloud (a named third party) or a
      self-hosted LiveKit server (the developer's own infrastructure, same treatment as Supabase) — check
      the real `LIVEKIT_URL` value, not just `local.properties.example`, since this changes whether call
      audio/video counts as "shared with a third party" on the form.
