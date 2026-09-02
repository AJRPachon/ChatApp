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

**Done (second pass — the message-bubble rendering tree):** split into 6 files by cohesion
rather than one, since the 22 composables involved are heavily interdependent:

- `MessageBubble.kt` — `pollIdOf`/`contactPhoneOf`, `ReplySelectContainer`, `ChatBubbleSlot`
  (the two building blocks nearly everything else renders through), `MessageBubble` itself.
- `ChatFileBubbles.kt` — `CallMessageBubble`, `FileBubble`, `PdfFileCard`, `GenericFileBubble`,
  `formatFileSize`, `VideoBubble`, `StickerBubble`, `DeletedMessageBubble`.
- `ChatBubbleContent.kt` — `MessageFooterContent`, `LocationMessageCard`, `ReadReceiptIcon`,
  `SendStatusIcon`, `MediaMetaOverlay`, `ReplyQuote`, `LinkPreviewCard`.
- `ChatImageGroupBubbles.kt` — `PendingImageBatchBubble`, `ImageGroupBubble`.
- `ContactBubble.kt` — `ContactHeader` (kept `private`, only used within this file),
  `ContactBubble`.
- `PollBubble.kt` — `PollBubble`, `PollOptionRow` (kept `private`, ditto).

`ChatScreen.kt`: 3051 → 1317 lines (1194/57 → this file alone, across the whole plan). Unlike
the first pass, most of these needed `internal` uniformly — the previous pass's instinct to
minimize visibility widening by keeping call graphs within one file doesn't hold here, since
`MessageBubble`, `PendingImageBatchBubble`, `ImageGroupBubble`, and `pollIdOf`/`contactPhoneOf`
are called directly from `ChatScreen`'s `LazyColumn`, and `ChatBubbleSlot` is called from
nearly every bubble type across every new file — so the pattern from pass one (mark it
`internal`, don't try to cleverly co-locate to keep `private`) generalizes better than
over-optimizing per-file visibility.

New failure mode this pass, worth watching for on any future large deletion: **splicing a big
line range by hand with an off-by-N line-number error silently deletes a legitimate
neighboring line** (here, the outer composable's own closing brace, one screenful above where
the actual bubble-tree section started) instead of just the intended range. A raw open-vs-close
brace count over the whole file (`content.count('{')` vs `content.count('}')`) caught the
one-brace imbalance immediately, before it reached a compile — cheap enough to run after any
line-range deletion this size, not just when something looks wrong.

Also found and fixed in passing (real, not baseline noise): a genuinely dead
`val context = LocalContext.current` inside `MessageBubble` — declared, never read, evidently
never caught by detekt before this move surfaced it standalone. Removed along with its
now-unused import.

**Done (third pass — the dialog/sheet host):** `ChatScreen`'s own body, after the previous two
passes, was still one ~1150-line composable: setup (state collection, effects, permission
launchers), a flat sequence of ~16 independent `if (state.showX) { XDialog(...) }` blocks, the
`Scaffold` (`topBar` with its own large dropdown-menu block, `bottomBar`, message-list
content). Of these, the dialog/sheet sequence was the one genuinely self-contained region —
extracted to `ChatDialogHost.kt` as a single composable. `ChatScreen.kt`: 1317 → 1069 lines.

Found and fixed in passing (real bug, not a refactor artifact): `ForwardConversationDialog` for
`state.showForwardSelectionDialog` was duplicated verbatim as two consecutive identical `if`
blocks, so it composed twice (stacked) whenever that flag was true. Collapsed to one during the
move.

Five pieces of local UI state are read *and* written from both inside the extracted host and
from `ChatScreen`'s own message-list content further down (opening the image viewer or the
reaction-details sheet from a tapped bubble): `showDeleteSelectionConfirm`, `showViewer`,
`viewerUrls`, `viewerInitialIndex`, `reactionDetailMessageId`. Rather than threading get/set
callback pairs for each (10 extra params) they're passed as the raw `MutableState<T>` objects
`remember`/`rememberSaveable` already produce — changing their declarations from `var x by
remember { mutableStateOf(v) }` to `val xState = remember { mutableStateOf(v) }` and every
`x = y` call site (in both the moved and the staying code) to `xState.value = y`. Idiomatic
Compose state-hoisting; keeps both sides of the split reading/writing the exact same state
identity instead of two independent copies.

