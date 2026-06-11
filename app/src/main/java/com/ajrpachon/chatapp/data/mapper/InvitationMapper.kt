package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.local.entity.InvitationDBO
import com.ajrpachon.chatapp.data.remote.dto.InvitationDTO
import com.ajrpachon.chatapp.domain.model.InvitationBO
import com.ajrpachon.chatapp.domain.model.InvitationStatus
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

fun InvitationDBO.toBO(sender: com.ajrpachon.chatapp.domain.model.UserBO) = InvitationBO(
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
