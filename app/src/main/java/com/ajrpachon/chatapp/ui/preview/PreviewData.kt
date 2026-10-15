package com.ajrpachon.chatapp.ui.preview

import com.ajrpachon.chatapp.domain.model.MessageBO
import kotlinx.datetime.Instant

/**
 * Deterministic fixtures for previews: fixed ids and timestamps, no `Clock`, `UUID` or `Random`,
 * so a preview always renders the same. Add new fixtures here (or in a `Preview<Area>.kt` next
 * to it if this object grows) instead of declaring private copies in every screen file.
 */
internal object PreviewData {

    const val CONVERSATION_ID = "c1"
    const val ME_ID = "me"
    const val OTHER_ID = "other"
    const val OTHER_NAME = "Ana García"

    fun message(
        id: String,
        content: String,
        isFromMe: Boolean,
        isRead: Boolean = true,
    ) = MessageBO(
        id = id,
        conversationId = CONVERSATION_ID,
        senderId = if (isFromMe) ME_ID else OTHER_ID,
        senderName = if (isFromMe) "Yo" else OTHER_NAME,
        content = content,
        isRead = isRead,
        isFromMe = isFromMe,
        createdAt = Instant.fromEpochMilliseconds(0L),
    )
}
