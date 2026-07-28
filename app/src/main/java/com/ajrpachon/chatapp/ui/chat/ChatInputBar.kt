package com.ajrpachon.chatapp.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.model.MessageLimits
import com.ajrpachon.chatapp.ui.components.ChatAppTextField
import kotlinx.coroutines.launch

// ── Chat input bar ────────────────────────────────────────────────────────────

@Suppress("LongParameterList")
@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
internal fun NormalInputBar(
    inputText: String,
    isSending: Boolean,
    isUploadingImage: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onMic: () -> Unit,
    onSticker: () -> Unit,
    onAttachFile: () -> Unit = {},
    onAttachVideo: () -> Unit = {},
    onLocation: () -> Unit = {},
    onContact: () -> Unit = {},
    onSchedule: () -> Unit = {},
    onAi: () -> Unit = {},
    onCreatePoll: () -> Unit = {},
) {
    val busy = isUploadingImage || isSending
    var showAttachSheet by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        IconButton(
            onClick = { showAttachSheet = true },
            enabled = !busy,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Adjuntar",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        ChatAppTextField(
            value = inputText,
            onValueChange = { if (it.length <= MessageLimits.MAX_CONTENT_LENGTH) onTextChange(it) },
            modifier = Modifier.weight(1f),
            placeholder = "Mensaje…",
            singleLine = false,
            maxLines = 4,
            isError = inputText.length >= MessageLimits.MAX_CONTENT_LENGTH,
            supportingText = if (inputText.length >= MessageLimits.MAX_CONTENT_LENGTH - 100)
                "${inputText.length}/${MessageLimits.MAX_CONTENT_LENGTH}" else null,
        )
        if (isUploadingImage) {
            CircularProgressIndicator(modifier = Modifier.size(40.dp).padding(8.dp))
        } else if (inputText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .combinedClickable(
                        enabled = !isSending,
                        onClick = onSend,
                        onLongClick = onSchedule,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar (mantén para programar)",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            IconButton(onClick = onAi, enabled = !busy) {
                Icon(Icons.Default.SmartToy, contentDescription = "Asistente IA")
            }
            IconButton(onClick = onMic, enabled = !busy) {
                Icon(Icons.Default.Mic, contentDescription = "Grabar audio")
            }
        }
    }

    if (showAttachSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachSheet = false },
            sheetState = sheetState,
        ) {
            AttachmentBottomSheet(
                onGallery = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showAttachSheet = false
                        onGallery()
                    }
                },
                onCamera = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showAttachSheet = false
                        onCamera()
                    }
                },
                onFile = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showAttachSheet = false
                        onAttachFile()
                    }
                },
                onVideo = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showAttachSheet = false
                        onAttachVideo()
                    }
                },
                onSticker = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showAttachSheet = false
                        onSticker()
                    }
                },
                onLocation = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showAttachSheet = false
                        onLocation()
                    }
                },
                onCreatePoll = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showAttachSheet = false
                        onCreatePoll()
                    }
                },
                onContact = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showAttachSheet = false
                        onContact()
                    }
                },
                onSchedule = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showAttachSheet = false
                        onSchedule()
                    }
                },
                onAi = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showAttachSheet = false
                        onAi()
                    }
                },
                isScheduleEnabled = inputText.isNotBlank(),
            )
        }
    }
}

// ── Attachment bottom sheet ───────────────────────────────────────────────────

private data class AttachmentOption(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val action: () -> Unit,
    val enabled: Boolean = true,
)

@Composable
internal fun AttachmentBottomSheet(
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onFile: () -> Unit,
    onVideo: () -> Unit,
    onSticker: () -> Unit,
    onLocation: () -> Unit = {},
    onCreatePoll: () -> Unit = {},
    onContact: () -> Unit = {},
    onSchedule: () -> Unit = {},
    onAi: () -> Unit = {},
    isScheduleEnabled: Boolean = true,
) {
    val options = listOf(
        AttachmentOption(Icons.Default.AddPhotoAlternate, "Galería", onGallery),
        AttachmentOption(Icons.Default.CameraAlt, "Cámara", onCamera),
        AttachmentOption(Icons.Default.AttachFile, "Archivo", onFile),
        AttachmentOption(Icons.Default.Videocam, "Video", onVideo),
        AttachmentOption(Icons.Default.EmojiEmotions, "Stickers", onSticker),
        AttachmentOption(Icons.Default.LocationOn, "Ubicación", onLocation),
        AttachmentOption(Icons.Default.CheckCircle, "Encuesta", onCreatePoll),
        AttachmentOption(Icons.Default.Contacts, "Contacto", onContact),
        AttachmentOption(Icons.Default.Schedule, "Programar", onSchedule, enabled = isScheduleEnabled),
        AttachmentOption(Icons.Default.SmartToy, "IA", onAi),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 32.dp),
    ) {
        Text(
            text = "Adjuntar",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )
        val columnsPerRow = 4
        options.chunked(columnsPerRow).forEach { rowOptions ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                rowOptions.forEach { option ->
                    val contentAlpha = if (option.enabled) 1f else 0.4f
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = option.enabled, onClick = option.action)
                            .padding(vertical = 12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = contentAlpha)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = option.label,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = contentAlpha),
                                modifier = Modifier.size(32.dp),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                // Pad the last row with empty spacers so items stay left-aligned in a 4-column grid
                repeat(columnsPerRow - rowOptions.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ── Reply preview bar ─────────────────────────────────────────────────────────

@Composable
internal fun ReplyPreviewBar(message: MessageBO, onCancel: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Reply,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
            Text(
                text = message.replySnippet(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Cancelar respuesta",
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