**Done (fourth pass — `topBar`/`bottomBar`/message-list, the rest of the `Scaffold`):** the
three regions left after the dialog-host pass, each extracted to its own file:

- `ChatTopBar.kt` — incognito banner, the `TopAppBar` (multi-select count vs. avatar/name/
  presence title, back/clear-selection navigation, the scheduled-badge/call/overflow dropdown
  menus), `PinnedMessageBanner`. Cleanest boundary of the three: its internal
  `callMenuExpanded`/`menuExpanded` locals are self-contained within the block itself, so it
  only needed `state`, `vm`, `latestPinned`, `pinnedBannerVisible`, the already-hoisted
  `showDeleteSelectionConfirm` `MutableState` (reused from the dialog-host pass — this bar's
  delete action opens that same confirmation dialog), and 5 pass-through navigation callbacks.
- `ChatBottomBar.kt` — offline banner, editing/reply preview bars, typing indicator, the
  audio-state recording/preview/normal-input switch. The one genuinely entangled with the
  permission-launcher machinery: 10 `rememberLauncherForActivityResult` launchers plus
  `cameraUri`/`videoUri` are all declared once at the top of `ChatScreen` and read from
  `NormalInputBar`'s attachment-picker callbacks. Rather than threading typed
  `ActivityResultLauncher<I>` objects across the file boundary (10 different contracts:
  `PickVisualMediaRequest`, `Array<String>`, `Uri`, `String`×5, `Void?` — exact generic
  matching for no benefit), `ChatScreen` keeps building the finished, permission-check-then-
  launch lambdas (`onGallery`/`onCamera`/`onMic`/`onAttachFile`/`onAttachVideo`/`onLocation`/
  `onContact`) exactly as before, and passes those 7 closed-over `() -> Unit` callbacks in. This
  also means `cameraUri`/`videoUri` never needed `MutableState`-hoisting — they stay `var x by
  rememberSaveable` in `ChatScreen`, since only `ChatScreen`-local code ever touches them.
- `ChatMessageList.kt` — the `Scaffold` content lambda: `ChatMessagesSkeleton` during initial
  load, the `LazyColumn` (pending-image-batch placeholder, the image-group-vs-single-bubble
  dispatch with its consecutive-image lookahead, paging-load-more indicator), the
  scroll-to-bottom FAB, `MessageSearchOverlay`. Reuses all four `MutableState`s already hoisted
  for `ChatDialogHost` (`viewerUrls`, `viewerInitialIndex`, `showViewer`,
  `reactionDetailMessageId`) since bubbles here are what originally populate them.

Found and fixed in passing (real bug, not a refactor artifact): `bottomBar`'s `Surface` block
rendered `OfflineBanner()` **twice** whenever offline — once via the outer
`AnimatedVisibility(!state.isOnline) { OfflineBanner() }`, and again via a redundant
`if (!state.isOnline) { OfflineBanner() }` a few lines further into the same `Column`. Kept only
the outer `AnimatedVisibility` version in `ChatBottomBar.kt`.

Left as-is, not "fixed": `pinnedBannerVisible` (`var ... by rememberSaveable(latestPinned?.id) {
mutableStateOf(true) }`) is read once (`if (latestPinned != null && pinnedBannerVisible)`) but
never reassigned anywhere in the file — grep confirms only the declaration and that one read.
Ambiguous whether this is dead code or a half-wired feature (there's no explicit
dismiss-without-unpin action), so passed through to `ChatTopBar` as a plain `Boolean` unchanged
rather than making a judgment call about it inside a pure-move refactor.

`ChatScreen.kt`: 1069 → 404 lines (1194/57 functions → this file alone, across the whole plan).
What remains is genuinely just setup: state/effect collection, the permission-launcher
declarations `ChatBottomBar`'s callbacks close over, the `LaunchedEffect`s driving
scroll/error/effect handling, and the `Scaffold` wiring the three new composables (plus
`ChatDialogHost`) together.

### Shrinking `ChatState`/`ChatIntent`

