package com.ajrpachon.chatapp.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.ui.components.OfflineBanner

/**
 * [ChatScreen]'s `Scaffold.bottomBar`: the offline banner, editing/reply preview strips, the
 * typing indicator, and the recording/audio-preview/normal-input switch. Extracted per
 * docs/chat-viewmodel-decomposition.md Phase 2.
 *
 * The attachment-picker callbacks ([onGallery]/[onCamera]/[onMic]/[onAttachFile]/
 * [onAttachVideo]/[onLocation]/[onContact]) are pre-built closures from [ChatScreen] rather than
 * the raw `ActivityResultLauncher`s they wrap — those launchers must stay registered where
 * `rememberLauncherForActivityResult` was originally called, and passing the finished
 * permission-check-then-launch lambda through is simpler than re-typing 10 launcher generics
 * across this boundary for no benefit.
 */
@Composable
internal fun ChatBottomBar(
    state: ChatState,
    vm: ChatViewModel,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onMic: () -> Unit,
    onAttachFile: () -> Unit,
    onAttachVideo: () -> Unit,
    onLocation: () -> Unit,
    onContact: () -> Unit,
) {
    Column {
        AnimatedVisibility(visible = !state.isOnline) {
            OfflineBanner()
        }
        if (state.isCurrentUserMember) Surface(shadowElevation = 4.dp) {
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))) {
                val editingMessage = state.editingMessage
                if (editingMessage != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(stringResource(R.string.chat_editing_message), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(editingMessage.content.take(60), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        IconButton(onClick = { vm.onIntent(ChatIntent.CancelEdit) }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.chat_cancel_edit))
                        }
                    }
                    HorizontalDivider()
                }
                val replyingTo = state.replyingTo
                if (replyingTo != null) {
                    ReplyPreviewBar(
                        message = replyingTo,
                        onCancel = { vm.onIntent(ChatIntent.CancelReply) },
                    )
                    HorizontalDivider()
                }
                // Typing indicator
                val typingUserNames = state.typingUserNames
                AnimatedVisibility(
                    visible = typingUserNames.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    val typingText = when (typingUserNames.size) {
                        1 -> stringResource(R.string.chat_typing_single, typingUserNames[0])
                        else -> stringResource(R.string.chat_typing_multiple, typingUserNames.take(2).joinToString(" y "))
                    }
                    Text(
                        text = typingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    )
                }
                val audioState = state.audioState
                when {
                    audioState.isRecording -> RecordingBar(
                        durationMs = audioState.recordingDurationMs,
                        amplitudeHistory = audioState.amplitudeHistory,
                        onStop = { vm.onIntent(ChatIntent.StopRecording) },
                    )
                    audioState.pendingFilePath != null -> AudioPreviewBar(
                        filePath = audioState.pendingFilePath,
                        amplitudeHistory = audioState.amplitudeHistory,
                        isUploading = audioState.isUploading,
                        onDiscard = { vm.onIntent(ChatIntent.DiscardAudio) },
                        onSend = { vm.onIntent(ChatIntent.SendAudio) },
                    )
                    else -> NormalInputBar(
                        inputText = state.inputText,
                        isSending = state.isSending,
                        isUploadingImage = state.isUploadingFile,
                        mediaUploadProgress = state.mediaUploadProgress,
                        onTextChange = { vm.onIntent(ChatIntent.InputChanged(it)) },
                        onSend = { vm.onIntent(ChatIntent.Send) },
                        onGallery = onGallery,
                        onCamera = onCamera,
                        onMic = onMic,
                        onSticker = { vm.onIntent(ChatIntent.OpenStickerPicker) },
                        onAttachFile = onAttachFile,
                        onAttachVideo = onAttachVideo,
                        onLocation = onLocation,
                        onContact = onContact,
                        onSchedule = { vm.onIntent(ChatIntent.OpenScheduleDialog) },
                        onAi = { vm.onIntent(ChatIntent.OpenAiSheet) },
                        onCreatePoll = { vm.onIntent(ChatIntent.OpenCreatePollSheet) },
                    )
                }
            }
        }
    }
}
