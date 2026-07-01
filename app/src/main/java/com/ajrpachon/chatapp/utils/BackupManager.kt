package com.ajrpachon.chatapp.utils

import android.accounts.AccountManager
import android.content.Context
import com.ajrpachon.chatapp.data.local.dao.MessageDao
import com.ajrpachon.chatapp.data.local.entity.MessageDBO
import com.google.android.gms.auth.GoogleAuthUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val DRIVE_UPLOAD_URL =
    "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
private const val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
private const val BACKUP_FILE_NAME = "chatapp_backup.json"
private const val DRIVE_SCOPE = "oauth2:https://www.googleapis.com/auth/drive.file"

data class BackupInfo(
    val lastBackupDate: String,
    val backupSizeMb: String,
    val fileId: String,
)

@Serializable
private data class MessageBackup(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val content: String,
    val isRead: Boolean,
    val createdAt: Long,
    val imageUrl: String? = null,
    val audioUrl: String? = null,
    val replyToId: String? = null,
    val replyToContent: String? = null,
    val replyToSenderName: String? = null,
    val callType: String? = null,
    val callStatus: String? = null,
    val callDuration: Int? = null,
    val gifUrl: String? = null,
    val stickerUrl: String? = null,
    val isEncrypted: Boolean = false,
    val isDeleted: Boolean = false,
    val isEdited: Boolean = false,
    val editedAt: Long? = null,
    val expiresAt: Long? = null,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val fileMimeType: String? = null,
    val videoUrl: String? = null,
    val isPinned: Boolean = false,
    val isSaved: Boolean = false,
)

private fun MessageDBO.toBackup() = MessageBackup(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    content = content,
    isRead = isRead,
    createdAt = createdAt,
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
    isDeleted = isDeleted,
    isEdited = isEdited,
    editedAt = editedAt,
    expiresAt = expiresAt,
    fileUrl = fileUrl,
    fileName = fileName,
    fileSize = fileSize,
    fileMimeType = fileMimeType,
    videoUrl = videoUrl,
    isPinned = isPinned,
    isSaved = isSaved,
)

private fun MessageBackup.toDBO() = MessageDBO(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    content = content,
    isRead = isRead,
    createdAt = createdAt,
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
    isDeleted = isDeleted,
    isEdited = isEdited,
    editedAt = editedAt,
    expiresAt = expiresAt,
    fileUrl = fileUrl,
    fileName = fileName,
    fileSize = fileSize,
    fileMimeType = fileMimeType,
    videoUrl = videoUrl,
    isPinned = isPinned,
    isSaved = isSaved,
)

