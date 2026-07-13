package com.ajrpachon.chatapp.domain.model

data class SessionBO(
    val id: String,
    val deviceInfo: String,
    val createdAt: Long,
    val lastActiveAt: Long,
    val isCurrent: Boolean,
)
