package com.ajrpachon.chatapp.data.repository
import com.ajrpachon.chatapp.utils.catchResult
import com.ajrpachon.chatapp.utils.AppLogger


import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.ajrpachon.chatapp.data.local.dao.MessageDao
import com.ajrpachon.chatapp.data.local.dao.ReactionDao
import com.ajrpachon.chatapp.data.local.dao.UserDao
import com.ajrpachon.chatapp.data.local.entity.ReactionDBO
import com.ajrpachon.chatapp.data.mapper.toDBO
import com.ajrpachon.chatapp.data.mapper.toBO
import com.ajrpachon.chatapp.data.remote.dto.MessageDTO
import com.ajrpachon.chatapp.data.remote.source.MessageRemoteSource
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import com.ajrpachon.chatapp.utils.E2EEKeyManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.SecretKey
import com.ajrpachon.chatapp.utils.UploadLimits.checkAudioSize
import com.ajrpachon.chatapp.utils.UploadLimits.checkImageSize
import com.ajrpachon.chatapp.utils.UploadLimits.checkFileSize
import com.ajrpachon.chatapp.utils.UploadLimits.checkVideoSize
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
private const val TAG = "MsgRepo"

private const val BUCKET = "chat-images"
private const val AUDIO_BUCKET = "chat-audio"
private const val FILE_BUCKET = "chat-files"
private const val VIDEO_BUCKET = "chat-videos"

/** Minimal DTO for fetching only the public_key field from profiles. */
@Serializable
private data class PublicKeyDTO(
    @SerialName("id") val id: String,
    @SerialName("public_key") val publicKey: String? = null,
)

