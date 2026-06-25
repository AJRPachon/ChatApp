package com.ajrpachon.chatapp.data.repository
import com.ajrpachon.chatapp.utils.catchResult

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.ajrpachon.chatapp.data.local.dao.MessageDao
import com.ajrpachon.chatapp.data.local.dao.UserDao
import com.ajrpachon.chatapp.data.mapper.toDBO
import com.ajrpachon.chatapp.data.mapper.toBO
import com.ajrpachon.chatapp.data.remote.dto.MessageDTO
import com.ajrpachon.chatapp.data.remote.source.MessageRemoteSource
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.ajrpachon.chatapp.utils.UploadLimits.checkAudioSize
import com.ajrpachon.chatapp.utils.UploadLimits.checkImageSize
import kotlinx.datetime.Instant

private const val BUCKET = "chat-images"
private const val AUDIO_BUCKET = "chat-audio"

class MessageRepositoryImpl(
    private val messageDao: MessageDao,
    private val userDao: UserDao,
    private val remoteSource: MessageRemoteSource,
    private val supabase: SupabaseClient,
) : MessageRepository {

    override fun observeMessages(conversationId: String, currentUserId: String, historyVisibleFrom: Long): Flow<List<MessageBO>> = channelFlow {
        android.util.Log.d("MsgRepo", "observeMessages conv=$conversationId historyVisibleFrom=$historyVisibleFrom")
        launch {
            catchResult {
                val remote = remoteSource.getMessages(conversationId, historyVisibleFrom)
                android.util.Log.d("MsgRepo", "getMessages returned ${remote.size} messages")
                messageDao.upsertAll(remote.map { it.toDBO() })
            }
        }
        launch {
            remoteSource.observeNewMessages(conversationId).collect { dto ->
                catchResult { messageDao.upsert(dto.toDBO()) }
            }
        }
        messageDao.observeByConversation(conversationId, historyVisibleFrom).map { dbos ->
            android.util.Log.d("MsgRepo", "Room emit: ${dbos.size} msgs conv=$conversationId since=$historyVisibleFrom firstCreatedAt=${dbos.firstOrNull()?.createdAt} lastCreatedAt=${dbos.lastOrNull()?.createdAt}")
            dbos.map { dbo ->
                val senderName = userDao.getById(dbo.senderId)?.displayName ?: dbo.senderId
                dbo.toBO(currentUserId, senderName)
            }
        }.collect { send(it) }
    }

    override suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        content: String,
        imageUrl: String?,
        audioUrl: String?,
        replyToId: String?,
        replyToContent: String?,
        replyToSenderName: String?,
        callType: String?,
        callStatus: String?,
        callDuration: Int?,
        gifUrl: String?,
        stickerUrl: String?,
    ): MessageBO {
        val messageDto = MessageDTO(
            id = java.util.UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderId = senderId,
            content = content,
            isRead = false,
            createdAt = Instant.fromEpochMilliseconds(System.currentTimeMillis()).toString(),
            imageUrl = imageUrl,
            audioUrl = audioUrl,
            replyToId = replyToId,
            replyToContent = replyToContent,
            replyToSenderName = replyToSenderName,
            callType = callType,
            callStatus = callStatus,
            callDuration = callDuration,
            gifUrl = gifUrl,
            stickerUrl = stickerUrl,
        )
        remoteSource.sendMessage(messageDto)
        val messageDbo = messageDto.toDBO()
        messageDao.upsert(messageDbo)
        val senderName = userDao.getById(senderId)?.displayName ?: senderId
        return messageDbo.toBO(senderId, senderName)
    }

    override suspend fun uploadAudio(conversationId: String, bytes: ByteArray): String {
        bytes.checkAudioSize()
        val path = "$conversationId/${java.util.UUID.randomUUID()}.m4a"
        supabase.storage[AUDIO_BUCKET].upload(path, bytes) { upsert = false }
        return supabase.storage[AUDIO_BUCKET].publicUrl(path)
    }

    override suspend fun uploadImage(conversationId: String, bytes: ByteArray, mimeType: String): String {
        bytes.checkImageSize()
        val ext = if (mimeType.contains("png")) "png" else "jpg"
        val path = "$conversationId/${java.util.UUID.randomUUID()}.$ext"
        supabase.storage[BUCKET].upload(path, bytes) { upsert = false }
        return supabase.storage[BUCKET].publicUrl(path)
    }

    override suspend fun markAsRead(conversationId: String, userId: String) {
        messageDao.markAllRead(conversationId)
        remoteSource.markAsRead(conversationId, userId)
    }

    override fun syncRemote(conversationId: String, historyVisibleFrom: Long): Flow<Unit> = channelFlow {
        launch {
            catchResult {
                val remote = remoteSource.getMessages(conversationId, historyVisibleFrom)
                android.util.Log.d("MsgRepo", "syncRemote fetched ${remote.size} msgs conv=$conversationId since=$historyVisibleFrom")
                messageDao.upsertAll(remote.map { it.toDBO() })
            }
        }
        launch {
            remoteSource.observeNewMessages(conversationId).collect { dto ->
                catchResult { messageDao.upsert(dto.toDBO()) }
            }
        }
        awaitCancellation()
    }

    override fun getMessagesPaged(conversationId: String, currentUserId: String, historyVisibleFrom: Long): Flow<PagingData<MessageBO>> =
        Pager(PagingConfig(pageSize = 30, enablePlaceholders = false)) {
            messageDao.getMessagesPaged(conversationId, historyVisibleFrom)
        }.flow.map { pagingData ->
            pagingData.map { dbo ->
                val senderName = userDao.getById(dbo.senderId)?.displayName ?: dbo.senderId
                dbo.toBO(currentUserId, senderName)
            }
        }

    override suspend fun syncMessages(conversationId: String, since: Long) {
        val dtos = remoteSource.getMessages(conversationId, since)
        messageDao.upsertAll(dtos.map { it.toDBO() })
    }

    override suspend fun clearMessages(conversationId: String) {
        messageDao.deleteByConversation(conversationId)
    }
}
