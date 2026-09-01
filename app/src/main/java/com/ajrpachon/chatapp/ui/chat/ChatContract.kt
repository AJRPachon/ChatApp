package com.ajrpachon.chatapp.ui.chat

import android.net.Uri
import com.ajrpachon.chatapp.domain.model.ChatTheme
import com.ajrpachon.chatapp.ui.common.formatDisappearingDuration
import com.ajrpachon.chatapp.ui.common.formatLastSeen
import com.ajrpachon.chatapp.domain.model.CallBO
import com.ajrpachon.chatapp.domain.model.ScheduledMessage
import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.model.GroupMemberBO
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.model.UserRelationship
import com.ajrpachon.chatapp.domain.model.PollBO
import com.ajrpachon.chatapp.domain.model.PollOptionBO
import com.ajrpachon.chatapp.domain.model.PollVoteBO
import com.ajrpachon.chatapp.utils.LinkPreviewData

/**
 * Relationship of the current user to whoever owns a shared contact card's phone number.
 * Keyed by phone (not by ChatState.otherUserId) because the contact card can appear in
 * group chats where otherUserId is null/irrelevant to the card's own contact.
 */
data class ContactPhoneLookup(
    val resolvedUser: UserBO? = null,
    val relationship: UserRelationship? = null,
    val isLoading: Boolean = false,
)

/**
 * Contact-card relationship lookups, owned by [ChatContactCardDelegate]. Sixth of the nested
 * groups in docs/chat-viewmodel-decomposition.md's "Shrinking ChatState/ChatIntent" plan.
 */
data class ChatContactCardUiState(
    // Keyed by phone (see ContactPhoneLookup doc).
    val lookups: Map<String, ContactPhoneLookup> = emptyMap(),
)

/**
 * In-conversation message search + jump-to-message highlighting, owned by
 * [ChatSearchDelegate]. Seventh of the nested groups in
 * docs/chat-viewmodel-decomposition.md's "Shrinking ChatState/ChatIntent" plan.
 *
 * [highlightedMessageId] lives here (not a separate top-level field) because it's only ever
 * set by [ChatSearchDelegate.jumpToMessage] — reached both from a tapped search result and
 * from a tapped reply-quote, but the highlight-and-fade-out behavior itself is this delegate's
 * concern either way. Distinct from `ChatScreen`'s own local `highlightedMessageId` (`by
 * remember`), which drives the actual scroll/visual-highlight animation and is set from the
 * `LaunchedEffect(state.search.highlightedMessageId)` that observes this field.
 */
data class ChatSearchUiState(
    val isActive: Boolean = false,
    val query: String = "",
    val results: List<MessageBO> = emptyList(),
    val isSearching: Boolean = false,
    val highlightedMessageId: String? = null,
)

/**
 * Image-batch/file/video upload state, owned by [ChatMediaUploadDelegate]. Eighth of the
 * nested groups in docs/chat-viewmodel-decomposition.md's "Shrinking ChatState/ChatIntent"
 * plan.
 *
 * [isUploadingFile] covers `sendFile`/`sendVideo` (a single upload, no per-item progress);
 * [progress]/[pendingImageUris]/[suppressedImageMessageIds] cover `sendImages` (a batch, with
 * a placeholder bubble and per-item completion tracking) — see their own docs below.
 */
data class ChatMediaUploadUiState(
    val isUploadingFile: Boolean = false,
    val progress: MediaUploadProgress? = null,
    // Local URIs for the in-flight image batch, so the placeholder bubble can render
    // thumbnails instantly (from device storage) without waiting on network uploads.
    val pendingImageUris: List<Uri> = emptyList(),
    // Ids of messages created by the current in-flight batch; hidden from the normal
    // paging-based grouping until the whole batch finishes, so only the stable
    // placeholder bubble is visible mid-upload instead of a growing/jumping bubble.
    val suppressedImageMessageIds: Set<String> = emptySet(),
)

/** Poll data + the current user's vote(s), kept in [ChatPollFeatureState.uiStates] keyed by pollId. */
data class PollUiState(
    val poll: PollBO? = null,
    val options: List<PollOptionBO> = emptyList(),
    val userVotes: List<PollVoteBO> = emptyList(),
)

