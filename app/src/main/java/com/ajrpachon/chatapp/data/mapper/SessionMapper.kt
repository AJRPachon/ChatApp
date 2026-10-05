package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.local.entity.SessionDBO
import com.ajrpachon.chatapp.domain.model.SessionBO

fun SessionDBO.toBO() = SessionBO(
    id = id,
    deviceInfo = deviceInfo,
    createdAt = createdAt,
    lastActiveAt = lastActiveAt,
    isCurrent = isCurrent,
)

fun SessionBO.toDBO() = SessionDBO(
    id = id,
    deviceInfo = deviceInfo,
    createdAt = createdAt,
    lastActiveAt = lastActiveAt,
    isCurrent = isCurrent,
)
