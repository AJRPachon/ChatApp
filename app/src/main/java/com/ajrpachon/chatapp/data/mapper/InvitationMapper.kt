package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.local.entity.InvitationDBO
import com.ajrpachon.chatapp.data.remote.dto.InvitationDTO
import com.ajrpachon.chatapp.domain.model.InvitationBO
import com.ajrpachon.chatapp.domain.model.InvitationStatus
import com.ajrpachon.chatapp.domain.model.UserBO
import kotlinx.datetime.Instant

fun InvitationDTO.toDBO() = InvitationDBO(
    id = id,
    senderId = senderId,
    senderUsername = sender?.username ?: "",
    senderDisplayName = sender?.displayName ?: "",
    receiverId = receiverId,
    status = status,
    createdAt = Instant.parse(createdAt).toEpochMilliseconds(),
)

/** Direct DTO → BO mapping (no round-trip through DBO). Uses embedded sender from the DTO. */
fun InvitationDTO.toBO(): InvitationBO {
    val senderBO = sender?.toBO()
        ?: UserBO(
            id = senderId,
            email = "",
            username = "",
            displayName = senderId,
            avatarUrl = null,
            createdAt = Instant.fromEpochMilliseconds(0),
        )
    return InvitationBO(
        id = id,
        sender = senderBO,
        receiverId = receiverId,
        status = when (status) {
            "accepted" -> InvitationStatus.ACCEPTED
            "rejected" -> InvitationStatus.REJECTED
            else -> InvitationStatus.PENDING
        },
        createdAt = runCatching { Instant.parse(createdAt) }.getOrElse { Instant.fromEpochMilliseconds(0) },
    )
}

fun InvitationDBO.toBO(sender: UserBO) = InvitationBO(
    id = id,
    sender = sender,
    receiverId = receiverId,
    status = when (status) {
        "accepted" -> InvitationStatus.ACCEPTED
        "rejected" -> InvitationStatus.REJECTED
        else -> InvitationStatus.PENDING
    },
    createdAt = Instant.fromEpochMilliseconds(createdAt),
)