data class AudioState(
    val isRecording: Boolean = false,
    val recordingDurationMs: Long = 0L,
    val pendingFilePath: String? = null,
    val isUploading: Boolean = false,
    val amplitudeHistory: List<Float> = emptyList(),
    val transcription: String? = null,
)

data class MediaUploadProgress(
    val totalCount: Int,
    val completedCount: Int,
    val totalBytes: Long,
)

/**
 * AI-assistant sheet state, owned by [ChatAiDelegate]. First of the nested groups in
 * docs/chat-viewmodel-decomposition.md's "Shrinking ChatState/ChatIntent" plan.
 */
data class ChatAiUiState(
    val showSheet: Boolean = false,
    val suggestion: String? = null,
    val isLoading: Boolean = false,
)

/**
 * Per-message translation/transcription state, owned by [ChatTranslationDelegate]. Second of
 * the nested groups in docs/chat-viewmodel-decomposition.md's "Shrinking ChatState/ChatIntent"
 * plan.
 */
data class ChatTranslationUiState(
    // messageId → translated text
    val translatedTexts: Map<String, String> = emptyMap(),
    val translatingMessageIds: Set<String> = emptySet(),
    // messageId → transcribed text
    val transcriptions: Map<String, String> = emptyMap(),
)

/**
 * Poll creation-sheet visibility + per-poll observed data, owned by [ChatPollDelegate]. Third
 * of the nested groups in docs/chat-viewmodel-decomposition.md's "Shrinking ChatState/
 * ChatIntent" plan.
 */
data class ChatPollFeatureState(
    val showCreateSheet: Boolean = false,
    // pollId → poll/options/vote, populated on demand via ChatIntent.ObservePoll
    val uiStates: Map<String, PollUiState> = emptyMap(),
)

/**
 * Scheduled-message dialog/sheet visibility + the live list, owned by
 * [ChatSchedulingDelegate]. Fourth of the nested groups in
 * docs/chat-viewmodel-decomposition.md's "Shrinking ChatState/ChatIntent" plan.
 *
 * [scheduledAtMs] is set by [ChatSchedulingDelegate.scheduleMessage] but never read back
 * anywhere — same kind of write-only leftover as `pinnedBannerVisible` in `ChatScreen`, kept
 * as-is rather than guessed at during a pure-move slice.
 */
data class ChatSchedulingUiState(
    val showDialog: Boolean = false,
    val scheduledAtMs: Long? = null,
    val messageCount: Int = 0,
    val showSheet: Boolean = false,
    val messages: List<ScheduledMessage> = emptyList(),
)

/**
 * Single-message forward dialog + multi-select forward-selection dialog, owned by
 * [ChatForwardDelegate]. Fifth of the nested groups in
 * docs/chat-viewmodel-decomposition.md's "Shrinking ChatState/ChatIntent" plan.
 *
 * [conversations] is shared by both dialogs (only one is ever shown at a time, per
 * [ChatForwardDelegate]) rather than duplicated per-dialog.
 */
data class ChatForwardUiState(
    val showDialog: Boolean = false,
    val message: MessageBO? = null,
    val showSelectionDialog: Boolean = false,
    val conversations: List<ConversationBO> = emptyList(),
)

/**
 * Live group-member presence, owned by [ChatGroupPresenceDelegate]. Tenth of the nested groups
 * in docs/chat-viewmodel-decomposition.md's "Shrinking ChatState/ChatIntent" plan (there is no
 * ninth row here — `audioState`/[AudioState] was already grouped before this plan started).
 *
 * Deliberately narrow: `isGroup`/`isCurrentUserMember`/`groupAvatarUrl` stay flat on
 * [ChatState] — they're conversation identity set once from `conversationRepository` in
 * `ChatViewModel.init`, not produced by this delegate. `state.isOnline` (network connectivity,
 * from `NetworkMonitor`) also stays flat despite the similarly-shaped name — unrelated concern.
 */
