package com.ajrpachon.chatapp.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.domain.model.ChatTheme

// ── Modal bottom sheets used from ChatScreen ────────────────────────────────
// Extracted per docs/chat-viewmodel-decomposition.md Phase 2 — each takes only its own narrow
// params, no dependency on ChatViewModel or the message-bubble rendering tree.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatThemePickerSheet(
    currentTheme: ChatTheme,
    onSelect: (ChatTheme) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.chat_chat_theme),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                items(ChatTheme.entries.size, key = { it }) { index ->
                    val theme = ChatTheme.entries[index]
                    val isSelected = theme == currentTheme
                    val label = when (theme) {
                        ChatTheme.DEFAULT -> stringResource(R.string.chat_theme_default)
                        ChatTheme.OCEAN -> stringResource(R.string.chat_theme_ocean)
                        ChatTheme.SUNSET -> stringResource(R.string.chat_theme_sunset)
                        ChatTheme.FOREST -> stringResource(R.string.chat_theme_forest)
                        ChatTheme.LAVENDER -> stringResource(R.string.chat_theme_lavender)
                        ChatTheme.ROSE -> stringResource(R.string.chat_theme_rose)
                        ChatTheme.MIDNIGHT -> stringResource(R.string.chat_theme_midnight)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            onSelect(theme)
                            onDismiss()
                        },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    color = if (theme == ChatTheme.DEFAULT)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        theme.toColors().bubbleColor,
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = stringResource(R.string.chat_selected),
                                    tint = if (theme == ChatTheme.MIDNIGHT)
                                        Color.White
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DisappearingModeSheet(
    currentSeconds: Long,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val options = listOf(
        stringResource(R.string.chat_disabled) to 0L,
        stringResource(R.string.chat_24_hours) to 86_400L,
        stringResource(R.string.chat_7_days) to 604_800L,
        stringResource(R.string.chat_30_days) to 2_592_000L,
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                stringResource(R.string.chat_disappearing_mode),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            Text(
                stringResource(R.string.chat_disappearing_mode_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            )
            options.forEach { (label, seconds) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(seconds) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, modifier = Modifier.weight(1f))
                    if (seconds == currentSeconds) {
                        Icon(
                            Icons.Default.Done,
                            contentDescription = stringResource(R.string.chat_selected),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun AiAssistantSheet(
    aiSuggestion: String?,
    isAiLoading: Boolean,
    onDismiss: () -> Unit,
    onSuggestReply: () -> Unit,
    onFreeform: (String) -> Unit,
    onInsert: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var freeformText by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.chat_ai_assistant),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            // Action chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                androidx.compose.material3.SuggestionChip(
                    onClick = onSuggestReply,
                    label = { Text(stringResource(R.string.chat_suggest_reply)) },
                    enabled = !isAiLoading,
                )
            }

            // Free-form input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = freeformText,
                    onValueChange = { freeformText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.chat_ask_question_placeholder)) },
                    singleLine = true,
                    enabled = !isAiLoading,
                )
                IconButton(
                    onClick = {
                        if (freeformText.isNotBlank()) {
                            onFreeform(freeformText)
                            freeformText = ""
                        }
                    },
                    enabled = freeformText.isNotBlank() && !isAiLoading,
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.chat_send_query))
                }
            }

            // Loading indicator
            if (isAiLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            // Result area
            if (aiSuggestion != null) {
                Text(
                    text = stringResource(R.string.chat_ai_demo_notice),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(12.dp),
                ) {
                    Text(
                        text = aiSuggestion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                androidx.compose.material3.Button(
                    onClick = onInsert,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.chat_insert_in_message))
                }
            }
        }
    }
}

@Composable
internal fun CreatePollSheetContent(
    onDismiss: () -> Unit,
    onCreate: (question: String, options: List<String>, allowMultiple: Boolean) -> Unit,
) {
    var question by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(listOf("", "")) }
    var allowMultiple by remember { mutableStateOf(false) }

    val isValid = question.isNotBlank() && options.count { it.isNotBlank() } >= 2

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.chat_create_poll),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            label = { Text(stringResource(R.string.chat_question)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        options.forEachIndexed { index, option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = option,
                    onValueChange = { newValue ->
                        options = options.toMutableList().also { it[index] = newValue }
                    },
                    label = { Text(stringResource(R.string.chat_option_number, index + 1)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                if (options.size > 2) {
                    IconButton(
                        onClick = {
                            options = options.toMutableList().also { it.removeAt(index) }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.chat_remove_option),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        if (options.size < 10) {
            TextButton(onClick = { options = options + "" }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.chat_add_option))
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.chat_poll_allow_multiple),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = allowMultiple, onCheckedChange = { allowMultiple = it })
        }

        androidx.compose.material3.Button(
            onClick = { onCreate(question, options.filter { it.isNotBlank() }, allowMultiple); onDismiss() },
            enabled = isValid,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.chat_create_poll)) }
    }
}

@Composable
internal fun ReactionDetailsSheet(
    reactions: List<com.ajrpachon.chatapp.domain.model.ReactionBO>,
    onDismiss: () -> Unit,
) {
    val grouped = reactions.groupBy { it.emoji }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding(),
    ) {
        Text(
            text = stringResource(R.string.chat_reactions_count, reactions.size),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        grouped.forEach { (emoji, reactors) ->
            reactors.forEach { reaction ->
                ListItem(
                    headlineContent = { Text(reaction.userId) },
                    trailingContent = { Text(emoji, style = MaterialTheme.typography.titleLarge) },
                )
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WallpaperPickerSheet(
    currentColor: Long?,
    onSelect: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = listOf(
        null to stringResource(R.string.chat_wallpaper_default),
        0xFFE3F2FDL to stringResource(R.string.chat_wallpaper_light_blue),
        0xFFF3E5F5L to stringResource(R.string.chat_wallpaper_purple),
        0xFFE8F5E9L to stringResource(R.string.chat_wallpaper_green),
        0xFFFFF8E1L to stringResource(R.string.chat_wallpaper_yellow),
        0xFFFCE4ECL to stringResource(R.string.chat_wallpaper_pink),
        0xFF212121L to stringResource(R.string.chat_wallpaper_dark),
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Text(
            stringResource(R.string.chat_wallpaper),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(colors, key = { it.first?.toString() ?: "default" }) { (colorValue, label) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onSelect(colorValue) },
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                colorValue?.let { androidx.compose.ui.graphics.Color(it) }
                                    ?: MaterialTheme.colorScheme.background
                            )
                            .border(
                                width = if (currentColor == colorValue) 3.dp else 1.dp,
                                color = if (currentColor == colorValue) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                shape = CircleShape,
                            )
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
