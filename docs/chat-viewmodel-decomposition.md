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
- `context: ChatDelegateContext` — bundles the three hooks a delegate needs back into
  ChatViewModel: `getState: () -> ChatState`, `updateState: ((ChatState) -> ChatState) -> Unit`
  (a bound reference to `BaseViewModel.updateState`, so a delegate can only ever produce a
  *new* `ChatState` via the same `.copy()` pattern every intent handler already uses — never a
  raw mutable field), and `sendEffect: (ChatEffect) -> Unit`. ChatViewModel builds one
  `ChatDelegateContext` and passes the same instance to every delegate. (The first three
  delegates each took `getState`/`updateState`/`sendEffect` as separate constructor params;
  bundled into `ChatDelegateContext` once the 4th delegate's constructor tripped detekt's
  `LongParameterList` threshold — start any new delegate with `context` from day one rather
  than adding the three separately.)

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
| 3 | `ChatPollDelegate` | `observePoll`, `createPoll`, `votePoll` | `pollRepository`, `sendMessageUseCase` | **Done** |
| 4 | `ChatSchedulingDelegate` | `scheduleMessage`, `cancelScheduledMessage` | `scheduledMessageRepository`, `workManager`, `draftRepository` | **Done** |
| 5 | `ChatForwardDelegate` | `showForwardDialog`, `forwardMessage`, `showForwardSelectionDialog`, `forwardSelectedMessages` | `conversationRepository`, `messageRepository` | **Done** |
| 6 | `ChatContactCardDelegate` | `checkContactRelationship`, `contactCardPrimaryAction` | `userRepository`, `sendInvitationUseCase` | **Done** |
| 7 | `ChatSearchDelegate` | `searchMessages`, `jumpToMessage` | `messageRepository` | **Done** |
| 8a | `ChatMediaUploadDelegate` | `sendImages`, `sendFile`, `sendVideo` | `messageRepository`, `sendMessageUseCase`, `getUriMetadataUseCase`, `readUriAsBytesUseCase` | **Done** |
| 8b | `ChatQuickSendDelegate` | `sendGif`, `sendSticker`, `sendContact`, `handleContactSelected`, `fetchAndSendLocation`, `sendLocationMessage` | `application` (LocationManager), `sendMessageUseCase`, `contactRepository` | **Done** |
| 8c | `ChatAudioRecordingDelegate` | `startRecording`, `stopRecording`, `discardAudio`, `sendAudio`, + `cleanup()` for `onCleared()` | `application`, `messageRepository`, `sendMessageUseCase` | **Done** — originally planned as one "ChatMediaDelegate"; split into 8a/8b/8c once a single delegate covering all of it (9+ constructor params) tripped `LongParameterList` — the three really are distinct concerns (batch upload progress vs. one-shot sends vs. a `MediaRecorder` resource lifecycle) so the split reads better anyway |
| 9 | `ChatGroupPresenceDelegate` | the `if (isGroup) { ... }` block: membership-sync polling, live member-list + per-member online-status observation, reacting to join/leave | `groupRepository`, `getGroupMembersUseCase`, `userRepository` | **Done** |

All 11 of Phase 1's delegates (rows 1-9) are now done. `ChatViewModel.kt` went from
1194 lines/57 functions to ~690 lines/~25 functions across all of them, with zero changes to
`ChatState`/`ChatIntent`/`ChatScreen` throughout. Row 9 was the one genuinely different from
the rest: not triggered by a `ChatIntent` (its `start(uid)` is called once from `init`, only
for group conversations) and it couldn't take full ownership of everything it touches —
`onMembershipChanged` is a callback into ChatViewModel rather than a repository dependency,
because syncing/clearing local messages on join/leave has to update `_historyVisibleFrom` and
restart the remote-sync subscription that back the `messages` Flow ChatScreen collects, and
duplicating ownership of that into the delegate would've meant ChatScreen having two paths to
the same state. `groupMembers` is exposed as a read-only property for the same reason: the
`@mention` suggestion matcher in `onIntent`'s `InputChanged` handler (staying on
ChatViewModel per the "what stays" list below) still needs to read it.

One more param-budget lesson from 8c: when a delegate's own constructor would hit 8 params,
prefer folding two *narrow, related* callbacks into one (e.g. "cancel the pending draft-save
job, then persist the cleared draft" became a single `clearDraft: suspend () -> Unit` instead
of separate `draftRepository`/`cancelDraftSave` params) over reaching for a second bundling
class — only introduce another `*Context`-style bundle if the delegate genuinely needs 3+ of
these hooks, the way `ChatDelegateContext` bundles state/effect access.

What stays directly on `ChatViewModel`: `onIntent` dispatch, `init` wiring (conversation/user
loading, the various `observe*`/state-projector subscriptions, and calling
`groupPresenceDelegate.start(uid)` when the conversation is a group), core
send/edit/delete/reaction handlers (`sendMessage`, `confirmEdit`, `deleteMessage`,
`toggleReaction`, `setExpiry`, `toggleMessageSelection`, `deleteSelectedMessages`),
mute/leave-group, incognito, chat theme, wallpaper, typing presence
(`startTypingPresence`/`sendTypingPresence`), `@mention` selection/matching, draft saving,
`startRemoteSync`/`_historyVisibleFrom` (backing the `messages` Flow). That's still a real
ViewModel, but one whose job is "core conversation messaging" instead of "everything the chat
screen can possibly do."