data class ChatGroupPresenceUiState(
    val onlineMemberCount: Int = 0,
    val memberCount: Int = 0,
)

/**
 * Mute state + its confirmation dialog, owned directly by `ChatViewModel` (no delegate — see
 * `toggleMute`/`muteFor`). Eleventh of the nested groups in
 * docs/chat-viewmodel-decomposition.md's "Shrinking ChatState/ChatIntent" plan.
 */
data class ChatMuteUiState(
    val isMuted: Boolean = false,
    // -1L = forever, 0L = unmute, positive = until epoch millis
    val mutedUntil: Long = 0L,
    val showDialog: Boolean = false,
)

/**
 * Chat wallpaper theme (colors, bubble style) + its picker sheet, owned directly by
 * `ChatViewModel` (no delegate — set via `setChatTheme`, observed from `chatThemeRepository`
 * in `init`). Twelfth of the nested groups in docs/chat-viewmodel-decomposition.md's
 * "Shrinking ChatState/ChatIntent" plan.
 */
data class ChatThemeUiState(
    val theme: ChatTheme = ChatTheme.DEFAULT,
    val showPicker: Boolean = false,
)

/**
 * Disappearing-messages mode + its picker sheet, owned directly by `ChatViewModel` (no
 * delegate — set via `setDisappearingMode`). Thirteenth of the nested groups in
 * docs/chat-viewmodel-decomposition.md's "Shrinking ChatState/ChatIntent" plan.
 */
data class ChatDisappearingUiState(
    // 0 = off, >0 = seconds for new messages to auto-expire
    val seconds: Long = 0L,
    val showSheet: Boolean = false,
)

/**
 * `@mention` autocomplete suggestions, owned directly by `ChatViewModel` (matched against
 * [ChatGroupPresenceDelegate.groupMembers] inside the `InputChanged` intent handler).
 * Fourteenth of the nested groups in docs/chat-viewmodel-decomposition.md's "Shrinking
 * ChatState/ChatIntent" plan.
 *
 * Like [ChatTranslationUiState], this has **no UI consumer today** — `InputChanged` populates
 * it, but no dropdown/overlay currently reads it to show the suggestions. Real feature gap,
 * not something this slice's pure move should paper over.
 */
data class ChatMentionUiState(
    val suggestions: List<GroupMemberBO> = emptyList(),
    val showSuggestions: Boolean = false,
)

data class ChatState(
    val inputText: String = "",
    val isSending: Boolean = false,
    val mediaUpload: ChatMediaUploadUiState = ChatMediaUploadUiState(),
    val currentUserId: String? = null,
    val conversationTitle: String = "",
    val error: String? = null,
    val audioState: AudioState = AudioState(),
    val otherUserId: String? = null,
    val otherUserAvatarUrl: String? = null,
    val isOtherUserOnline: Boolean = false,
    val otherUserLastSeenMs: Long? = null,
    val groupAvatarUrl: String? = null,
    val isGroup: Boolean = false,
    val isCurrentUserMember: Boolean = true,
    val replyingTo: MessageBO? = null,
    val showStickerPicker: Boolean = false,
    val mute: ChatMuteUiState = ChatMuteUiState(),
    val editingMessage: MessageBO? = null,
    val search: ChatSearchUiState = ChatSearchUiState(),
    val expiryDialogMessageId: String? = null,
    val selectedMessageIds: Set<String> = emptySet(),
    val forward: ChatForwardUiState = ChatForwardUiState(),
    val typingUserNames: List<String> = emptyList(),
    val translation: ChatTranslationUiState = ChatTranslationUiState(),
    val pinnedMessages: List<MessageBO> = emptyList(),
    val poll: ChatPollFeatureState = ChatPollFeatureState(),
    val isExporting: Boolean = false,
    val theme: ChatThemeUiState = ChatThemeUiState(),
    val disappearing: ChatDisappearingUiState = ChatDisappearingUiState(),
    val mention: ChatMentionUiState = ChatMentionUiState(),
    // Incognito mode: when true, messages are NOT persisted to local Room DB
    val isIncognito: Boolean = false,
    val showIncognitoInfoDialog: Boolean = false,
    val scheduling: ChatSchedulingUiState = ChatSchedulingUiState(),
    // AI Assistant
    val ai: ChatAiUiState = ChatAiUiState(),
    // Wallpaper
    val wallpaperColor: Long? = null,
    val showWallpaperPicker: Boolean = false,
    val groupPresence: ChatGroupPresenceUiState = ChatGroupPresenceUiState(),
    val isOnline: Boolean = true,
    val contactCard: ChatContactCardUiState = ChatContactCardUiState(),
    // Link previews: detected URL → fetched preview (null while loading or not found)
    val linkPreviews: Map<String, LinkPreviewData?> = emptyMap(),
) {
    val isMultiSelectActive: Boolean get() = selectedMessageIds.isNotEmpty()
    val latestPinnedMessage: MessageBO? get() = pinnedMessages.firstOrNull()

    /** Formatted label for the disappearing-mode timer shown in the app bar. */
    val disappearingDurationLabel: String get() = formatDisappearingDuration(disappearing.seconds)

    /** Formatted presence text for 1-1 chats ("En línea" or "última vez hace X"). */
    val presenceText: String?
        get() = when {
            isGroup -> null
            isOtherUserOnline -> "En línea"
            otherUserLastSeenMs != null ->
                formatLastSeen(System.currentTimeMillis() - otherUserLastSeenMs)
            else -> null
        }

    /** Group subtitle shown in the app bar ("X en línea" or "X miembros"). */
    val subtitleText: String?
        get() = when {
            !isGroup -> null
            groupPresence.onlineMemberCount > 0 -> "${groupPresence.onlineMemberCount} en línea"
            groupPresence.memberCount > 0 -> "${groupPresence.memberCount} miembros"
            else -> null
        }
}

