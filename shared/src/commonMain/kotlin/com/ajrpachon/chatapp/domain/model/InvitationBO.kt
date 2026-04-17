package com.ajrpachon.chatapp.domain.model

import kotlinx.datetime.Instant

enum class InvitationStatus { PENDING, ACCEPTED, REJECTED }

data class InvitationBO(
    val id: String,
    val sender: UserBO,
    val receiverId: String,
    val status: InvitationStatus,
    val createdAt: Instant,
)
