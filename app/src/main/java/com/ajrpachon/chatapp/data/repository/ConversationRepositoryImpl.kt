package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.local.dao.ConversationDao
import com.ajrpachon.chatapp.data.local.dao.GroupMemberDao
import com.ajrpachon.chatapp.data.local.dao.MessageDao
import com.ajrpachon.chatapp.data.local.dao.UserDao
import com.ajrpachon.chatapp.data.local.entity.ConversationDBO
import com.ajrpachon.chatapp.data.mapper.toDBO
import com.ajrpachon.chatapp.data.mapper.toBO
import com.ajrpachon.chatapp.data.remote.source.ConversationRemoteSource
import com.ajrpachon.chatapp.data.remote.source.MessageRemoteSource
import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.catchResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

private const val TAG = "ConvRepo"

class ConversationRepositoryImpl(
    private val conversationDao: ConversationDao,
    private val userDao: UserDao,
    private val messageDao: MessageDao,
    private val groupMemberDao: GroupMemberDao,
    private val messageRemoteSource: MessageRemoteSource,
    private val conversationRemoteSource: ConversationRemoteSource,
) : ConversationRepository {

    private val syncMutex = Mutex()

    override fun observeConversations(userId: String): Flow<List<ConversationBO>> = channelFlow {
        launch { catchResult { syncConversations(userId) } }

        launch {
            while (isActive) {
                delay(60_000)
                AppLogger.d(TAG, "periodic resync userId=$userId")
                catchResult { syncConversations(userId) }
            }
        }

        catchResult { conversationRemoteSource.ensureSession() }

        launch {
            conversationRemoteSource.observeParticipantInserts(userId).collect { record ->
                val addedUserId = record["user_id"]?.jsonPrimitive?.contentOrNull
                if (addedUserId == userId) catchResult { syncConversations(userId) }
            }
        }

        launch {
            conversationRemoteSource.observeNewMessageInserts(userId).collect { messageDto ->
                catchResult {
                    messageDao.upsert(messageDto.toDBO())
                    val existingConversation = conversationDao.getById(messageDto.conversationId)
                    if (existingConversation != null) {
                        val newUnread = if (messageDto.senderId != userId)
                            existingConversation.unreadCount + 1
                        else
                            existingConversation.unreadCount
                        conversationDao.upsert(
                            existingConversation.copy(
                                updatedAt = System.currentTimeMillis(),
                                unreadCount = newUnread,
                            )
                        )
                    } else {
                        syncConversations(userId)
                    }
                }
            }
        }

        launch {
            AppLogger.d(TAG, "conversationsUpdateChannel subscribing userId=$userId")
            conversationRemoteSource.observeConversationUpdates(userId).collect {
                AppLogger.d(TAG, "conversationsUpdateChannel UPDATE received userId=$userId")
                catchResult { syncConversations(userId) }
                    .onFailure { e -> AppLogger.e(TAG, "syncConversations failed after conversations UPDATE", e) }
            }
        }

        launch {
            conversationRemoteSource.observeProfileUpdates(userId).collect { record ->
                catchResult {
                    val profileId = record["id"]?.jsonPrimitive?.contentOrNull ?: return@catchResult
                    val existing = userDao.getById(profileId) ?: return@catchResult
                    val newAvatarUrl = record["avatar_url"]?.jsonPrimitive?.contentOrNull
                    val newDisplayName = record["display_name"]?.jsonPrimitive?.contentOrNull
                    val newUsername = record["username"]?.jsonPrimitive?.contentOrNull
                    userDao.upsert(
                        existing.copy(
                            avatarUrl = newAvatarUrl,
                            displayName = newDisplayName ?: existing.displayName,
                            username = newUsername ?: existing.username,
                        )
                    )
                    conversationDao.getByOtherUserId(profileId)?.let { conv ->
                        conversationDao.upsert(
                            conv.copy(
                                name = newUsername?.takeIf { it.isNotBlank() }
                                    ?: newDisplayName?.takeIf { it.isNotBlank() }
                                    ?: conv.name,
                            )
                        )
                    }
                }
            }
        }

        conversationDao.observeActive().map { dbos ->
            dbos.mapNotNull { dbo -> dbo.toBO(userId) }
        }.collect { send(it) }
    }

    override fun observeArchivedConversations(userId: String): Flow<List<ConversationBO>> =
        conversationDao.observeArchived().map { dbos -> dbos.mapNotNull { dbo -> dbo.toBO(userId) } }

    private suspend fun ConversationDBO.toBO(userId: String): ConversationBO? {
        val lastMsg = messageDao.getLastMessage(id)?.let { msgDbo ->
            val sender = userDao.getById(msgDbo.senderId)?.toBO()
            msgDbo.toBO(userId, sender?.displayName ?: msgDbo.senderId)
        }
        val trailingImages = messageDao.getTrailingImageCount(id)
        val otherUser = otherUserId?.let { userDao.getById(it) }
        return ConversationBO(
            id = id,
            name = name ?: "Chat",
            isGroup = isGroup,
            participants = emptyList(),
            lastMessage = lastMsg,
            unreadCount = unreadCount,
            updatedAt = Instant.fromEpochMilliseconds(updatedAt),
            trailingImageCount = trailingImages,
            otherUserAvatarUrl = otherUser?.avatarUrl,
            groupAvatarUrl = groupAvatarUrl,
            description = description,
            isMuted = isEffectivelyMuted(),
            mutedUntil = mutedUntil,
            isArchived = isArchived,
        )
    }

    override suspend fun getOrCreateDirectConversation(
        currentUserId: String,
        otherUserId: String,
    ): ConversationBO {
        val conversationId = conversationRemoteSource.getOrCreateDirectConversation(currentUserId, otherUserId)
        val nowMs = System.currentTimeMillis()
        val now = Instant.fromEpochMilliseconds(nowMs)

        val otherName = userDao.getById(otherUserId)?.let { dbo ->
            dbo.username.takeIf { it.isNotBlank() } ?: dbo.displayName.takeIf { it.isNotBlank() }
        } ?: catchResult {
            conversationRemoteSource.fetchUserProfile(otherUserId)
                ?.also { userDao.upsert(it.toDBO()) }
                ?.let { dto ->
                    dto.username?.takeIf { it.isNotBlank() } ?: dto.displayName.takeIf { it.isNotBlank() }
                }
        }.getOrNull()

        conversationDao.upsert(
            ConversationDBO(
                id = conversationId,
                name = otherName,
                isGroup = false,
                createdBy = currentUserId,
                updatedAt = now.toEpochMilliseconds(),
                otherUserId = otherUserId,
            )
        )
        return ConversationBO(
            id = conversationId,
            name = otherName ?: "Chat",
            isGroup = false,
            participants = emptyList(),
            lastMessage = null,
            unreadCount = 0,
            updatedAt = now,
        )
    }

    override suspend fun toggleMute(conversationId: String, muted: Boolean) {
        conversationDao.updateMuted(conversationId, muted)
    }

    override suspend fun muteFor(conversationId: String, mutedUntil: Long) {
        conversationDao.updateMutedUntil(conversationId, mutedUntil)
    }

    override suspend fun clearChat(conversationId: String) {
        messageDao.deleteByConversation(conversationId)
    }

    override suspend fun deleteConversation(conversationId: String) {
        messageDao.deleteByConversation(conversationId)
        groupMemberDao.deleteAllForConversation(conversationId)
        conversationDao.deleteById(conversationId)
    }

    override suspend fun archiveConversation(conversationId: String, archived: Boolean) {
        conversationDao.setArchived(conversationId, archived)
    }

    override suspend fun setDisappearingMode(conversationId: String, seconds: Long) {
        conversationDao.setDisappearingMode(conversationId, seconds)
    }

    override suspend fun syncConversations(userId: String) = syncMutex.withLock {
        val rows = conversationRemoteSource.fetchParticipantsWithConversations(userId)

        for (participantRow in rows) {
            val conversationDto = participantRow.conversation
            val existingConversation = conversationDao.getById(conversationDto.id)
            val historyVisibleFrom = catchResult {
                Instant.parse(participantRow.joinedAt).toEpochMilliseconds()
            }.getOrDefault(0L)
            AppLogger.d(
                TAG,
                "syncConv conv=${conversationDto.id} isGroup=${conversationDto.isGroup} " +
                    "avatarUrl=${conversationDto.avatarUrl} existingAvatarUrl=${existingConversation?.groupAvatarUrl}"
            )

            var resolvedOtherUserId: String? = null
            val resolvedName = if (!conversationDto.isGroup) {
                val otherUserId = catchResult {
                    conversationRemoteSource.fetchOtherParticipantId(conversationDto.id, userId)
                }.getOrNull()
                    ?: conversationDto.createdBy?.takeIf { it != userId }

                resolvedOtherUserId = otherUserId ?: existingConversation?.otherUserId

                if (otherUserId != null) {
                    val otherUserProfile = catchResult {
                        conversationRemoteSource.fetchUserProfile(otherUserId)
                            ?.also { userDao.upsert(it.toDBO()) }
                    }.getOrNull()
                    otherUserProfile?.username?.takeIf { it.isNotBlank() }
                        ?: otherUserProfile?.displayName?.takeIf { it.isNotBlank() }
                        ?: userDao.getById(otherUserId)?.username?.takeIf { it.isNotBlank() }
                        ?: userDao.getById(otherUserId)?.displayName?.takeIf { it.isNotBlank() }
                } else null
            } else conversationDto.name

            conversationDao.upsert(
                ConversationDBO(
                    id = conversationDto.id,
                    name = resolvedName ?: existingConversation?.name ?: conversationDto.name,
                    isGroup = conversationDto.isGroup,
                    createdBy = conversationDto.createdBy ?: userId,
                    updatedAt = catchResult { Instant.parse(conversationDto.updatedAt).toEpochMilliseconds() }
                        .getOrElse { System.currentTimeMillis() },
                    otherUserId = resolvedOtherUserId,
                    description = conversationDto.description ?: existingConversation?.description,
                    groupAvatarUrl = conversationDto.avatarUrl ?: existingConversation?.groupAvatarUrl,
                    historyVisibleFrom = historyVisibleFrom,
                    isArchived = existingConversation?.isArchived ?: false,
                )
            )

            catchResult {
                val lastMsg = messageRemoteSource.getLastMessage(conversationDto.id, historyVisibleFrom)
                if (lastMsg != null) messageDao.upsert(lastMsg.toDBO())
            }
        }
    }
}