sealed interface ChatIntent {
    data class InputChanged(val text: String) : ChatIntent
    data object Send : ChatIntent
    data class SendImages(val uris: List<Uri>) : ChatIntent
    data class SendFile(val uri: Uri) : ChatIntent
    data class SendVideo(val uri: Uri) : ChatIntent
    data object StartRecording : ChatIntent
    data object StopRecording : ChatIntent
    data object DiscardAudio : ChatIntent
    data object SendAudio : ChatIntent
    data class StartCall(val callType: String) : ChatIntent
    data object DismissError : ChatIntent
    data class SetReply(val message: MessageBO) : ChatIntent
    data object CancelReply : ChatIntent
    data object OpenStickerPicker : ChatIntent
    data object CloseStickerPicker : ChatIntent
    data class SendGif(val url: String) : ChatIntent
    data class SendSticker(val emoji: String) : ChatIntent
    data object ToggleMute : ChatIntent
    data object ShowMuteDialog : ChatIntent
    data object DismissMuteDialog : ChatIntent
    // mutedUntil: -1L = forever, 0L = unmute, positive = until epoch millis
    data class MuteFor(val mutedUntil: Long) : ChatIntent
    data object LeaveGroup : ChatIntent
    data class DeleteMessage(val messageId: String) : ChatIntent
    data class StartEdit(val message: MessageBO) : ChatIntent
    data object CancelEdit : ChatIntent
    data object ConfirmEdit : ChatIntent
    data object OpenSearch : ChatIntent
    data object CloseSearch : ChatIntent
    data class SearchQueryChanged(val query: String) : ChatIntent
    data class ToggleReaction(val messageId: String, val emoji: String) : ChatIntent
    data class JumpToMessage(val messageId: String) : ChatIntent
    data class ShowExpiryDialog(val messageId: String) : ChatIntent
    data object DismissExpiryDialog : ChatIntent
    // expiresAt: null = remove expiry, positive = epoch millis
    data class SetExpiry(val messageId: String, val expiresAt: Long?) : ChatIntent
    data class ToggleMessageSelection(val messageId: String) : ChatIntent
    data object ClearSelection : ChatIntent
    data object DeleteSelectedMessages : ChatIntent
    data class ShowForwardDialog(val message: MessageBO) : ChatIntent
    data object DismissForwardDialog : ChatIntent
    data class ForwardMessage(val messageId: String, val targetConversationId: String) : ChatIntent
    data class ForwardSelectedMessages(val targetConversationId: String) : ChatIntent
    data class SendLocation(val mapsUrl: String) : ChatIntent
    data class TranslateMessage(val messageId: String, val text: String) : ChatIntent
    data class DismissTranslation(val messageId: String) : ChatIntent
    data class TranscribeAudio(val messageId: String) : ChatIntent
    data class PinMessage(val messageId: String) : ChatIntent
    data class UnpinMessage(val messageId: String) : ChatIntent
    data class SaveMessage(val messageId: String) : ChatIntent
    data class UnsaveMessage(val messageId: String) : ChatIntent
    data object OpenCreatePollSheet : ChatIntent
    data object DismissCreatePollSheet : ChatIntent
    data class CreatePoll(val question: String, val options: List<String>, val allowMultiple: Boolean = false) : ChatIntent
    data class VotePoll(val pollId: String, val optionId: String) : ChatIntent
    data class SetChatTheme(val theme: ChatTheme) : ChatIntent
    data object OpenThemePicker : ChatIntent
    data object DismissThemePicker : ChatIntent
    data object ExportConversation : ChatIntent
    data object ShowDisappearingModeSheet : ChatIntent
    data object DismissDisappearingModeSheet : ChatIntent
    // seconds: 0 = off, positive = duration in seconds
    data class SetDisappearingMode(val conversationId: String, val seconds: Long) : ChatIntent
    data class SelectMention(val member: GroupMemberBO) : ChatIntent
    data object ToggleIncognito : ChatIntent
    data object DismissIncognitoDialog : ChatIntent
    data object ConfirmIncognito : ChatIntent
    // Scheduled messages
    data object OpenScheduleDialog : ChatIntent
    data object DismissScheduleDialog : ChatIntent
    // scheduledAt: epoch millis when the message should be sent
    data class ScheduleMessage(val scheduledAt: Long) : ChatIntent
    data object ShowScheduledSheet : ChatIntent
    data object DismissScheduledSheet : ChatIntent
    data class CancelScheduledMessage(val id: String) : ChatIntent
    // AI Assistant
    data object OpenAiSheet : ChatIntent
    data object DismissAiSheet : ChatIntent
    data object AiSummarize : ChatIntent
    data object AiSuggestReply : ChatIntent
    data class AiFreeform(val prompt: String) : ChatIntent
    data object InsertAiSuggestion : ChatIntent
    data object OpenWallpaperPicker : ChatIntent
    data object DismissWallpaperPicker : ChatIntent
    data class SetWallpaperColor(val color: Long?) : ChatIntent
    data class SendContact(val name: String, val phone: String) : ChatIntent
    data class ContactSelected(val uri: android.net.Uri) : ChatIntent
    data class RetryMessage(val messageId: String) : ChatIntent
    data class CopyMessageContent(val content: String) : ChatIntent
    data object FetchAndSendLocation : ChatIntent
    // Multi-forward
    data object ShowForwardSelectionDialog : ChatIntent
    data object DismissForwardSelectionDialog : ChatIntent
    // Contact card actions
    data class CheckContactRelationship(val phone: String) : ChatIntent
    data class ContactCardPrimaryAction(val phone: String) : ChatIntent
    // Polls: rendered lazily by PollBubble; tells the ViewModel to start observing this poll
    data class ObservePoll(val pollId: String) : ChatIntent
    // Link previews: notifies the ViewModel a URL was detected in a message so it can fetch a preview
    data class DetectedUrlChanged(val url: String) : ChatIntent
}

sealed interface ChatEffect {
    data object ScrollToBottom : ChatEffect
    data class NavigateToCall(val call: CallBO) : ChatEffect
    data object NavigateBack : ChatEffect
    data class ShowSnackbar(val message: String) : ChatEffect
    data class ShowShareSheet(val uri: android.net.Uri) : ChatEffect
    data class NavigateToConversation(val conversationId: String, val otherUserName: String) : ChatEffect
    data class InviteContact(val phoneNumber: String, val text: String) : ChatEffect
}
