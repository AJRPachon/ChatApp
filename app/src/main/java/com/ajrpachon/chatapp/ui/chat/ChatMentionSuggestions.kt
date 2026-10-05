package com.ajrpachon.chatapp.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.domain.model.GroupMemberBO
import com.ajrpachon.chatapp.domain.model.GroupRole
import com.ajrpachon.chatapp.ui.components.ChatAppAvatar
import com.ajrpachon.chatapp.ui.theme.ChatAppTheme
import kotlinx.datetime.Instant

/**
 * `@mention` autocomplete list shown above the input bar in group chats while the user is typing
 * an `@query` (see [ChatState.mentionSuggestions]). Stateless: selection is reported through
 * [onSelect], which [ChatBottomBar] maps to `ChatIntent.SelectMention`.
 */
@Composable
internal fun MentionSuggestionList(
    suggestions: List<GroupMemberBO>,
    onSelect: (GroupMemberBO) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        HorizontalDivider()
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
            items(suggestions, key = { it.userId }) { member ->
                MentionSuggestionRow(member = member, onClick = { onSelect(member) })
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun MentionSuggestionRow(member: GroupMemberBO, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = stringResource(R.string.chat_mention_click_label), onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ChatAppAvatar(name = member.displayName, url = member.avatarUrl, size = 32.dp)
        Column {
            Text(
                text = member.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.chat_mention_username, member.username),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
internal fun MentionSuggestionListPreview() {
    fun member(id: String, name: String, username: String) = GroupMemberBO(
        userId = id,
        conversationId = "c1",
        displayName = name,
        username = username,
        avatarUrl = null,
        role = GroupRole.MEMBER,
        joinedAt = Instant.fromEpochMilliseconds(0),
    )
    ChatAppTheme {
        MentionSuggestionList(
            suggestions = listOf(member("1", "Ana García", "ana"), member("2", "Andrés López", "andres_l")),
            onSelect = {},
        )
    }
}