**Done.** The composable split above meant `state.xxx` reads were spread across
`ChatScreen.kt` + `ChatTopBar.kt`/`ChatBottomBar.kt`/`ChatMessageList.kt`/
`ChatDialogHost.kt`/`ChatDialogs.kt`/`ChatBottomSheets.kt`/the message-bubble files rather
than one file — but each nested-state group below only touched 3-6 files in practice (a
delegate, the one or two UI files that read that concern's fields, `ChatViewModel.kt`'s
`onIntent` branches), so grouping one concern at a time stayed a small, independently
verifiable slice throughout. Same discipline as Phase 1's delegates: one slice = one commit,
verified by the full compile→detekt→test cycle, `ChatViewModelTest` updated only on the two
slices (3 and 5) whose tests asserted on a moved field directly. See "Shrinking
ChatState/ChatIntent: done" below the target-shape table for the closing summary.

**Target shape:** each group becomes a nested `data class` with its own `= DefaultValue()`,
referenced as `state.<name>` instead of `state.<individual field>`. Grouped by the delegate
that owns the fields where one exists (traceable 1:1 back to the Phase 1 table above); fields
with no owning delegate (handled directly in `ChatViewModel.onIntent`, or pure conversation
identity set once in `init`) are grouped by UI feature instead.

| # | Nested state | Fields | Owning delegate | Status |
|---|--------------|--------|------------------|--------|
| 1 | `state.ai: ChatAiUiState` | `showAiSheet`→`showSheet`, `aiSuggestion`→`suggestion`, `isAiLoading`→`isLoading` | `ChatAiDelegate` | **Done** |
| 2 | `state.translation: ChatTranslationUiState` | `translatedTexts`, `translatingMessageIds`, `audioTranscriptions`→`transcriptions` | `ChatTranslationDelegate` | **Done** |
| 3 | `state.poll: ChatPollFeatureState` | `showCreatePollSheet`→`showCreateSheet`, `pollUiStates`→`uiStates` (renamed to avoid colliding with the existing per-poll `PollUiState` data class) | `ChatPollDelegate` | **Done** |
| 4 | `state.scheduling: ChatSchedulingUiState` | `showScheduleDialog`→`showDialog`, `scheduledAtMs`, `scheduledMessageCount`→`messageCount`, `showScheduledSheet`→`showSheet`, `scheduledMessages`→`messages` | `ChatSchedulingDelegate` | **Done** |
| 5 | `state.forward: ChatForwardUiState` | `showForwardDialog`→`showDialog`, `forwardingMessage`→`message`, `forwardableConversations`→`conversations`, `showForwardSelectionDialog`→`showSelectionDialog` | `ChatForwardDelegate` | **Done** |
| 6 | `state.contactCard: ChatContactCardUiState` | `contactPhoneLookups`→`lookups` | `ChatContactCardDelegate` | **Done** |
| 7 | `state.search: ChatSearchUiState` | `isSearchActive`→`isActive`, `searchQuery`→`query`, `searchResults`→`results`, `isSearching`, `highlightedMessageId` | `ChatSearchDelegate` | **Done** |
| 8 | `state.mediaUpload: ChatMediaUploadUiState` | `isUploadingFile`, `mediaUploadProgress`→`progress`, `pendingImageUris`, `suppressedImageMessageIds` | `ChatMediaUploadDelegate` | **Done** |
| 9 | `state.audioState: AudioState` | (unchanged — already grouped since before Phase 1) | `ChatAudioRecordingDelegate` | Already done |
| 10 | `state.groupPresence: ChatGroupPresenceUiState` | `onlineMemberCount`, `groupMemberCount`→`memberCount` (**not** `isOnline` — that's `NetworkMonitor`, a different concern despite the similar name; **not** `isGroup`/`isCurrentUserMember`/`groupAvatarUrl` — conversation identity, set once from `conversationRepository` in `init`, not by this delegate) | `ChatGroupPresenceDelegate` | **Done** |
| 11 | `state.mute: ChatMuteUiState` | `isMuted`, `mutedUntil`, `showMuteDialog`→`showDialog` | none (`toggleMute`/`muteFor` on `ChatViewModel`) | **Done** |
| 12 | `state.theme: ChatThemeUiState` | `chatTheme`→`theme`, `showThemePicker`→`showPicker` | none | **Done** |
| 13 | `state.disappearing: ChatDisappearingUiState` | `disappearingModeSeconds`→`seconds`, `showDisappearingModeSheet`→`showSheet` | none | **Done** |
| 14 | `state.mention: ChatMentionUiState` | `mentionSuggestions`→`suggestions`, `showMentionSuggestions`→`showSuggestions` | none | **Done** |
| 15 | `state.incognito: ChatIncognitoUiState` | `isIncognito`, `showIncognitoInfoDialog`→`showInfoDialog` | none | **Done** |
| 16 | `state.wallpaper: ChatWallpaperUiState` | `wallpaperColor`→`color`, `showWallpaperPicker`→`showPicker` | none | **Done** |

Left flat on `ChatState` (no grouping planned): `inputText`, `isSending`, `currentUserId`,
`conversationTitle`, `error`, `replyingTo`, `editingMessage`, `selectedMessageIds`,
`expiryDialogMessageId`, `showStickerPicker`, `typingUserNames`, `pinnedMessages`,
`isExporting`, `linkPreviews` (core messaging — the ViewModel's own remaining concern per the
Phase 1 "what stays" list) and `otherUserId`/`otherUserAvatarUrl`/`isOtherUserOnline`/
`otherUserLastSeenMs`/`groupAvatarUrl`/`isGroup`/`isCurrentUserMember`/`isOnline`
(conversation identity/presence — touched by the `presenceText`/`subtitleText` computed
properties and both `ChatTopBar`/`ChatBottomBar`; grouping this one is riskier than the
delegate-backed rows since there's no delegate boundary to anchor it to, so it's deferred
rather than guessed at up front).

**Slice 1 (`ChatAiUiState`) — what the pattern looks like in practice:** `ChatContract.kt`
gains a `data class ChatAiUiState(val showSheet: Boolean = false, val suggestion: String? =
null, val isLoading: Boolean = false)`; `ChatState`'s three flat fields become `val ai:
ChatAiUiState = ChatAiUiState()`. Every `it.copy(showAiSheet = x, aiSuggestion = y)` becomes
`it.copy(ai = it.ai.copy(showSheet = x, suggestion = y))` — one extra `.copy()` nesting level,
mechanical at each call site. `ChatAiDelegate.kt` (3 fields, 9 call sites), `ChatViewModel.kt`
(`onIntent`'s `OpenAiSheet`/`DismissAiSheet`/`InsertAiSuggestion` branches), `ChatBottomSheets.kt`
(`AiAssistantSheet`'s params — unchanged signature, since it already took the 3 fields as
separate params, not `ChatState` itself) and `ChatDialogHost.kt` (the call site building those
params from `state.showAiSheet`/`state.aiSuggestion`/`state.isAiLoading` → `state.ai.showSheet`/
`state.ai.suggestion`/`state.ai.isLoading`) were the only 4 files touched — confirms the
per-slice blast radius stays small even after the composable split spread `ChatState` reads
across many files.

**Slice 2 (`ChatTranslationUiState`) — even smaller than slice 1:** `translatedTexts`/
`translatingMessageIds`/`audioTranscriptions` (renamed `transcriptions` once nested — the
`audio` prefix was only disambiguating it from `translatedTexts` at the top level) turned out
to have **no UI consumer at all** — grep across `ui/chat` found reads only in
`ChatTranslationDelegate.kt` (the delegate that produces them) and `ChatViewModel.kt`'s
`DismissTranslation` branch. `TranslateMessage`/`TranscribeAudio` intents can be sent and the
delegate populates the state, but no bubble/dialog currently displays a translated string or a
transcription — the feature is wired ViewModel-side but not surfaced in the UI yet. Not this
slice's job to fix (pure move only); noting it here since it's a real gap, not a refactor
artifact. Only 3 files touched: `ChatContract.kt`, `ChatTranslationDelegate.kt` (4 call sites),
`ChatViewModel.kt` (1 call site).

**Slice 3 (`ChatPollFeatureState`) — first slice that also touched a test file:**
`showCreatePollSheet`/`pollUiStates` → `state.poll.showCreateSheet`/`state.poll.uiStates`.
Renamed `pollUiStates` to `uiStates` once nested (kept the pre-existing per-poll `PollUiState`
data class itself untouched — different type, still keyed by pollId inside the map, just a
name collision at the top level that nesting resolves). 5 files: `ChatContract.kt`,
`ChatPollDelegate.kt` (7 call sites across `observePoll`'s three flows plus `createPoll`),
`ChatViewModel.kt` (`OpenCreatePollSheet`/`DismissCreatePollSheet`), `ChatDialogHost.kt` (the
create-sheet visibility check), `ChatMessageList.kt` (two identical-looking but
differently-indented `state.pollUiStates[id]` reads feeding `MessageBubble`'s own
`pollUiStates` param — a plain-text edit on the first one silently missed the second because
the surrounding indentation differed by 4 spaces between the `group.size > 2` image-group
branch and the plain-bubble branch; caught immediately by re-grepping for the old field name
after the edit, which is now the standard verify-before-compile step for any slice touching
more than one call site). `ChatViewModelTest.kt`'s
`ObservePoll populates pollUiStates from pollRepository flows` test reads
`vm.state.value.pollUiStates["poll1"]` directly — the first slice where the state shape leaked
into a test assertion, updated to `vm.state.value.poll.uiStates["poll1"]` alongside the
production code in the same commit.

**Slice 4 (`ChatSchedulingUiState`) — another write-only field found:** `showScheduleDialog`/
`scheduledAtMs`/`scheduledMessageCount`/`showScheduledSheet`/`scheduledMessages` →
`state.scheduling.{showDialog,scheduledAtMs,messageCount,showSheet,messages}`. While tracing
every call site, `scheduledAtMs` turned out to be set by
`ChatSchedulingDelegate.scheduleMessage` but never read back anywhere in the app — same shape
as `pinnedBannerVisible` from the topBar/bottomBar/message-list pass: a real, pre-existing
leftover, not something this slice introduced or should silently start reading/using. Left the
field in place (documented with a code comment on `ChatSchedulingUiState` itself this time,
so the next person tracing it doesn't have to re-derive it via grep). 5 files touched:
`ChatContract.kt`, `ChatSchedulingDelegate.kt` (2 call sites), `ChatViewModel.kt` (the `init`
block's `scheduledMessageRepository.observeAll()` collector plus 4 `onIntent` branches),
`ChatDialogHost.kt` (4 reads across the schedule dialog and scheduled-messages sheet),
`ChatTopBar.kt` (the scheduled-count badge, 2 reads).

**Slice 5 (`ChatForwardUiState`) — two dialogs sharing one field:** `showForwardDialog`/
`forwardingMessage`/`forwardableConversations`/`showForwardSelectionDialog` →
`state.forward.{showDialog,message,conversations,showSelectionDialog}`. The single-message
forward dialog and the multi-select forward-selection dialog are mutually exclusive (only one
shown at a time) but share the same conversations list, so `conversations` stays a single field
on the nested state rather than being duplicated per-dialog — documented directly on
`ChatForwardUiState` this time rather than left implicit. 5 files: `ChatContract.kt`,
`ChatForwardDelegate.kt` (6 call sites across all 4 functions), `ChatViewModel.kt`
(`DismissForwardDialog`/`DismissForwardSelectionDialog`), `ChatDialogHost.kt` (both
`ForwardConversationDialog` call sites — including the one with the `Was previously duplicated
verbatim` comment from the Phase 2 scaffold-split pass, left as-is since it's about a past bug
fix, not the field names), `ChatViewModelTest.kt` (2 tests, renamed alongside their
assertions since the old names — `sets showForwardDialog to true`,
`resets showForwardDialog and forwardingMessage` — would otherwise describe fields that no
longer exist).

**Slice 6 (`ChatContactCardUiState`) — smallest slice, single field:** `contactPhoneLookups` →
`state.contactCard.lookups`. Only one field to move, but `ChatContactCardDelegate.kt` had 7
call sites all sharing the same `it.copy(contactPhoneLookups = it.contactPhoneLookups +
(phone to X))` shape with a different `X` each time — handled with a small Python regex
substitution (`it\.copy\(contactPhoneLookups = it\.contactPhoneLookups \+ \((.*?)\)\)` →
inject the nesting around the captured group) rather than 7 manual edits, verified afterward
by re-reading the whole file since a non-greedy regex across nested parens is exactly the kind
of thing that can silently truncate on a more deeply-nested call site — it didn't here (all 7
sites are a flat `phone to <expr>` with no further nested parens to confuse the match), but
worth the second look every time this shortcut is used. 3 files: `ChatContract.kt` (new
`ChatContactCardUiState`, added next to `ContactPhoneLookup` itself since they're one
concern), `ChatContactCardDelegate.kt` (7 call sites), `ChatMessageList.kt` (2
`state.contactPhoneLookups[phone]` reads feeding `MessageBubble`'s own same-named param —
learned from slice 3's indentation miss, verified both were caught this time with a repo-wide
grep before compiling rather than trusting a single `replace_all`).

**Slice 7 (`ChatSearchUiState`) — a same-named field on two different types:**
`isSearchActive`/`searchQuery`/`searchResults`/`isSearching`/`highlightedMessageId` →
`state.search.{isActive,query,results,isSearching,highlightedMessageId}`. Two gotchas worth
flagging for future slices:
- `searchQuery`/`searchResults`/`isSearchActive` also exist, unrelated, on
  `BroadcastListContract`, `ConversationListContract`, and `CreateGroupContract` — a repo-wide
  grep without `ui/chat` path-scoping would have pulled in files this slice has no business
  touching. Scoped every grep to `app/src/main/java/com/ajrpachon/chatapp/ui/chat` (and the
  matching test package) from the start this time.
- `state.highlightedMessageId` (the `ChatState` field this slice moved) is a *different thing*
  from `ChatScreen`'s own local `var highlightedMessageId by remember` — same name, same
  general purpose (a message ID to visually highlight), but the local one drives the actual
  scroll animation and fade-out and is set from inside the `LaunchedEffect` that *observes*
  `state.search.highlightedMessageId`. Documented the distinction directly on
  `ChatSearchUiState` so it doesn't need re-deriving by reading `ChatScreen.kt` again next
  time. Only the `ChatState` field moved this slice; `ChatScreen`'s local `remember` is
  untouched.

4 files: `ChatContract.kt`, `ChatSearchDelegate.kt` (6 call sites across `searchMessages`/
`jumpToMessage`), `ChatViewModel.kt` (`OpenSearch`/`CloseSearch`), `ChatMessageList.kt` (the
search-overlay call site), `ChatScreen.kt` (the `LaunchedEffect(state.highlightedMessageId)`
that triggers the scroll — the first slice among 1-7 that needed to touch `ChatScreen.kt`
directly rather than only the files it delegates to).

**Slice 8 (`ChatMediaUploadUiState`) — two flows, one nested state:** `isUploadingFile`/
`mediaUploadProgress`/`pendingImageUris`/`suppressedImageMessageIds` →
`state.mediaUpload.{isUploadingFile,progress,pendingImageUris,suppressedImageMessageIds}`.
`ChatMediaUploadDelegate` actually covers two different upload flows sharing one nested
state: `sendFile`/`sendVideo` (a single upload, `isUploadingFile` as a plain busy flag) and
`sendImages` (a batch, with a placeholder bubble and per-item completion tracked via
`progress`/`pendingImageUris`/`suppressedImageMessageIds`) — documented the split directly on
`ChatMediaUploadUiState` so the four fields' relationship isn't left to be re-derived from the
delegate's code. `ChatMediaUploadDelegate.kt`'s call sites were too varied (mixing these
fields with `replyingTo`/`error` in different combinations) for one clean regex like slice 6's,
so each was a targeted edit instead — verified via the same repo-wide grep-before-compile
discipline as every slice since 3. Touched 4 files: `ChatContract.kt`, `ChatMediaUploadDelegate.kt`
(9 call sites), `ChatBottomBar.kt` (2 reads for `NormalInputBar`'s upload-progress display),
`ChatMessageList.kt` (5 reads: the pending-batch placeholder plus 3 `suppressedImageMessageIds`
membership checks in the paging/grouping logic).

**Slice 9 (`ChatGroupPresenceUiState`) — narrowest scope yet by design:** `onlineMemberCount`/
`groupMemberCount` → `state.groupPresence.{onlineMemberCount,memberCount}`. (Numbered slice 9
here, but row 10 in the target-shape table — there's no separate slice for row 9's
`audioState`/`AudioState`, since that was already its own nested type from before this
"Shrinking ChatState/ChatIntent" plan started.) The narrowest grouping so far on purpose:
`isGroup`/`isCurrentUserMember`/`groupAvatarUrl` (conversation identity, set once in
`ChatViewModel.init` from `conversationRepository`) and `isOnline` (network connectivity, from
`NetworkMonitor` — unrelated despite the similarly-shaped name) stay flat, exactly as scoped
in the target-shape table before any code was touched. 3 files: `ChatContract.kt` (also
updated the `subtitleText` computed property, the one non-delegate read of these fields),
`ChatGroupPresenceDelegate.kt` (2 call sites), `ChatTopBar.kt` (1 read, the presence-dot color
check).

**Slice 10 (`ChatMuteUiState`) — first non-delegate-owned group:** `isMuted`/`mutedUntil`/
`showMuteDialog` → `state.mute.{isMuted,mutedUntil,showDialog}`. First slice with no owning
delegate (`toggleMute`/`muteFor` live directly on `ChatViewModel`, per the "what stays"
list from Phase 1) — same mechanical move regardless, just no delegate file in the touched
set. One thing to watch that didn't bite here but is worth naming: `ChatIntent.MuteFor(val
mutedUntil: Long)` and `ConversationBO.isMuted`/`mutedUntil` are same-named fields on
*different* types (an intent payload and a domain model, respectively) — neither should be
touched by this slice, and grepping bare `mutedUntil` without checking each hit's surrounding
type would have flagged both as false positives. 4 files: `ChatContract.kt`,
`ChatViewModel.kt` (the `init` block's conversation-load `updateState`, `ShowMuteDialog`/
`DismissMuteDialog`, `toggleMute`/`muteFor`), `ChatDialogHost.kt` (1 read), `ChatTopBar.kt` (3
reads in the mute/unmute menu item).

**Slice 11 (`ChatThemeUiState`) — a `theme` field on a type named `*ThemeUiState`:** `chatTheme`/
`showThemePicker` → `state.theme.{theme,showPicker}`. `state.theme.theme` reads a little odd at
the call site but is the correct outcome of the same "drop the redundant prefix once nested"
rule every other slice followed — `ChatThemeUiState.theme: ChatTheme` is unambiguous in
context, and `state.theme.themeValue` or similar would break the pattern for no real gain.
Another non-delegate group, like slice 10. 4 files: `ChatContract.kt`, `ChatViewModel.kt` (the
`init` block's `chatThemeRepository.observe` collector, `OpenThemePicker`/
`DismissThemePicker`), `ChatDialogHost.kt` (2 reads for `ChatThemePickerSheet`),
`ChatScreen.kt` (1 read, `chatThemeColors = state.theme.theme.toColors()` — the `Scaffold`
container-color computation, still living in `ChatScreen.kt` itself since it wasn't part of
any of the four scaffold-extraction files).

**Slice 12 (`ChatDisappearingUiState`) — a computed property to update, again:**
`disappearingModeSeconds`/`showDisappearingModeSheet` → `state.disappearing.{seconds,showSheet}`.
Like slice 9's `subtitleText`, `ChatState`'s own `disappearingDurationLabel` computed
property reads `disappearingModeSeconds` implicitly (bare, inside the class body) — a second
reminder that these computed properties, sitting right below the field list they're derived
from, are easy to miss with a call-site-only search and need their own explicit check each
slice. Also confirmed `conv?.disappearingModeSeconds` (read from `ConversationBO`, a domain
model, inside the `init` block's conversation-load `updateState`) is a same-named field on a
different type — only the left-hand `disappearingModeSeconds =` key in that `it.copy(...)`
moved, not the right-hand `conv?.` read. 4 files: `ChatContract.kt` (field + the
`disappearingDurationLabel` computed property), `ChatViewModel.kt` (the `init` block, 2
`onIntent` branches, `sendMessage`, `setDisappearingMode`), `ChatDialogHost.kt` (2 reads for
`DisappearingModeSheet`), `ChatTopBar.kt` (1 read, the disappearing-mode timer icon).

**Slice 13 (`ChatMentionUiState`) — a second wired-but-unsurfaced feature:**
`mentionSuggestions`/`showMentionSuggestions` → `state.mention.{suggestions,showSuggestions}`.
Same shape as slice 2's translation finding: `InputChanged`'s `@mention` matching logic
populates this state, but grep across `ui/chat` found no dropdown/overlay that reads it back —
no UI consumer today. Documented on `ChatMentionUiState` itself, same as the translation
slice. Only 2 files touched — the smallest blast radius of any slice with an owning writer,
because there's no delegate, no dialog host entry, and no top-bar/bottom-bar/message-list
read: `ChatContract.kt`, `ChatViewModel.kt` (`InputChanged`'s two branches, `selectMention`).

**Slice 14 (`ChatIncognitoUiState`) — same-named repository method, different receiver:**
`isIncognito`/`showIncognitoInfoDialog` → `state.incognito.{isIncognito,showInfoDialog}`.
`incognitoRepository.isIncognito(conversationId)` (a repository method call, observed in
`init`) reads identically to the `ChatState` field it feeds — same lesson as slice 10's
`ConversationBO.isMuted`/`MuteFor.mutedUntil`: only the `it.copy(isIncognito = incognito)`
call site moved, the repository call itself is untouched. 4 files: `ChatContract.kt`,
`ChatViewModel.kt` (the `init` block's `incognitoRepository.isIncognito` collector,
`DismissIncognitoDialog`, `toggleIncognito`/`confirmIncognito`), `ChatDialogHost.kt` (1 read),
`ChatTopBar.kt` (3 reads: the incognito banner visibility, the overflow-menu label, and its
icon tint).

**Slice 15 (`ChatWallpaperUiState`) — last slice, same pattern as every non-delegate group
before it:** `wallpaperColor`/`showWallpaperPicker` → `state.wallpaper.{color,showPicker}`.
No surprises — by this point every one of the remaining "no delegate" groups (mute, theme,
disappearing, mention, incognito, wallpaper) had settled into the same shape: one `init`-block
observer or `onIntent` writer, one dialog-host or top-bar reader, occasionally a
`ChatMessageList`/`ChatScreen` read for something rendered mid-conversation (here, the
wallpaper-tinted background behind the message list). 4 files: `ChatContract.kt`,
`ChatViewModel.kt` (the `init` block's `wallpaperRepository.getWallpaperColor` collector,
`OpenWallpaperPicker`/`DismissWallpaperPicker`), `ChatDialogHost.kt` (2 reads for
`WallpaperPickerSheet`), `ChatMessageList.kt` (1 read, the background tint).

### Shrinking ChatState/ChatIntent: done

All 16 rows of the target-shape table are now **Done**. `ChatState` went from 65 flat fields
to 16 nested state objects (`ai`, `translation`, `poll`, `scheduling`, `forward`,
`contactCard`, `search`, `mediaUpload`, `audioState`, `groupPresence`, `mute`, `theme`,
`disappearing`, `mention`, `incognito`, `wallpaper`) plus the ~20 fields that were always core
messaging or conversation identity and stayed flat by design (see the "Left flat on
`ChatState`" paragraph above the target-shape table). Every slice was a pure move — verified
by the same compile→detekt→test cycle each time, `ChatViewModelTest` updated only where a test
literally asserted on a moved field's old name (slices 3 and 5) — spread across 15 commits on
`feat/tema-senal`, none touching more than 6 files.

Two recurring lessons worth keeping in mind for any future `ChatState` work:

- **Same-named-but-different-type fields are everywhere in a chat app.** `ChatIntent.MuteFor`
  vs. `ChatState.mute.mutedUntil`, `ConversationBO.isMuted`/`disappearingModeSeconds` vs. the
  `ChatState` fields they seed, `incognitoRepository.isIncognito()` vs. `state.incognito.
  isIncognito`, `MessageBubble`'s own `pollUiStates`/`contactPhoneLookups` params vs. the
  `ChatState` maps that narrow into them — a bare grep for a field name needs its matches
  checked one by one, not just counted.
- **Not every field this plan moved has a UI reader today.** `translation` (slice 2) and
  `mention` (slice 13) are both populated by their respective delegate/handler but never read
  by any composable — real, pre-existing feature gaps that this plan surfaced by forcing a
  full grep of every field's usage, not something introduced by the moves themselves.

`ChatIntent`'s 78-variant `when` in `onIntent` was **not** split into per-concern sealed
hierarchies — per the original plan, that was always lower priority than the state split, and
nothing in doing the state work surfaced a concrete pain point (a merge-conflict hotspot, a
review-diff problem) that would justify it now. Worth revisiting only if `onIntent`'s `when`
itself becomes the bottleneck in a future session.

## Non-goals

- No sub-`ViewModel`s (Koin's `viewModelOf`/`viewModel` DI wiring, `koinViewModel()` call
  sites in `ChatScreen`, and `SavedStateHandle`/lifecycle semantics all assume one
  `ChatViewModel` per chat screen — introducing more `ViewModel`s would need its own DI and
  lifecycle design, not just a code move).
- No behavior changes bundled into a decomposition commit — each delegate extraction should
  be a pure move (verified by re-running `ChatViewModelTest` unchanged) so a regression is
  easy to bisect to "the code moved wrong," not "the code moved and something else changed."
