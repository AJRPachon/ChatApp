package com.ajrpachon.chatapp.ui.preview

import com.ajrpachon.chatapp.domain.model.CallBO
import com.ajrpachon.chatapp.domain.model.CallStatus
import com.ajrpachon.chatapp.domain.model.CallType
import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.model.GroupMemberBO
import com.ajrpachon.chatapp.domain.model.GroupRole
import com.ajrpachon.chatapp.domain.model.InvitationBO
import com.ajrpachon.chatapp.domain.model.InvitationStatus
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.ui.profile.SessionInfo
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

    private val EPOCH = Instant.fromEpochMilliseconds(0L)

    private const val SESSION_NOW_MS = 1_760_000_000_000L
    private const val ONE_DAY_MS = 86_400_000L
    private const val ONE_HOUR_MS = 3_600_000L

    fun user(id: String, name: String) = UserBO(
        id = id,
        email = "",
        username = name.lowercase().replace(" ", "_"),
        displayName = name,
        avatarUrl = null,
        createdAt = EPOCH,
    )

    fun conversation(id: String, name: String, isGroup: Boolean = false, unread: Int = 0) = ConversationBO(
        id = id,
        name = name,
        isGroup = isGroup,
        participants = emptyList(),
        lastMessage = null,
        unreadCount = unread,
        updatedAt = EPOCH,
    )

    fun invitation(
        id: String,
        from: String,
        to: String? = null,
        status: InvitationStatus = InvitationStatus.PENDING,
    ) = InvitationBO(
        id = id,
        sender = user("s$id", from),
        receiverId = ME_ID,
        status = status,
        createdAt = EPOCH,
        receiver = to?.let { user("r$id", it) },
    )

    fun groupMember(id: String, name: String, role: GroupRole) = GroupMemberBO(
        userId = id,
        conversationId = CONVERSATION_ID,
        displayName = name,
        username = name.lowercase().replace(" ", "_"),
        avatarUrl = null,
        role = role,
        joinedAt = EPOCH,
    )

    fun incomingCall(type: CallType) = CallBO(
        id = "call1",
        conversationId = CONVERSATION_ID,
        callerId = "u1",
        callerName = OTHER_NAME,
        calleeId = ME_ID,
        type = type,
        status = CallStatus.RINGING,
        roomName = "room1",
    )

    fun session(id: String, device: String, current: Boolean) = SessionInfo(
        id = id,
        deviceInfo = device,
        createdAt = SESSION_NOW_MS - ONE_DAY_MS,
        lastActiveAt = SESSION_NOW_MS - ONE_HOUR_MS,
        isCurrent = current,
    )
}
