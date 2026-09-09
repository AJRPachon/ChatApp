package com.ajrpachon.chatapp.domain.model

import kotlinx.datetime.Instant

data class InvitationBO(
    val id: String,
    val sender: UserBO,
    val receiverId: String,
    val status: InvitationStatus,
    val createdAt: Instant,
    // Only populated for invitations *I* sent (Invitations screen's "Sent" tab) — the profile
    // of the person I invited. Null for received invitations, where [sender] already is the
    // other party.
    val receiver: UserBO? = null,
)
