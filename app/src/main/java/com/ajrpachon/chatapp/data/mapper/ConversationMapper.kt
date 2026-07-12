package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.local.entity.ConversationDBO
import com.ajrpachon.chatapp.data.remote.dto.ConversationDTO
import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.model.MessageBO
import kotlinx.datetime.Instant

/**
 * Maps a [ConversationDTO] to a [ConversationDBO].
 *
 * Because the DTO only carries raw server data, callers must supply the extra
 * values that the repository resolves before persisting (resolved name, other-user
 * id, history cursor, and the previously cached row so mute/archive state is
 * preserved).
 */
fun ConversationDTO.toDBO(
    createdBy: String = this.createdBy ?: "",
    resolvedName: String? = null,
    resolvedOtherUserId: String? = null,
    historyVisibleFrom: Long = 0L,
    existing: ConversationDBO? = null,
): ConversationDBO = ConversationDBO(
    id = id,
    name = resolvedName ?: existing?.name ?: name,
    isGroup = isGroup,
    createdBy = createdBy,
    updatedAt = runCatching { Instant.parse(updatedAt).toEpochMilliseconds() }
        .getOrElse { System.currentTimeMillis() },
    otherUserId = resolvedOtherUserId,
    description = description ?: existing?.description,
    groupAvatarUrl = avatarUrl ?: existing?.groupAvatarUrl,
    historyVisibleFrom = historyVisibleFrom,
    isArchived = existing?.isArchived ?: false,
    unreadCount = existing?.unreadCount ?: 0,
    isMuted = existing?.isMuted ?: false,
    mutedUntil = existing?.mutedUntil ?: 0L,
    disappearingModeSeconds = existing?.disappearingModeSeconds ?: 0L,
)

/**
 * Maps a [ConversationDBO] to a [ConversationBO].
 *
 * All values that require DB queries (last message, trailing image count, other-user
 * avatar) must be resolved by the repository before calling this function.
 */
fun ConversationDBO.toBO(
    lastMessage: MessageBO? = null,
    trailingImageCount: Int = 0,
    otherUserAvatarUrl: String? = null,
): ConversationBO = ConversationBO(
    id = id,
    name = name ?: "Chat",
    isGroup = isGroup,
    participants = emptyList(),
    lastMessage = lastMessage,
    unreadCount = unreadCount,
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
    trailingImageCount = trailingImageCount,
    otherUserAvatarUrl = otherUserAvatarUrl,
    groupAvatarUrl = groupAvatarUrl,
    description = description,
    isMuted = isEffectivelyMuted(),
    mutedUntil = mutedUntil,
    isArchived = isArchived,
    otherUserId = otherUserId,
    historyVisibleFrom = historyVisibleFrom,
    disappearingModeSeconds = disappearingModeSeconds,
)
