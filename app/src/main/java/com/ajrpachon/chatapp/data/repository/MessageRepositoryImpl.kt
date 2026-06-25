package com.ajrpachon.chatapp.data.repository
import com.ajrpachon.chatapp.utils.catchResult
import com.ajrpachon.chatapp.utils.AppLogger

private const val TAG = "MsgRepo"

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
import com.ajrpachon.chatapp.utils.E2EEKeyManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.ajrpachon.chatapp.utils.UploadLimits.checkAudioSize
import com.ajrpachon.chatapp.utils.UploadLimits.checkImageSize
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val BUCKET = "chat-images"
private const val AUDIO_BUCKET = "chat-audio"

/** Minimal DTO for fetching only the public_key field from profiles. */
@Serializable
private data class PublicKeyDTO(
    @SerialName("id") val id: String,
    @SerialName("public_key") val publicKey: String? = null,
)

class MessageRepositoryImpl(
    private val messageDao: MessageDao,
    private val userDao: UserDao,
    private val remoteSource: MessageRemoteSource,
    private val supabase: SupabaseClient,
) : MessageRepository {

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
        otherUserId: String?,
    ): MessageBO {
        // Attempt E2EE for 1:1 text messages (skip for media/call messages and group chats)
        val (finalContent, isEncrypted) = if (
            otherUserId != null &&
            content.isNotBlank() &&
            imageUrl == null && audioUrl == null && callType == null &&
            gifUrl == null && stickerUrl == null
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

    override suspend fun markAsRead(conversationId: String, userId: String) {
        messageDao.markAllRead(conversationId)
        remoteSource.markAsRead(conversationId, userId)
    }

    override fun syncRemote(conversationId: String, historyVisibleFrom: Long): Flow<Unit> = channelFlow {
        launch {
            catchResult {
                val remote = remoteSource.getMessages(conversationId, historyVisibleFrom)
                AppLogger.d(TAG, "syncRemote fetched ${remote.size} msgs conv=$conversationId since=$historyVisibleFrom")
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

    // ---------------------------------------------------------------------------
    // E2EE helpers
    // ---------------------------------------------------------------------------

    /**
     * Fetches the other user's public key from `profiles` and encrypts [content].
     * Falls back to plaintext (unencrypted) if the key is missing or an error occurs.
     */
    private suspend fun tryEncrypt(senderId: String, otherUserId: String, content: String): Pair<String, Boolean> {
        return runCatching {
            val row = supabase.postgrest["profiles"]
                .select { filter { eq("id", otherUserId) } }
                .decodeSingleOrNull<PublicKeyDTO>()
            val otherPublicKey = row?.publicKey
            if (otherPublicKey.isNullOrBlank()) {
                android.util.Log.d("E2EE", "No public key for $otherUserId — sending unencrypted")
                return Pair(content, false)
            }
            // Ensure our own key pair exists so the other side can decrypt
            E2EEKeyManager.getOrCreateKeyPair(senderId)
            val sharedKey = E2EEKeyManager.deriveSharedKey(senderId, otherPublicKey)
            val encrypted = E2EEKeyManager.encrypt(sharedKey, content)
            Pair(encrypted, true)
        }.getOrElse { e ->
            android.util.Log.w("E2EE", "Encryption failed, sending unencrypted: ${e.message}")
            Pair(content, false)
        }
    }

    /**
     * Attempts to decrypt a message received with [isEncrypted] == true.
     * Falls back to the original (ciphertext) [bo] on any error.
     *
     * [currentUserId] is the local user; [senderId] is who sent the message.
     * We derive the shared key from our own private key + sender's public key.
     */
    private suspend fun tryDecrypt(currentUserId: String, senderId: String, bo: MessageBO): MessageBO {
        return runCatching {
            // TODO: cache shared keys per (currentUserId, senderId) pair to avoid
            //       repeated Supabase fetches. For now fetch each time.
            val row = supabase.postgrest["profiles"]
                .select { filter { eq("id", senderId) } }
                .decodeSingleOrNull<PublicKeyDTO>()
            val senderPublicKey = row?.publicKey
            if (senderPublicKey.isNullOrBlank()) {
                android.util.Log.d("E2EE", "No public key for sender $senderId — cannot decrypt")
                return bo
            }
            val sharedKey = E2EEKeyManager.deriveSharedKey(currentUserId, senderPublicKey)
            val plaintext = E2EEKeyManager.decrypt(sharedKey, bo.content)
            bo.copy(content = plaintext)
        }.getOrElse { e ->
            android.util.Log.w("E2EE", "Decryption failed for msg ${bo.id}: ${e.message}")
            bo // return as-is (shows ciphertext rather than crashing)
        }
    }
}
