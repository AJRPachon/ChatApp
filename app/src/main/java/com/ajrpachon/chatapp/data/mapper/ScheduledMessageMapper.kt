package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.local.entity.ScheduledMessageDBO
import com.ajrpachon.chatapp.domain.model.ScheduledMessageBO

fun ScheduledMessageDBO.toDomain(): ScheduledMessageBO = ScheduledMessageBO(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    text = text,
    scheduledAtMs = scheduledAtMs,
    createdAt = createdAt,
)
