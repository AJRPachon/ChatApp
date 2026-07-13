package com.ajrpachon.chatapp.domain.model

data class BroadcastListBO(
    val id: String,
    val name: String,
    val createdAt: Long,
    val members: List<UserBO> = emptyList(),
)