## Phase 2 (in progress): split ChatScreen, shrink State/Intent

### Splitting `ChatScreen.kt`'s composables into per-feature files

Lowest-risk first, per the original plan: the self-contained dialogs/sheets that already take
only their own narrow params, no dependency on `ChatViewModel` or the message-bubble
rendering tree.

**Done (first pass):** `ChatDialogs.kt` (`ExpiryDurationDialog`, `MuteDurationDialog`,
`ForwardConversationDialog`, `ScheduleMessageDialog`, `ImageViewerDialog`),
`ChatBottomSheets.kt` (`ChatThemePickerSheet`, `DisappearingModeSheet`, `AiAssistantSheet`,
`CreatePollSheetContent`, `ReactionDetailsSheet`, `WallpaperPickerSheet`),
`PinnedMessageBanner.kt`. `ChatScreen.kt`: 3818 → 3051 lines.

Two things this pass had to get right that a naive copy-paste doesn't:

- **Visibility**: these composables were `private` (file-scoped in Kotlin — a top-level
  `private` symbol is invisible outside its own file, even within the same package), but
  `ChatScreen`'s main composable still calls all of them. Moving the declaration to a new file
  requires widening to `internal` (visible module-wide), not keeping `private`, or the call
  sites in `ChatScreen.kt` stop compiling.
- **No FQN mid-chain**: some of the original code used fully-qualified names as a *standalone*
  reference (`androidx.compose.foundation.layout.Column { ... }`, valid) or to qualify a
  receiver at the *start* of a chain (`androidx.compose.ui.Modifier.height(8.dp)`, valid — but
  note the `height` extension function still needs importing even though `Modifier` itself is
  FQN'd, since Kotlin resolves extension-function names purely via import/scope, independent
  of how the receiver was written). What doesn't work — and doesn't even parse — is injecting
  a package path *mid-chain* after a receiver expression, e.g.
  `Modifier.size(52.dp).androidx.compose.foundation.background(...)`. First draft of this
  extraction introduced exactly that (an attempt to "FQN everything defensively" instead of
  adding proper imports); fixed before commit by using bare names + real imports throughout,
  which is what the compiler catches immediately if it recurs.
- Also: `import androidx.compose.foundation.layout.weight` resolves to the wrong (internal)
  symbol — `Modifier.weight()` is a member extension on `RowScope`/`ColumnScope`, resolved
  automatically for any `.weight(...)` call inside a `Row { }`/`Column { }` lambda with **no
  import at all**; explicitly importing `weight` breaks it. Same lesson as `LongParameterList`
  in Phase 1: verify against a real compile, don't assume a plausible-looking import is right.

**Left for a later pass, higher risk, in the order noted originally:** the message-bubble
rendering tree (`MessageBubble` and everything it dispatches to — `ChatBubbleSlot`,
`CallMessageBubble`, `FileBubble`, `PdfFileCard`, `GenericFileBubble`, `VideoBubble`,
`StickerBubble`, `DeletedMessageBubble`, `MessageFooterContent`, `LocationMessageCard`,
`MediaMetaOverlay`, `PendingImageBatchBubble`, `ImageGroupBubble`, `ReplyQuote`,
`LinkPreviewCard`, `ContactHeader`, `ContactBubble`, `PollBubble`, `PollOptionRow`,
`ReplySelectContainer`) and the top-level `ChatScreen`/`ChatScreenContent` scaffold itself.
`PollBubble`/`PollOptionRow` stayed in `ChatScreen.kt` in this pass specifically because they
depend on `ChatBubbleSlot`, which lives in that higher-risk tree.

### Shrinking `ChatState`/`ChatIntent`

Not started. Only once the composable split above is further along does it make sense to
group `ChatState`'s ~65 fields into nested state objects owned by each delegate (e.g.
`state.ai: ChatAiState`) — every `state.xxx` read for that field across `ChatScreen.kt`'s
(now multiple) files needs updating in the same step, so this should happen file-by-file
alongside a composable's own move, not as one big rename. Splitting `ChatIntent` into
per-concern sealed hierarchies, if the 78-variant `when` in `onIntent` becomes the
bottleneck, is likely lower priority than the state split.

## Non-goals

- No sub-`ViewModel`s (Koin's `viewModelOf`/`viewModel` DI wiring, `koinViewModel()` call
  sites in `ChatScreen`, and `SavedStateHandle`/lifecycle semantics all assume one
  `ChatViewModel` per chat screen — introducing more `ViewModel`s would need its own DI and
  lifecycle design, not just a code move).
- No behavior changes bundled into a decomposition commit — each delegate extraction should
  be a pure move (verified by re-running `ChatViewModelTest` unchanged) so a regression is
  easy to bisect to "the code moved wrong," not "the code moved and something else changed."