class BackupManager(
    private val context: Context,
    private val messageDao: MessageDao,
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    private suspend fun getAccessToken(): String = withContext(Dispatchers.IO) {
        val accounts = AccountManager.get(context).getAccountsByType("com.google")
        val account = accounts.firstOrNull()
            ?: error("No Google account found. Sign in with Google to enable backups.")
        GoogleAuthUtil.getToken(context, account, DRIVE_SCOPE)
    }

    private suspend fun findExistingBackupFileId(token: String): String? =
        withContext(Dispatchers.IO) {
            val encodedQuery = java.net.URLEncoder.encode(
                "name='$BACKUP_FILE_NAME' and trashed=false", "UTF-8"
            )
            val url = "$DRIVE_FILES_URL?q=$encodedQuery" +
                "&fields=files(id)&orderBy=modifiedTime+desc"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()
            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string() ?: return@withContext null
                val files = JSONObject(body).optJSONArray("files") ?: return@withContext null
                if (files.length() == 0) null else files.getJSONObject(0).getString("id")
            }
        }

    suspend fun backup(): BackupInfo = withContext(Dispatchers.IO) {
        val token = getAccessToken()

        val messages = messageDao.getAllMessages()
        val backups = messages.map { it.toBackup() }
        val jsonBytes = json.encodeToString(backups).toByteArray(Charsets.UTF_8)

        val existingId = findExistingBackupFileId(token)

        val metadata = """{"name":"$BACKUP_FILE_NAME","mimeType":"application/json"}"""
        val metaPart = metadata.toRequestBody("application/json; charset=UTF-8".toMediaType())
        val dataPart = jsonBytes.toRequestBody("application/json".toMediaType())
        val multipart = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(metaPart)
            .addPart(dataPart)
            .build()

        val request = if (existingId != null) {
            Request.Builder()
                .url(
                    "https://www.googleapis.com/upload/drive/v3/files/$existingId" +
                        "?uploadType=multipart"
                )
                .addHeader("Authorization", "Bearer $token")
                .patch(multipart)
                .build()
        } else {
            Request.Builder()
                .url(DRIVE_UPLOAD_URL)
                .addHeader("Authorization", "Bearer $token")
                .post(multipart)
                .build()
        }

        httpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                error("Drive upload failed: ${resp.code} ${resp.body?.string()}")
            }
        }

        val sizeMb = "%.2f".format(jsonBytes.size.toDouble() / 1_048_576)
        BackupInfo(
            lastBackupDate = dateFormat.format(Date()),
            backupSizeMb = sizeMb,
            fileId = existingId ?: "",
        )
    }

    suspend fun restore() = withContext(Dispatchers.IO) {
        val token = getAccessToken()

        val encodedQuery = java.net.URLEncoder.encode(
            "name='$BACKUP_FILE_NAME' and trashed=false", "UTF-8"
        )
        val url = "$DRIVE_FILES_URL?q=$encodedQuery" +
            "&fields=files(id)&orderBy=modifiedTime+desc"
        val listRequest = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        val fileId = httpClient.newCall(listRequest).execute().use { resp ->
            if (!resp.isSuccessful) error("Drive list failed: ${resp.code}")
            val body = resp.body?.string() ?: error("Empty response from Drive")
            val files = JSONObject(body).optJSONArray("files")
                ?: error("No backup file found on Drive")
            if (files.length() == 0) error("No backup file found on Drive")
            files.getJSONObject(0).getString("id")
        }

        val downloadRequest = Request.Builder()
            .url("$DRIVE_FILES_URL/$fileId?alt=media")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        val jsonText = httpClient.newCall(downloadRequest).execute().use { resp ->
            if (!resp.isSuccessful) error("Drive download failed: ${resp.code}")
            resp.body?.string() ?: error("Empty backup file")
        }

        val messages = json.decodeFromString<List<MessageBackup>>(jsonText)
        messageDao.upsertAll(messages.map { it.toDBO() })
    }

    suspend fun getLatestBackupInfo(): BackupInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val token = getAccessToken()
            val encodedQuery = java.net.URLEncoder.encode(
                "name='$BACKUP_FILE_NAME' and trashed=false", "UTF-8"
            )
            val url = "$DRIVE_FILES_URL?q=$encodedQuery" +
                "&fields=files(id,size,modifiedTime)&orderBy=modifiedTime+desc"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()
            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body?.string() ?: return@use null
                val files = JSONObject(body).optJSONArray("files") ?: return@use null
                if (files.length() == 0) return@use null
                val file = files.getJSONObject(0)
                val fileId = file.getString("id")
                val sizeBytes = file.optLong("size", 0L)
                val modifiedTime = file.optString("modifiedTime", "")
                val formattedDate = runCatching {
                    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                    val date = isoFormat.parse(modifiedTime)
                    if (date != null) dateFormat.format(date) else modifiedTime
                }.getOrDefault(modifiedTime)
                val sizeMb = "%.2f".format(sizeBytes.toDouble() / 1_048_576)
                BackupInfo(
                    lastBackupDate = formattedDate,
                    backupSizeMb = sizeMb,
                    fileId = fileId,
                )
            }
        }.getOrNull()
    }
}
