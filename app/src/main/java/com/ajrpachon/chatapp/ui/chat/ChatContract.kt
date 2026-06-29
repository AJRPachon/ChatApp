package com.ajrpachon.chatapp.ui.chat

import android.content.Context
import android.net.Uri
import com.ajrpachon.chatapp.domain.model.CallBO
import com.ajrpachon.chatapp.domain.model.MessageBO

data class AudioState(
    val isRecording: Boolean = false,
    val recordingDurationMs: Long = 0L,
    val pendingFilePath: String? = null,
    val isUploading: Boolean = false,
    val amplitudeHistory: List<Float> = emptyList(),
)

data class ChatState(
    val inputText: String = "",
    val isSending: Boolean = false,
    val isUploadingImage: Boolean = false,
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
    val typingUserNames: List<String> = emptyList(),
)

sealed interface ChatIntent {
    data class InputChanged(val text: String) : ChatIntent
    data object Send : ChatIntent
    data class SendImages(val context: Context, val uris: List<Uri>) : ChatIntent
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
}

sealed interface ChatEffect {
    data object ScrollToBottom : ChatEffect
    data class NavigateToCall(val call: CallBO) : ChatEffect
    data object NavigateBack : ChatEffect
}
