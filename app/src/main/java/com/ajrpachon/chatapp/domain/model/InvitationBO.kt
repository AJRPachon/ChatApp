package com.ajrpachon.chatapp.domain.model

import kotlinx.datetime.Instant

data class InvitationBO(
    val id: String,
    val sender: UserBO,
    val receiverId: String,
    val status: InvitationStatus,
    val createdAt: Instant,
)
