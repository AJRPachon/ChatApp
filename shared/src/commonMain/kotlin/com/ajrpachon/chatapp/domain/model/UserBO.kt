package com.ajrpachon.chatapp.domain.model

import kotlinx.datetime.Instant

data class UserBO(
    val id: String,
    val email: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val createdAt: Instant,
)