class MessageRepositoryImpl(
    private val messageDao: MessageDao,
    private val userDao: UserDao,
    private val reactionDao: ReactionDao,
    private val remoteSource: MessageRemoteSource,
    private val supabase: SupabaseClient,
) : MessageRepository {

    // Shared key cache: avoids a Supabase round-trip on every encrypt/decrypt.
    // Key = (localUserId, peerUserId). Entry is populated on first use per session.
    private val sharedKeyCache = ConcurrentHashMap<Pair<String, String>, SecretKey>()
    private val keyDerivationMutex = Mutex()

    override fun observeMessages(conversationId: String, currentUserId: String, historyVisibleFrom: Long): Flow<List<MessageBO>> = channelFlow {
        AppLogger.d(TAG, "observeMessages conv=$conversationId historyVisibleFrom=$historyVisibleFrom")
        launch {
            catchResult {
                val remote = remoteSource.getMessages(conversationId, historyVisibleFrom)
                AppLogger.d(TAG, "getMessages returned ${remote.size} messages")
                messageDao.upsertAll(remote.map { it.toDBO() })
            }
        }
        launch {
            remoteSource.observeNewMessages(conversationId).collect { dto ->
                catchResult { messageDao.upsert(dto.toDBO()) }
            }
        }
        messageDao.observeByConversation(conversationId, historyVisibleFrom).map { dbos ->
            AppLogger.d(TAG, "Room emit: ${dbos.size} msgs conv=$conversationId since=$historyVisibleFrom firstCreatedAt=${dbos.firstOrNull()?.createdAt} lastCreatedAt=${dbos.lastOrNull()?.createdAt}")
            dbos.map { dbo ->
                val senderName = userDao.getById(dbo.senderId)?.displayName ?: dbo.senderId
                val bo = dbo.toBO(currentUserId, senderName)
                // Attempt to decrypt E2EE messages inline; fall back to ciphertext on error
                if (bo.isEncrypted && bo.content.isNotBlank()) {
                    tryDecrypt(currentUserId, bo.senderId, bo)
                } else {
                    bo
                }
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
        fileUrl: String?,
        fileName: String?,
        fileSize: Long?,
        fileMimeType: String?,
        videoUrl: String?,
        otherUserId: String?,
    ): MessageBO {
        // Attempt E2EE for 1:1 text messages (skip for media/call messages and group chats)
        val (finalContent, isEncrypted) = if (
            otherUserId != null &&
            content.isNotBlank() &&
            imageUrl == null && audioUrl == null && callType == null &&
            gifUrl == null && stickerUrl == null && fileUrl == null && videoUrl == null
        ) {
            tryEncrypt(senderId, otherUserId, content)
        } else {
            Pair(content, false)
        }

        val messageDto = MessageDTO(
            id = java.util.UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderId = senderId,
            content = finalContent,
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
            fileUrl = fileUrl,
            fileName = fileName,
            fileSize = fileSize,
            fileMimeType = fileMimeType,
            videoUrl = videoUrl,
            isEncrypted = isEncrypted,
        )
        remoteSource.sendMessage(messageDto)
        val messageDbo = messageDto.toDBO()
        messageDao.upsert(messageDbo)
        val senderName = userDao.getById(senderId)?.displayName ?: senderId
        // Return with plaintext for local display
        return messageDbo.toBO(senderId, senderName).let {
            if (isEncrypted) it.copy(content = content, isEncrypted = true) else it
        }
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

    override suspend fun uploadFile(conversationId: String, bytes: ByteArray, fileName: String, mimeType: String): String {
        bytes.checkFileSize()
        val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val path = "$conversationId/${java.util.UUID.randomUUID()}_$safeName"
        supabase.storage[FILE_BUCKET].upload(path, bytes) { upsert = false }
        return supabase.storage[FILE_BUCKET].publicUrl(path)
    }

    override suspend fun uploadVideo(conversationId: String, bytes: ByteArray): String {
        bytes.checkVideoSize()
        val path = "$conversationId/${java.util.UUID.randomUUID()}.mp4"
        supabase.storage[VIDEO_BUCKET].upload(path, bytes) { upsert = false }
        return supabase.storage[VIDEO_BUCKET].publicUrl(path)
    }

    override suspend fun markAsRead(conversationId: String, userId: String) {
        messageDao.markAllRead(conversationId)
        remoteSource.markAsRead(conversationId, userId)
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> = catchResult {
        remoteSource.deleteMessage(messageId)
        messageDao.markDeleted(messageId)
    }

    override suspend fun editMessage(messageId: String, newContent: String): Result<Unit> = catchResult {
        remoteSource.editMessage(messageId, newContent)
        messageDao.updateContent(messageId, newContent, System.currentTimeMillis())
    }

    override fun syncRemote(conversationId: String, historyVisibleFrom: Long): Flow<Unit> = channelFlow {
        launch {
            catchResult {
                val remote = remoteSource.getMessages(conversationId, historyVisibleFrom)
                AppLogger.d(TAG, "syncRemote fetched ${remote.size} msgs conv=$conversationId since=$historyVisibleFrom")
                messageDao.upsertAll(remote.map { it.toDBO() })
                // Sync reactions for fetched messages
                val messageIds = remote.map { it.id }
                if (messageIds.isNotEmpty()) {
                    val reactions = remoteSource.getReactionsForMessages(messageIds)
                    reactionDao.upsertAll(reactions.map { ReactionDBO(it.messageId, it.userId, it.emoji) })
                }
            }
        }
        launch {
            remoteSource.observeNewMessages(conversationId).collect { dto ->
                catchResult { messageDao.upsert(dto.toDBO()) }
            }
        }
        launch {
            remoteSource.observeMessageUpdates(conversationId).collect { dto ->
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
                val bo = dbo.toBO(currentUserId, senderName)
                if (bo.isEncrypted && bo.content.isNotBlank()) {
                    tryDecrypt(currentUserId, bo.senderId, bo)
                } else {
                    bo
                }
            }
        }

    override suspend fun syncMessages(conversationId: String, since: Long) {
        val dtos = remoteSource.getMessages(conversationId, since)
        messageDao.upsertAll(dtos.map { it.toDBO() })
    }

    override suspend fun clearMessages(conversationId: String) {
        messageDao.deleteByConversation(conversationId)
    }

    override suspend fun searchMessages(conversationId: String, currentUserId: String, query: String): List<MessageBO> {
        if (query.isBlank()) return emptyList()
        return messageDao.searchMessages(conversationId, query.trim()).map { dbo ->
            val senderName = userDao.getById(dbo.senderId)?.displayName ?: dbo.senderId
            dbo.toBO(currentUserId, senderName)
        }
    }

    override suspend fun setMessageExpiry(messageId: String, expiresAt: Long?) {
        messageDao.setExpiry(messageId, expiresAt)
    }

    override suspend fun deleteExpiredMessages() {
        messageDao.deleteExpired(System.currentTimeMillis())
    }

    override fun getPinnedMessages(conversationId: String, currentUserId: String): Flow<List<MessageBO>> =
        messageDao.getPinnedMessages(conversationId).map { dbos ->
            dbos.map { dbo ->
                val senderName = userDao.getById(dbo.senderId)?.displayName ?: dbo.senderId
                dbo.toBO(currentUserId, senderName)
            }
        }

    override suspend fun setPinned(messageId: String, pinned: Boolean) {
        messageDao.setPinned(messageId, pinned)
    }

    override fun getSavedMessages(currentUserId: String): Flow<List<MessageBO>> =
        messageDao.getSavedMessages().map { dbos ->
            dbos.map { dbo ->
                val senderName = userDao.getById(dbo.senderId)?.displayName ?: dbo.senderId
                dbo.toBO(currentUserId, senderName)
            }
        }

    override suspend fun setSaved(messageId: String, saved: Boolean) {
        messageDao.setSaved(messageId, saved)
    }

    // ---------------------------------------------------------------------------
    // E2EE helpers
    // ---------------------------------------------------------------------------

    /**
     * Returns the cached shared key for [localUserId]+[peerId], deriving and caching it on
     * first call. Returns null if the peer has no public key registered yet.
     */
    private suspend fun getOrDeriveSharedKey(localUserId: String, peerId: String): SecretKey? {
        val cacheKey = localUserId to peerId
        sharedKeyCache[cacheKey]?.let { return it }
        // Mutex prevents duplicate derivations when multiple messages arrive concurrently.
        return keyDerivationMutex.withLock {
            sharedKeyCache[cacheKey]?.let { return it }
            val row = supabase.postgrest["profiles"]
                .select { filter { eq("id", peerId) } }
                .decodeSingleOrNull<PublicKeyDTO>()
            val peerPublicKey = row?.publicKey
            if (peerPublicKey.isNullOrBlank()) return null
            E2EEKeyManager.getOrCreateKeyPair(localUserId)
            val key = E2EEKeyManager.deriveSharedKey(localUserId, peerPublicKey)
            sharedKeyCache[cacheKey] = key
            key
        }
    }

    /** Encrypts [content] for [otherUserId]. Falls back to plaintext on any error. */
    private suspend fun tryEncrypt(senderId: String, otherUserId: String, content: String): Pair<String, Boolean> {
        return runCatching {
            val sharedKey = getOrDeriveSharedKey(senderId, otherUserId)
            if (sharedKey == null) {
                android.util.Log.d("E2EE", "No public key for $otherUserId — sending unencrypted")
                return Pair(content, false)
            }
            Pair(E2EEKeyManager.encrypt(sharedKey, content), true)
        }.getOrElse { e ->
            android.util.Log.w("E2EE", "Encryption failed, sending unencrypted: ${e.message}")
            Pair(content, false)
        }
    }

    /**
     * Decrypts [bo] using the sender's public key. Falls back to ciphertext on any error.
     * [currentUserId] is the local user; [senderId] is who sent the message.
     */
    private suspend fun tryDecrypt(currentUserId: String, senderId: String, bo: MessageBO): MessageBO {
        return runCatching {
            val sharedKey = getOrDeriveSharedKey(currentUserId, senderId)
            if (sharedKey == null) {
                android.util.Log.d("E2EE", "No public key for sender $senderId — cannot decrypt")
                return bo
            }
            bo.copy(content = E2EEKeyManager.decrypt(sharedKey, bo.content))
        }.getOrElse { e ->
            android.util.Log.w("E2EE", "Decryption failed for msg ${bo.id}: ${e.message}")
            bo
        }
    }

    override suspend fun getAllMessages(conversationId: String, currentUserId: String): List<MessageBO> {
        return messageDao.getAllMessages(conversationId).map { dbo ->
            val senderName = userDao.getById(dbo.senderId)?.displayName ?: dbo.senderId
            dbo.toBO(currentUserId, senderName)
        }
    }
}
