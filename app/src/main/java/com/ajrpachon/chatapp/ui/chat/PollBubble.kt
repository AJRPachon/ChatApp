package com.ajrpachon.chatapp.ui.chat

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.domain.model.PollOptionBO

// ── Poll bubble ───────────────────────────────────────────────────────────────
// Extracted per docs/chat-viewmodel-decomposition.md Phase 2 — part of the message-bubble
// rendering tree, rendered from MessageBubble.kt via ChatBubbleSlot (defined there).

@Composable
internal fun PollBubble(
    pollId: String,
    isFromMe: Boolean,
    pollUiState: PollUiState?,
    onVote: (optionId: String) -> Unit,
    onObserve: (String) -> Unit,
) {
    LaunchedEffect(pollId) { onObserve(pollId) }
    val poll = pollUiState?.poll
    val options = pollUiState?.options ?: emptyList()
    val userVotes = pollUiState?.userVotes ?: emptyList()

    ChatBubbleSlot(isFromMe = isFromMe, modifier = Modifier.padding(vertical = 4.dp)) { maxBubbleWidth ->
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(min = 220.dp, max = minOf(300.dp, maxBubbleWidth)),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.HowToVote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.chat_poll),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                if (poll == null) {
                    Text(
                        text = stringResource(R.string.chat_loading_poll),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                } else {
                    // Question
                    val safePoll = poll ?: return@Column
                    Text(
                        text = safePoll.question,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    val totalVotes = options.sumOf { it.voteCount }.coerceAtLeast(1)

                    // Options
                    val allowMultiple = safePoll.allowMultiple
                    options.forEach { option ->
                        PollOptionRow(
                            option = option,
                            isSelected = userVotes.any { it.optionId == option.id },
                            allowMultiple = allowMultiple,
                            totalVotes = totalVotes,
                            onVote = { onVote(option.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PollOptionRow(
    option: PollOptionBO,
    isSelected: Boolean,
    allowMultiple: Boolean,
    totalVotes: Int,
    onVote: () -> Unit,
) {
    val fraction = option.voteCount.toFloat() / totalVotes.toFloat()
    // Animate width instead of snapping so re-votes read as a smooth transition.
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 300),
        label = "pollOptionFraction",
    )
    Column(modifier = Modifier.padding(bottom = 6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (allowMultiple) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onVote() },
                    modifier = Modifier.size(20.dp),
                )
            } else {
                RadioButton(
                    selected = isSelected,
                    onClick = onVote,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = option.text,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "${option.voteCount}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .padding(start = 26.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
