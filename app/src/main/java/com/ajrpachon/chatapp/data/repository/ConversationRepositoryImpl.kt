package com.ajrpachon.chatapp.data.repository
import com.ajrpachon.chatapp.utils.catchResult
import com.ajrpachon.chatapp.utils.AppLogger


import com.ajrpachon.chatapp.data.local.dao.ConversationDao
import com.ajrpachon.chatapp.data.local.dao.GroupMemberDao
import com.ajrpachon.chatapp.data.local.dao.MessageDao
import com.ajrpachon.chatapp.data.local.dao.UserDao
import com.ajrpachon.chatapp.data.local.entity.ConversationDBO
import com.ajrpachon.chatapp.data.mapper.toDBO
import com.ajrpachon.chatapp.data.mapper.toBO
import com.ajrpachon.chatapp.data.remote.dto.ConversationParticipantWithConvDTO
import com.ajrpachon.chatapp.data.remote.dto.MessageDTO
import com.ajrpachon.chatapp.data.remote.dto.UserDTO
import com.ajrpachon.chatapp.data.remote.source.MessageRemoteSource
import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
private const val TAG = "ConvRepo"

private val lenientJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class ParticipantUserIdDTO(@SerialName("user_id") val userId: String)

class ConversationRepositoryImpl(
    private val conversationDao: ConversationDao,
    private val userDao: UserDao,
    private val messageDao: MessageDao,
    private val groupMemberDao: GroupMemberDao,
    private val messageRemoteSource: MessageRemoteSource,
    private val supabase: SupabaseClient,
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
        catchResult { supabase.auth.currentSessionOrNull() }

        val participantsChannel = supabase.channel("participants:$userId")
        val messagesChannel = supabase.channel("messages:list:$userId")

        launch {
            val flow = participantsChannel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "conversation_participants"
            }
            participantsChannel.subscribe()
            try {
                flow.collect { action ->
                    val addedUserId = action.record["user_id"]?.jsonPrimitive?.contentOrNull
                    if (addedUserId == userId) catchResult { syncConversations(userId) }
                }
            } finally {
                withContext(NonCancellable) {
                    catchResult { participantsChannel.unsubscribe() }
                    catchResult { supabase.realtime.removeChannel(participantsChannel) }
                }
            }
        }

        launch {
            val flow = messagesChannel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "messages"
            }
            messagesChannel.subscribe()
            try {
                flow.collect { action ->
                    catchResult {
                        val messageDto = lenientJson.decodeFromString<MessageDTO>(action.record.toString())
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
            } finally {
                withContext(NonCancellable) {
                    catchResult { messagesChannel.unsubscribe() }
                    catchResult { supabase.realtime.removeChannel(messagesChannel) }
                }
            }
        }

        // Group avatar / name / description changes — conversations UPDATE
        val conversationsUpdateChannel = supabase.channel("conversations:updates:$userId-${System.nanoTime()}")
        launch {
            val flow = conversationsUpdateChannel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                table = "conversations"
            }
            AppLogger.d(TAG, "conversationsUpdateChannel subscribing userId=$userId")
            conversationsUpdateChannel.subscribe()
            AppLogger.d(TAG, "conversationsUpdateChannel subscribed userId=$userId")
            try {
                flow.collect { action ->
                    AppLogger.d(TAG, "conversationsUpdateChannel UPDATE received userId=$userId record=${action.record}")
                    catchResult { syncConversations(userId) }
                        .onFailure { e -> AppLogger.e(TAG, "syncConversations failed after conversations UPDATE", e) }
                }
            } finally {
                withContext(NonCancellable) {
                    catchResult { conversationsUpdateChannel.unsubscribe() }
                    catchResult { supabase.realtime.removeChannel(conversationsUpdateChannel) }
                }
            }
        }

        // Individual user avatar / name changes — profiles UPDATE
        val profilesChannel = supabase.channel("profiles:updates:$userId-${System.nanoTime()}")
        launch {
            val flow = profilesChannel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                table = "profiles"
            }
            profilesChannel.subscribe()
            try {
                flow.collect { action ->
                    catchResult {
                        val profileId = action.record["id"]?.jsonPrimitive?.contentOrNull ?: return@catchResult
                        val existing = userDao.getById(profileId) ?: return@catchResult
                        val newAvatarUrl = action.record["avatar_url"]?.jsonPrimitive?.contentOrNull
                        val newDisplayName = action.record["display_name"]?.jsonPrimitive?.contentOrNull
                        val newUsername = action.record["username"]?.jsonPrimitive?.contentOrNull
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
            } finally {
                withContext(NonCancellable) {
                    catchResult { profilesChannel.unsubscribe() }
                    catchResult { supabase.realtime.removeChannel(profilesChannel) }
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
        val result = supabase.postgrest.rpc(
            "get_or_create_direct_conversation",
            buildJsonObject {
                put("user_a", currentUserId)
                put("user_b", otherUserId)
            },
        )
        val conversationId = Json.decodeFromString<String>(result.data)
        val nowMs = System.currentTimeMillis()
        val now = Instant.fromEpochMilliseconds(nowMs)

        // Prefer username over displayName; fetch from remote if not cached
        val otherName = userDao.getById(otherUserId)?.let { dbo ->
            dbo.username.takeIf { it.isNotBlank() } ?: dbo.displayName.takeIf { it.isNotBlank() }
        } ?: catchResult {
            supabase.postgrest["profiles"]
                .select { filter { eq("id", otherUserId) } }
                .decodeSingleOrNull<UserDTO>()
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

    override suspend fun syncConversations(userId: String) = syncMutex.withLock {
        val rows = supabase.postgrest["conversation_participants"]
            .select(Columns.raw("conversation_id, joined_at, conversations(id,name,is_group,created_by,updated_at,avatar_url,description)")) {
                filter { eq("user_id", userId) }
            }
            .decodeList<ConversationParticipantWithConvDTO>()

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
                    supabase.postgrest["conversation_participants"]
                        .select(Columns.list("user_id")) {
                            filter {
                                eq("conversation_id", conversationDto.id)
                                neq("user_id", userId)
                            }
                        }
                        .decodeList<ParticipantUserIdDTO>()
                        .firstOrNull()?.userId
                }.getOrNull()
                    ?: conversationDto.createdBy?.takeIf { it != userId }

                resolvedOtherUserId = otherUserId ?: existingConversation?.otherUserId

                if (otherUserId != null) {
                    val otherUserProfile = catchResult {
                        supabase.postgrest["profiles"]
                            .select { filter { eq("id", otherUserId) } }
                            .decodeSingleOrNull<UserDTO>()
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

            // Fetch the last message so the conversation list can show a preview
            // without requiring the user to open each chat first.
            catchResult {
                val lastMsg = messageRemoteSource.getLastMessage(conversationDto.id, historyVisibleFrom)
                if (lastMsg != null) messageDao.upsert(lastMsg.toDBO())
            }
        }
    }
}
