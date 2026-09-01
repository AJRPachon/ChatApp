# ChatViewModel / ChatScreen decomposition plan

## Why

`ChatViewModel.kt` (1194 lines, 57 functions) and `ChatScreen.kt` (3818 lines, 39
`@Composable`s) are the largest files in the app by a wide margin. `ChatContract.kt` backs
them with one flat `ChatState` (~65 fields) and one flat `ChatIntent` (78 variants) covering
every feature the chat screen has ever grown: core messaging, media/attachments, calls,
polls, AI assistant, scheduling, translation, transcription, disappearing messages,
mentions, incognito, theming, wallpaper, group presence, contact-card lookups, link
previews. `@Suppress("LongParameterList", "TooManyFunctions")` on the class is the detekt
signal; the real cost is that touching any one feature means reading and reasoning about
all the others, and testing one concern in isolation isn't possible.

This was flagged by `chatapp-architecture-reviewer` as **not a one-pass fix** — unlike the
[MessageRepository interface split](../app/src/main/java/com/ajrpachon/chatapp/domain/repository/MessageRepository.kt)
(mechanical: group 36 methods by actual per-consumer usage), there's no single correct shape
here without touching the UI layer too. This document lays out the target shape and a safe
migration order so the work can proceed incrementally, across sessions, without a single
high-risk rewrite of the app's busiest screen.

## Target shape: delegate-per-concern, State/Intent unchanged (for now)

