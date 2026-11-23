package com.ajrpachon.chatapp.ui.preview

import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.model.GroupMemberBO
import com.ajrpachon.chatapp.domain.model.GroupRole
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.model.ReactionBO
import com.ajrpachon.chatapp.domain.model.ScheduledMessageBO
import kotlinx.datetime.Instant

/**
 * Extra fixtures for the chat input bar, top bar, search overlay, message list and dialog host
 * previews. Same rules as [PreviewData]: fixed ids and timestamps, nothing random.
 */
internal object PreviewChatInput {

    /** Newest first, like the real paging source. */
    val messages: List<MessageBO> = listOf(
        PreviewData.message("m4", "Perfecto, nos vemos allí", isFromMe = true),
        PreviewData.message("m3", "¿Quedamos a las 7?", isFromMe = false),
        PreviewData.message("m2", "Voy a llegar un poco tarde", isFromMe = true),
        PreviewData.message("m1", "Hola, ¿qué tal?", isFromMe = false),
    )

    val searchResults: List<MessageBO> = listOf(
        PreviewData.message("s2", "¿Quedamos a las 7?", isFromMe = false),
        PreviewData.message("s1", "Mañana quedamos para comer", isFromMe = true),
    )

    val reactions: List<ReactionBO> = listOf(
        ReactionBO(messageId = "m4", userId = PreviewData.OTHER_ID, emoji = "👍"),
        ReactionBO(messageId = "m4", userId = PreviewData.ME_ID, emoji = "❤️"),
    )

    val members: List<GroupMemberBO> = listOf(
        member("u1", "Ana García", "ana"),
        member("u2", "Andrés López", "andres_l"),
    )

    val conversations: List<ConversationBO> = listOf(
        conversation("c2", "Bruno Pérez"),
        conversation("c3", "Equipo Android"),
    )

    val scheduledMessages: List<ScheduledMessageBO> = listOf(
        ScheduledMessageBO(
            id = "sch1",
            conversationId = PreviewData.CONVERSATION_ID,
            senderId = PreviewData.ME_ID,
            text = "Feliz cumpleaños",
            scheduledAtMs = 86_400_000L,
            createdAt = 0L,
        ),
    )

    private fun member(id: String, name: String, username: String) = GroupMemberBO(
        userId = id,
        conversationId = PreviewData.CONVERSATION_ID,
        displayName = name,
        username = username,
        avatarUrl = null,
        role = GroupRole.MEMBER,
        joinedAt = Instant.fromEpochMilliseconds(0L),
    )

    private fun conversation(id: String, name: String) = ConversationBO(
        id = id,
        name = name,
        isGroup = false,
        participants = emptyList(),
        lastMessage = null,
        unreadCount = 0,
        updatedAt = Instant.fromEpochMilliseconds(0L),
    )
}
