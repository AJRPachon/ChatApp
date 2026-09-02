# Audio message transcription — known gap, not yet implemented

## What's broken today

`ChatIntent.TranscribeAudio(messageId)` → `ChatTranslationDelegate.transcribeAudio()` →
`AudioTranscriber.transcribeFromMic()`. That last call opens the device's **live microphone**
via Android's `SpeechRecognizer` and transcribes whatever the *current user* says out loud — it
never touches the audio file the message actually contains. Tapping "Transcribir" on a voice
note someone else sent would silently start listening to your own mic instead of transcribing
what they said. No UI currently calls this intent (see
[chat-viewmodel-decomposition.md](chat-viewmodel-decomposition.md)'s slice 2 finding), so the
bug hasn't shipped visibly yet — but the underlying wiring is wrong, not just unfinished.

## Why this isn't a quick fix

Android's `SpeechRecognizer` (the only on-device speech API this app uses) **only accepts live
microphone input** — there is no public API to hand it an existing audio file. Real
"transcribe this voice message" needs a cloud speech-to-text call instead, following the same
pattern as `ai-assistant`'s Edge Function (`AiAssistantRepository` →
`supabaseClient.functions.invoke(...)` → Deno function → external API, key held server-side).

That pattern hits a second snag specific to this app: voice messages are recorded as **AAC
inside an MP4 container** (`.m4a` — see `ChatAudioRecordingDelegate.kt`'s
`MediaRecorder.OutputFormat.MPEG_4` / `AudioEncoder.AAC`). **Google Cloud Speech-to-Text's
synchronous `recognize` REST API doesn't accept that container/codec** — its supported
`AudioEncoding` values are `LINEAR16`, `FLAC`, `MULAW`, `AMR`, `AMR_WB`, `OGG_OPUS`,
`SPEEX_WITH_HEADER_BYTE`, `MP3`, none of which is AAC-in-MP4. A Supabase Edge Function (Deno,
sandboxed) can't shell out to `ffmpeg` to transcode first — no native process access.

## Recommended path for a future session

**Switch the transcription provider to OpenAI's Whisper transcription API** — it accepts
`m4a`/AAC directly (along with mp3/mp4/mpeg/mpga/wav/webm), so no transcoding step is needed.
Concretely:

1. New Edge Function `transcribe-audio` (same shape as `ai-assistant`): receives
   `{ audioUrl: string }`, fetches the file, forwards it to
   `POST https://api.openai.com/v1/audio/transcriptions` with an `OPENAI_API_KEY` Supabase
   secret (never shipped in the APK), returns `{ text: string }`.
2. New `AudioTranscriptionRepository` (Kotlin, mirrors `AiAssistantRepository`) calling that
   function via `supabaseClient.functions.invoke(...)`.
3. `ChatTranslationDelegate.transcribeAudio(messageId: String)` needs the message's `audioUrl`
   passed in (not just the id) — update `ChatIntent.TranscribeAudio` and its one dispatch site
   accordingly — and calls the new repository instead of `AudioTranscriber.transcribeFromMic()`.
4. `AudioTranscriber`/`transcribeFromMic()` itself is a legitimate, separate feature (live
   dictation into the message input, similar to what many chat apps offer) — worth keeping, just
   under an intent/button name that's honest about recording your own mic, not confused with
   transcribing a received voice note.
5. UI: a "Transcribir" action + result display on `RemoteAudioPlayer`/audio message bubbles,
   same pattern as the `state.translation.translatedTexts` UI added alongside this doc.

**Alternative considered and rejected for now:** keep Google Cloud Speech-to-Text and add a
transcoding step (e.g. a separate Cloud Run service running `ffmpeg`, called from the Edge
Function before the STT call). Technically possible but a whole extra infrastructure component
to deploy and keep running versus Whisper's zero-transcoding path — not worth it unless there's
a reason to prefer Google's STT specifically.

Needs an `OPENAI_API_KEY` (or equivalent) before this can be built — not yet provided.