**Phase 1 (this document's main scope): extract ChatViewModel's *logic* into per-concern
delegate classes, without touching `ChatState`/`ChatIntent`/`ChatScreen` at all.**

Each delegate is a plain class (not a `ViewModel`, not a `BaseViewModel` subclass — it has no
UI lifecycle of its own) that receives, via constructor:

- the repositories/use-cases it actually needs (a subset of ChatViewModel's ~30 constructor
  params — most delegates need 2-4),
- `conversationId: String` and a way to read the current user id,
- a `scope: CoroutineScope` (ChatViewModel passes its own `viewModelScope`, so delegate
  coroutines are cancelled exactly when the ViewModel is cleared — no separate lifecycle to
  manage),
- `updateState: ((ChatState) -> ChatState) -> Unit` — a bound reference to
  `BaseViewModel.updateState` (`::updateState`, accessible since it's called from inside
  ChatViewModel), so a delegate can only ever produce a *new* `ChatState` via the same
  `.copy()` pattern every intent handler already uses. It never touches a raw mutable
  field.
- optionally `sendEffect: suspend (ChatEffect) -> Unit` if the concern emits effects.

`ChatViewModel.onIntent()` keeps its single `when` dispatcher (that's the MVI contract and
shouldn't move), but each branch forwards to a delegate method instead of a private function
on the ViewModel itself. `ChatState`/`ChatIntent` stay exactly as they are, so **ChatScreen.kt
needs zero changes for Phase 1** — this is what keeps each extraction low-risk and
independently revertable.

Reference implementation: [`ChatAiDelegate.kt`](../app/src/main/java/com/ajrpachon/chatapp/ui/chat/ChatAiDelegate.kt),
extracted first because it's the most self-contained concern (2 repository deps, no shared
mutable fields like `recorder`/`draftSaveJob`/job-cancellation state that other concerns
touch).

## Proposed delegates and migration order

Ordered by how self-contained each concern is (fewest shared mutable fields / job handles
first — those are the ones most likely to introduce a subtle behavior change if extracted
carelessly):

| # | Delegate | ChatViewModel functions it replaces | Deps | Status |
|---|----------|--------------------------------------|------|--------|
| 1 | `ChatAiDelegate` | `aiSummarize`, `aiSuggestReply`, `aiFreeform` | `messageRepository`, `aiAssistantRepository` | **Done** |
| 2 | `ChatTranslationDelegate` | `translateMessage`, `transcribeAudio` | `translationManager`, `audioTranscriber` | **Done** |
| 3 | `ChatPollDelegate` | `observePoll`, `createPoll`, `votePoll` | `pollRepository`, `sendMessageUseCase` | Planned |
| 4 | `ChatSchedulingDelegate` | `scheduleMessage`, `cancelScheduledMessage` | `scheduledMessageRepository`, `workManager`, `draftRepository` | Planned |
| 5 | `ChatForwardDelegate` | `showForwardDialog`, `forwardMessage`, `showForwardSelectionDialog`, `forwardSelectedMessages` | `conversationRepository`, `messageRepository` | Planned |
| 6 | `ChatContactCardDelegate` | `checkContactRelationship`, `contactCardPrimaryAction` | `userRepository`, `sendInvitationUseCase` | Planned |
| 7 | `ChatSearchDelegate` | `searchMessages`, `jumpToMessage` | `messageRepository` | Planned |
| 8 | `ChatMediaDelegate` | `sendImages`, `sendFile`, `sendVideo`, `startRecording`, `stopRecording`, `discardAudio`, `sendAudio`, `sendGif`, `sendSticker`, `sendContact`, `handleContactSelected`, `fetchAndSendLocation`, `sendLocationMessage` | `messageRepository`, `sendMessageUseCase`, `getUriMetadataUseCase`, `readUriAsBytesUseCase`, `contactRepository` | Planned — largest slice, owns the `recorder`/`recordingTimerJob` fields, do last among the "planned" group |
| 9 | init-block group-presence/membership logic (lines ~174-273) | not a delegate — this is `init` wiring with heavy cross-references to other state (`groupMembers`, `memberOnlineStatuses`, `_historyVisibleFrom`) | most repositories | **Not planned yet** — highest risk, needs its own design pass |

What stays directly on `ChatViewModel` after all of the above: `onIntent` dispatch, `init`
wiring (conversation/user/group loading — see row 9), core send/edit/delete/reaction
handlers (`sendMessage`, `confirmEdit`, `deleteMessage`, `toggleReaction`, `setExpiry`,
`toggleMessageSelection`, `deleteSelectedMessages`), mute/leave-group, incognito, chat theme,
wallpaper, typing presence (`startTypingPresence`/`sendTypingPresence`), mention selection,
draft saving. That's still a real ViewModel, but one whose job is "core conversation
messaging" instead of "everything the chat screen can possibly do."

## Phase 2 (later, not started): shrink State/Intent, split ChatScreen

Only once the delegates in the table above are stable does it make sense to:

- Group `ChatState`'s ~65 fields into nested state objects owned by each delegate (e.g.
  `state.ai: ChatAiState`) — this is what actually reduces the "one giant flat state" cost,
  but it means every `state.xxx` read in `ChatScreen.kt` for that field needs updating, so it
  must happen file-by-file in step with a delegate's own extraction, not as one big rename.
- Split `ChatIntent` into per-concern sealed hierarchies if the 78-variant `when` in
  `onIntent` becomes the bottleneck — likely lower priority than the state split.
- Split `ChatScreen.kt`'s 39 composables into per-feature files. Lowest-risk first: the
  self-contained dialogs/sheets that already take their own narrow state slice as params
  (forward dialog, schedule dialog, AI sheet, create-poll sheet, theme picker, wallpaper
  picker, disappearing-mode sheet) — each can move to its own file with no behavior change,
  same pattern as this session's earlier `LocationMessageFormat`/`CallControlButton`
  extractions. The message-bubble rendering tree (already partially decomposed this session)
  and the top-level scaffold are higher-risk, do last.

## Non-goals

- No sub-`ViewModel`s (Koin's `viewModelOf`/`viewModel` DI wiring, `koinViewModel()` call
  sites in `ChatScreen`, and `SavedStateHandle`/lifecycle semantics all assume one
  `ChatViewModel` per chat screen — introducing more `ViewModel`s would need its own DI and
  lifecycle design, not just a code move).
- No behavior changes bundled into a decomposition commit — each delegate extraction should
  be a pure move (verified by re-running `ChatViewModelTest` unchanged) so a regression is
  easy to bisect to "the code moved wrong," not "the code moved and something else changed."
