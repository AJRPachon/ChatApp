package com.ajrpachon.chatapp.ui.chat

import android.content.Context
import android.net.Uri
import com.ajrpachon.chatapp.data.local.ChatTheme
import com.ajrpachon.chatapp.domain.model.CallBO
import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.model.GroupMemberBO
import com.ajrpachon.chatapp.domain.model.MessageBO

data class AudioState(
    val isRecording: Boolean = false,
    val recordingDurationMs: Long = 0L,
    val pendingFilePath: String? = null,
    val isUploading: Boolean = false,
    val amplitudeHistory: List<Float> = emptyList(),
    val transcription: String? = null,
)

data class ChatState(
    val inputText: String = "",
    val isSending: Boolean = false,
    val isUploadingImage: Boolean = false,
    val isUploadingFile: Boolean = false,
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
    val isMuted: Boolean = false,
    val mutedUntil: Long = 0L,
    val showMuteDialog: Boolean = false,
    val editingMessage: MessageBO? = null,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<MessageBO> = emptyList(),
    val isSearching: Boolean = false,
    val highlightedMessageId: String? = null,
    val expiryDialogMessageId: String? = null,
    val selectedMessageIds: Set<String> = emptySet(),
    val showForwardDialog: Boolean = false,
    val forwardingMessage: MessageBO? = null,
    val forwardableConversations: List<ConversationBO> = emptyList(),
    val typingUserNames: List<String> = emptyList(),
    // messageId → translated text
    val translatedTexts: Map<String, String> = emptyMap(),
    val translatingMessageIds: Set<String> = emptySet(),
    val audioTranscriptions: Map<String, String> = emptyMap(),
    val pinnedMessages: List<MessageBO> = emptyList(),
    val showCreatePollSheet: Boolean = false,
    val isExporting: Boolean = false,
    val chatTheme: ChatTheme = ChatTheme.DEFAULT,
    val showThemePicker: Boolean = false,
    // 0 = off, >0 = seconds for new messages to auto-expire
    val disappearingModeSeconds: Long = 0L,
    val showDisappearingModeSheet: Boolean = false,
    val mentionSuggestions: List<GroupMemberBO> = emptyList(),
    val showMentionSuggestions: Boolean = false,
    // Incognito mode: when true, messages are NOT persisted to local Room DB
    val isIncognito: Boolean = false,
    // Scheduled messages
    val showScheduleDialog: Boolean = false,
    val scheduledAtMs: Long? = null,
    val scheduledMessageCount: Int = 0,
    // AI Assistant
    val showAiSheet: Boolean = false,
    val aiSuggestion: String? = null,
    val isAiLoading: Boolean = false,
) {
    val isMultiSelectActive: Boolean get() = selectedMessageIds.isNotEmpty()
    val latestPinnedMessage: MessageBO? get() = pinnedMessages.firstOrNull()
}

sealed interface ChatIntent {
    data class InputChanged(val text: String) : ChatIntent
    data object Send : ChatIntent
    data class SendImages(val context: Context, val uris: List<Uri>) : ChatIntent
    data class SendFile(val context: Context, val uri: Uri) : ChatIntent
    data class SendVideo(val context: Context, val uri: Uri) : ChatIntent
    data class StartRecording(val context: Context, val outputFilePath: String) : ChatIntent
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
    data class SendLocation(val mapsUrl: String) : ChatIntent
    data class TranslateMessage(val messageId: String, val text: String) : ChatIntent
    data class DismissTranslation(val messageId: String) : ChatIntent
    data class TranscribeAudio(val context: Context, val messageId: String) : ChatIntent
    data class PinMessage(val messageId: String) : ChatIntent
    data class UnpinMessage(val messageId: String) : ChatIntent
    data class SaveMessage(val messageId: String) : ChatIntent
    data class UnsaveMessage(val messageId: String) : ChatIntent
    data object OpenCreatePollSheet : ChatIntent
    data object DismissCreatePollSheet : ChatIntent
    data class CreatePoll(val question: String, val options: List<String>) : ChatIntent
    data class VotePoll(val pollId: String, val optionId: String) : ChatIntent
    data class SetChatTheme(val theme: ChatTheme) : ChatIntent
    data object OpenThemePicker : ChatIntent
    data object DismissThemePicker : ChatIntent
    data class ExportConversation(val context: Context) : ChatIntent
    data object ShowDisappearingModeSheet : ChatIntent
    data object DismissDisappearingModeSheet : ChatIntent
    // seconds: 0 = off, positive = duration in seconds
    data class SetDisappearingMode(val conversationId: String, val seconds: Long) : ChatIntent
    data class SelectMention(val member: GroupMemberBO) : ChatIntent
    data object ToggleIncognito : ChatIntent
    // Scheduled messages
    data object OpenScheduleDialog : ChatIntent
    data object DismissScheduleDialog : ChatIntent
    // scheduledAt: epoch millis when the message should be sent
    data class ScheduleMessage(val scheduledAt: Long) : ChatIntent
    // AI Assistant
    data object OpenAiSheet : ChatIntent
    data object DismissAiSheet : ChatIntent
    data object AiSummarize : ChatIntent
    data object AiSuggestReply : ChatIntent
    data class AiFreeform(val prompt: String) : ChatIntent
    data object InsertAiSuggestion : ChatIntent
}

sealed interface ChatEffect {
    data object ScrollToBottom : ChatEffect
    data class NavigateToCall(val call: CallBO) : ChatEffect
    data object NavigateBack : ChatEffect
    data class ShowSnackbar(val message: String) : ChatEffect
    data class ShowShareSheet(val uri: android.net.Uri) : ChatEffect
}
