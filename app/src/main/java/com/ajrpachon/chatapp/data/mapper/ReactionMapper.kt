package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.local.entity.ReactionDBO
import com.ajrpachon.chatapp.data.remote.dto.ReactionRemoteDTO
import com.ajrpachon.chatapp.domain.model.ReactionBO

fun ReactionRemoteDTO.toBO() = ReactionBO(
    messageId = messageId,
    userId = userId,
    emoji = emoji,
)

fun ReactionDBO.toBO() = ReactionBO(
    messageId = messageId,
    userId = userId,
    emoji = emoji,
)
