package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.local.dao.ConversationDao
import com.ajrpachon.chatapp.data.local.dao.GroupMemberDao
import com.ajrpachon.chatapp.data.local.dao.MessageDao
import com.ajrpachon.chatapp.data.local.dao.UserDao
import com.ajrpachon.chatapp.data.local.entity.ConversationDBO
import com.ajrpachon.chatapp.data.mapper.toBO
import com.ajrpachon.chatapp.data.mapper.toDBO
import com.ajrpachon.chatapp.data.remote.source.ConversationRemoteSource
import com.ajrpachon.chatapp.data.remote.source.MessageRemoteSource
import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.domain.util.catchResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
    private val remoteSource: ConversationRemoteSource,
) : ConversationRepository {

    private val syncMutex = Mutex()

    override fun observeConversations(userId: String): Flow<List<ConversationBO>> = channelFlow {
        launch { catchResult { syncConversations(userId) } }

        // Periodic resync so group avatar / name changes are never missed if Realtime is delayed
        launch {
            while (isActive) {
                delay(60_000)
                AppLogger.d(TAG, "periodic resync userId=$userId")
                catchResult { syncConversations(userId) }
            }
        }

        // Ensure user JWT is loaded so Realtime subscriptions are authenticated
        catchResult { remoteSource.currentUserId() }

        // New participant rows: the user was added to a conversation (e.g. a group)
        launch {
            remoteSource.observeParticipantInserts(userId).collect { record ->
                val addedUserId = record["user_id"]?.jsonPrimitive?.contentOrNull
                if (addedUserId == userId) catchResult { syncConversations(userId) }
            }
        }

        // New messages: keep the local copy and the unread counter up to date
        launch {
            remoteSource.observeNewMessageInserts(userId).collect { messageDto ->
                catchResult {
                    messageDao.upsert(messageDto.toDBO())
                    val existingConversation = conversationDao.getById(messageDto.conversationId)
                    if (existingConversation != null) {
                        val newUnread = if (messageDto.senderId != userId)
                            existingConversation.unreadCount + 1
                        else
                            existingConversation.unreadCount
                        conversationDao.upsert(existingConversation.copy(
                            updatedAt = System.currentTimeMillis(),
                            unreadCount = newUnread,
                        ))
                    } else {
                        syncConversations(userId)
                    }
                }
            }
        }

        // Group avatar / name / description changes — conversations UPDATE
        launch {
            remoteSource.observeConversationUpdates(userId).collect { record ->
                AppLogger.d(TAG, "conversationsUpdateChannel UPDATE received userId=$userId record=$record")
                catchResult { syncConversations(userId) }
                    .onFailure { e -> AppLogger.e(TAG, "syncConversations failed after conversations UPDATE", e) }
            }
        }

        // Individual user avatar / name changes — profiles UPDATE
        launch {
            remoteSource.observeProfileUpdates(userId).collect { record ->
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
                    // Touch the DM conversation so observeAll() re-emits with the new avatar
                    conversationDao.getByOtherUserId(profileId)?.let { conv ->
                        conversationDao.upsert(conv.copy(
                            name = newUsername?.takeIf { it.isNotBlank() }
                                ?: newDisplayName?.takeIf { it.isNotBlank() }
                                ?: conv.name,
                        ))
                    }
                }
            }
        }

        conversationDao.observeActive().map { dbos ->
            dbos.mapNotNull { dbo -> assembleConversation(dbo, userId) }
        }.collect { send(it) }
    }

    override suspend fun getLocalConversations(userId: String): List<ConversationBO> =
        conversationDao.observeActive().first().mapNotNull { dbo -> assembleConversation(dbo, userId) }

    override fun observeArchivedConversations(userId: String): Flow<List<ConversationBO>> =
        conversationDao.observeArchived().map { dbos -> dbos.mapNotNull { dbo -> assembleConversation(dbo, userId) } }

    /**
     * Builds a [ConversationBO] from a stored row. Not a pure mapper: it also reads the last
     * message, the trailing image count and the other participant from the local database.
     */
    private suspend fun assembleConversation(dbo: ConversationDBO, userId: String): ConversationBO? {
        val lastMsg = messageDao.getLastMessage(dbo.id)?.let { msgDbo ->
            val sender = userDao.getById(msgDbo.senderId)?.toBO()
            msgDbo.toBO(userId, sender?.displayName ?: msgDbo.senderId)
        }
        val trailingImages = messageDao.getTrailingImageCount(dbo.id)
        val otherUser = dbo.otherUserId?.let { userDao.getById(it) }
        return dbo.toBO(lastMsg, trailingImages, otherUser?.avatarUrl)
    }

    override suspend fun getOrCreateDirectConversation(
        currentUserId: String,
        otherUserId: String,
    ): ConversationBO {
        val conversationId = remoteSource.getOrCreateDirectConversation(currentUserId, otherUserId)
        val nowMs = System.currentTimeMillis()
        val now = Instant.fromEpochMilliseconds(nowMs)

        // Prefer username over displayName; fetch from remote if not cached
        val otherName = userDao.getById(otherUserId)?.let { dbo ->
            dbo.username.takeIf { it.isNotBlank() } ?: dbo.displayName.takeIf { it.isNotBlank() }
        } ?: catchResult {
            remoteSource.fetchUserProfile(otherUserId)
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
        val rows = remoteSource.fetchParticipantsWithConversations(userId)

        for (participantRow in rows) {
            val conversationDto = participantRow.conversation
            val existingConversation = conversationDao.getById(conversationDto.id)
            val historyVisibleFrom = catchResult {
                Instant.parse(participantRow.joinedAt).toEpochMilliseconds()
            }.getOrDefault(0L)
            AppLogger.d(TAG, "syncConv conv=${conversationDto.id} isGroup=${conversationDto.isGroup} avatarUrl=${conversationDto.avatarUrl} existingAvatarUrl=${existingConversation?.groupAvatarUrl}")

            var resolvedOtherUserId: String? = null
            val resolvedName = if (!conversationDto.isGroup) {
                val otherUserId = catchResult {
                    remoteSource.fetchOtherParticipantId(conversationDto.id, excludeUserId = userId)
                }.getOrNull()
                    ?: conversationDto.createdBy?.takeIf { it != userId }

                resolvedOtherUserId = otherUserId ?: existingConversation?.otherUserId

                if (otherUserId != null) {
                    val otherUserProfile = catchResult {
                        remoteSource.fetchUserProfile(otherUserId)
                            ?.also { userDao.upsert(it.toDBO()) }
                    }.getOrNull()
                    otherUserProfile?.username?.takeIf { it.isNotBlank() }
                        ?: otherUserProfile?.displayName?.takeIf { it.isNotBlank() }
                        ?: userDao.getById(otherUserId)?.username?.takeIf { it.isNotBlank() }
                        ?: userDao.getById(otherUserId)?.displayName?.takeIf { it.isNotBlank() }
                } else null
            } else conversationDto.name

            conversationDao.upsert(
                conversationDto.toDBO(
                    createdBy = conversationDto.createdBy ?: userId,
                    resolvedName = resolvedName,
                    resolvedOtherUserId = resolvedOtherUserId,
                    historyVisibleFrom = historyVisibleFrom,
                    existing = existingConversation,
                )
            )

            // Fetch the last message so the conversation list can show a preview
            // without requiring the user to open each chat first.
            catchResult {
                val lastMsg = messageRemoteSource.getLastMessage(conversationDto.id, historyVisibleFrom)
                if (lastMsg != null) messageDao.upsert(lastMsg.toDBO())
            }
        }
    }

    override suspend fun getById(conversationId: String): ConversationBO? {
        val dbo = conversationDao.getById(conversationId) ?: return null
        val userId = remoteSource.currentUserId() ?: return null
        return assembleConversation(dbo, userId)
    }

    override fun observeById(conversationId: String): Flow<ConversationBO?> {
        val userId = remoteSource.currentUserId() ?: return flowOf(null)
        return conversationDao.observeById(conversationId).map { it?.let { dbo -> assembleConversation(dbo, userId) } }
    }

    override suspend fun resetUnreadCount(conversationId: String) {
        conversationDao.resetUnreadCount(conversationId)
    }
}
