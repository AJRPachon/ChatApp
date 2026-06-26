package com.ajrpachon.chatapp.ui.chat
import com.ajrpachon.chatapp.utils.catchResult

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ajrpachon.chatapp.data.local.dao.ConversationDao
import com.ajrpachon.chatapp.domain.model.CallBO
import com.ajrpachon.chatapp.domain.model.CallType
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.repository.CallRepository
import com.ajrpachon.chatapp.domain.repository.GroupRepository
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import com.ajrpachon.chatapp.domain.repository.UserRepository
import com.ajrpachon.chatapp.domain.usecase.GetGroupMembersUseCase
import com.ajrpachon.chatapp.domain.usecase.LeaveGroupUseCase
import com.ajrpachon.chatapp.domain.usecase.SendMessageUseCase
import com.ajrpachon.chatapp.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File



class ChatViewModel(
    private val conversationId: String,
    private val otherUserName: String,
    private val sendMessageUseCase: SendMessageUseCase,
    private val messageRepository: MessageRepository,
    private val conversationDao: ConversationDao,
    private val callRepository: CallRepository,
    private val userRepository: UserRepository,
    private val getGroupMembersUseCase: GetGroupMembersUseCase,
    private val leaveGroupUseCase: LeaveGroupUseCase,
    private val groupRepository: GroupRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state = _state.asStateFlow()

    private val _effect = Channel<ChatEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    // Resolved synchronously — Supabase Auth is in-memory, never blocks.
    private val currentUserId: String? = userRepository.getCurrentUserId()

    private val _historyVisibleFrom = MutableStateFlow(0L)

    val messages: Flow<PagingData<MessageBO>> = _historyVisibleFrom
        .flatMapLatest { since ->
            messageRepository.getMessagesPaged(
                conversationId,
                currentUserId ?: "",
                since,
            )
        }
        .cachedIn(viewModelScope)

    private var recorder: MediaRecorder? = null
    private var recordingTimerJob: Job? = null
    private var remoteSyncJob: Job? = null

    init {
        _state.update { it.copy(conversationTitle = otherUserName) }
        val uid = currentUserId
        if (uid != null) {
            _state.update { it.copy(currentUserId = uid) }
            viewModelScope.launch {
            val conv = conversationDao.getById(conversationId)
            val otherUserId = conv?.otherUserId
            val isGroup = conv?.isGroup == true
            val historyVisibleFrom = conv?.historyVisibleFrom ?: 0L
            AppLogger.d(TAG, "init conv=$conversationId historyVisibleFrom=$historyVisibleFrom isGroup=$isGroup")
            _state.update {
                it.copy(
                    otherUserId = otherUserId,
                    isGroup = isGroup,
                    groupAvatarUrl = conv?.groupAvatarUrl,
                    isCurrentUserMember = true,
                    isMuted = conv?.isMuted == true,
                )
            }
            _historyVisibleFrom.value = historyVisibleFrom
            startRemoteSync(historyVisibleFrom)

            launch {
                catchResult { messageRepository.markAsRead(conversationId, uid) }
                catchResult { conversationDao.resetUnreadCount(conversationId) }
            }

            if (otherUserId != null) {
                launch {
                    userRepository.observeUserById(otherUserId).collect { user ->
                        _state.update {
                            it.copy(
                                otherUserAvatarUrl = user?.avatarUrl ?: it.otherUserAvatarUrl,
                                isOtherUserOnline = user?.isOnline() == true,
                                otherUserLastSeenMs = user?.lastSeen?.toEpochMilliseconds(),
                            )
                        }
                    }
                }
                launch {
                    catchResult { userRepository.getUserById(otherUserId) }
                }
            }

            launch {
                conversationDao.observeById(conversationId).collect { conv ->
                    if (conv != null) {
                        _state.update {
                            it.copy(
                                groupAvatarUrl = conv.groupAvatarUrl,
                                conversationTitle = if (conv.isGroup) conv.name?.takeIf { n -> n.isNotBlank() } ?: it.conversationTitle else it.conversationTitle,
                            )
                        }
                    }
                }
            }

            if (isGroup) {
                launch {
                    catchResult { groupRepository.syncMembership(conversationId) }
                    while (isActive) {
                        delay(3_000)
                        catchResult { groupRepository.syncMembership(conversationId) }
                    }
                }
                var previousIsMember = true
                try {
                    getGroupMembersUseCase(conversationId).collect { members ->
                        val isMember = members.any { it.userId == uid }
                        AppLogger.d(TAG, "members emission: size=${members.size} isMember=$isMember prev=$previousIsMember")
                        _state.update { it.copy(isCurrentUserMember = isMember) }
                        when {
                            isMember && !previousIsMember -> {
                                launch {
                                    val since = conversationDao.getById(conversationId)?.historyVisibleFrom ?: 0L
                                    AppLogger.d(TAG, "re-joined: syncMessages since=$since")
                                    catchResult { messageRepository.syncMessages(conversationId, since) }
                                    _historyVisibleFrom.value = since
                                    startRemoteSync(since)
                                }
                            }
                            !isMember && previousIsMember -> {
                                launch {
                                    AppLogger.d(TAG, "expelled: clearMessages")
                                    catchResult { messageRepository.clearMessages(conversationId) }
                                }
                            }
                        }
                        previousIsMember = isMember
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    AppLogger.e(TAG, "getGroupMembers collect FAILED conv=$conversationId", e)
                }
                AppLogger.w(TAG, "getGroupMembers collect ENDED conv=$conversationId")
            }
        }
        } else {
            AppLogger.e(TAG, "getCurrentUserId returned null — aborting init conv=$conversationId")
        }
    }

    private fun startRemoteSync(historyVisibleFrom: Long) {
        remoteSyncJob?.cancel()
        remoteSyncJob = viewModelScope.launch {
            AppLogger.d(TAG, "startRemoteSync conv=$conversationId since=$historyVisibleFrom")
            messageRepository.syncRemote(conversationId, historyVisibleFrom).collect { }
        }
    }

    fun onIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.InputChanged -> _state.update { it.copy(inputText = intent.text) }
            is ChatIntent.Send -> if (_state.value.editingMessage != null) confirmEdit() else sendMessage()
            is ChatIntent.SendImages -> sendImages(intent.context, intent.uris)
            is ChatIntent.StartRecording -> startRecording(intent.context, intent.outputFilePath)
            is ChatIntent.StopRecording -> stopRecording()
            is ChatIntent.DiscardAudio -> discardAudio()
            is ChatIntent.SendAudio -> sendAudio()
            is ChatIntent.StartCall -> startCall(intent.callType)
            is ChatIntent.DismissError -> _state.update { it.copy(error = null) }
            is ChatIntent.SetReply -> _state.update { it.copy(replyingTo = intent.message) }
            is ChatIntent.CancelReply -> _state.update { it.copy(replyingTo = null) }
            is ChatIntent.OpenStickerPicker -> _state.update { it.copy(showStickerPicker = true) }
            is ChatIntent.CloseStickerPicker -> _state.update { it.copy(showStickerPicker = false) }
            is ChatIntent.SendGif -> sendGif(intent.url)
            is ChatIntent.SendSticker -> sendSticker(intent.emoji)
            is ChatIntent.ToggleMute -> toggleMute()
            is ChatIntent.LeaveGroup -> leaveGroup()
            is ChatIntent.StartEdit -> _state.update { it.copy(editingMessage = intent.message, inputText = intent.message.content) }
            is ChatIntent.CancelEdit -> _state.update { it.copy(editingMessage = null, inputText = "") }
            is ChatIntent.ConfirmEdit -> confirmEdit()
        }
    }

    private fun sendMessage() {
        val text = _state.value.inputText.trim()
        val userId = _state.value.currentUserId ?: return
        if (text.isBlank()) return
        val reply = _state.value.replyingTo

        viewModelScope.launch {
            _effect.send(ChatEffect.ScrollToBottom)
            _state.update { it.copy(isSending = true, inputText = "", replyingTo = null) }
            sendMessageUseCase(
                conversationId, userId, text,
                replyToId = reply?.id,
                replyToContent = reply?.replySnippet(),
                replyToSenderName = reply?.senderName,
            ).onFailure { e ->
                AppLogger.e(TAG, "Send message failed", e)
                _state.update { it.copy(error = e.message, inputText = text) }
            }
            _state.update { it.copy(isSending = false) }
        }
    }

    private fun sendImages(context: Context, uris: List<Uri>) {
        val userId = _state.value.currentUserId ?: return
        val reply = _state.value.replyingTo
        viewModelScope.launch {
            _effect.send(ChatEffect.ScrollToBottom)
            _state.update { it.copy(isUploadingImage = true, replyingTo = null) }
            for ((index, uri) in uris.withIndex()) {
                val bytes = try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }
                } catch (e: Exception) { null }
                if (bytes == null) continue
                catchResult {
                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val imageUrl = messageRepository.uploadImage(conversationId, bytes, mimeType)
                    val replyForImage = if (index == 0) reply else null
                    sendMessageUseCase(
                        conversationId, userId, "", imageUrl,
                        replyToId = replyForImage?.id,
                        replyToContent = replyForImage?.replySnippet(),
                        replyToSenderName = replyForImage?.senderName,
                    )
                }.onFailure { e ->
                    AppLogger.e(TAG, "Send image failed", e)
                    _state.update { it.copy(error = e.message ?: "Error uploading image") }
                }
            }
            _state.update { it.copy(isUploadingImage = false) }
        }
    }

    private fun startRecording(context: Context, outputFilePath: String) {
        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        catchResult {
            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFilePath)
                prepare()
                start()
            }
            recorder = rec
            _state.update {
                it.copy(audioState = AudioState(isRecording = true, pendingFilePath = outputFilePath))
            }
            val startMs = System.currentTimeMillis()
            recordingTimerJob = viewModelScope.launch {
                while (true) {
                    delay(100)
                    val elapsed = System.currentTimeMillis() - startMs
                    val amp = catchResult {
                        (recorder?.maxAmplitude ?: 0).toFloat() / 32767f
                    }.getOrDefault(0f)
                    _state.update { s ->
                        val newHistory = (s.audioState.amplitudeHistory + amp).takeLast(30)
                        s.copy(audioState = s.audioState.copy(
                            recordingDurationMs = elapsed,
                            amplitudeHistory = newHistory,
                        ))
                    }
                }
            }
        }.onFailure { e ->
            AppLogger.e(TAG, "Recording failed", e)
            catchResult { rec.release() }
            _state.update { it.copy(error = "No se pudo iniciar la grabación") }
        }
    }

    private fun stopRecording() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
        val durationMs = _state.value.audioState.recordingDurationMs
        catchResult { recorder?.apply { stop(); release() } }
        recorder = null
        _state.update { it.copy(audioState = it.audioState.copy(isRecording = false, recordingDurationMs = durationMs)) }
    }

    private fun discardAudio() {
        val path = _state.value.audioState.pendingFilePath
        path?.let { catchResult { File(it).delete() } }
        _state.update { it.copy(audioState = AudioState()) }
    }

    private fun sendAudio() {
        val userId = _state.value.currentUserId ?: return
        val filePath = _state.value.audioState.pendingFilePath ?: return
        val reply = _state.value.replyingTo
        viewModelScope.launch {
            _effect.send(ChatEffect.ScrollToBottom)
            _state.update { it.copy(audioState = it.audioState.copy(isUploading = true), replyingTo = null) }
            catchResult {
                val bytes = withContext(Dispatchers.IO) { File(filePath).readBytes() }
                val audioUrl = messageRepository.uploadAudio(conversationId, bytes)
                sendMessageUseCase(
                    conversationId, userId, "", audioUrl = audioUrl,
                    replyToId = reply?.id,
                    replyToContent = reply?.replySnippet(),
                    replyToSenderName = reply?.senderName,
                )
                catchResult { File(filePath).delete() }
                _state.update { it.copy(audioState = AudioState()) }
            }.onFailure { e ->
                AppLogger.e(TAG, "Send audio failed", e)
                _state.update {
                    it.copy(
                        audioState = it.audioState.copy(isUploading = false),
                        error = e.message ?: "Error al enviar el audio",
                    )
                }
            }
        }
    }

    private fun startCall(typeStr: String) {
        val callType = if (typeStr == "video") CallType.VIDEO else CallType.AUDIO
        val isGroup = _state.value.isGroup
        viewModelScope.launch {
            catchResult {
                val call = if (isGroup) {
                    callRepository.createGroupCall(conversationId, callType)
                } else {
                    val calleeId = _state.value.otherUserId ?: return@launch
                    callRepository.createCall(conversationId, calleeId, callType)
                }
                _effect.send(ChatEffect.NavigateToCall(call))
            }.onFailure { e ->
                AppLogger.e(TAG, "Start call failed", e)
                _state.update { it.copy(error = e.message ?: "Error al iniciar la llamada") }
            }
        }
    }

    private fun sendGif(url: String) {
        val userId = _state.value.currentUserId ?: return
        val reply = _state.value.replyingTo
        viewModelScope.launch {
            _effect.send(ChatEffect.ScrollToBottom)
            _state.update { it.copy(showStickerPicker = false, replyingTo = null) }
            sendMessageUseCase(
                conversationId, userId, "",
                gifUrl = url,
                replyToId = reply?.id,
                replyToContent = reply?.replySnippet(),
                replyToSenderName = reply?.senderName,
            ).onFailure { e ->
                AppLogger.e(TAG, "Send GIF failed", e)
                _state.update { it.copy(error = e.message ?: "Error al enviar el GIF") }
            }
        }
    }

    private fun sendSticker(emoji: String) {
        val userId = _state.value.currentUserId ?: return
        val reply = _state.value.replyingTo
        viewModelScope.launch {
            _effect.send(ChatEffect.ScrollToBottom)
            _state.update { it.copy(showStickerPicker = false, replyingTo = null) }
            sendMessageUseCase(
                conversationId, userId, "",
                stickerUrl = emoji,
                replyToId = reply?.id,
                replyToContent = reply?.replySnippet(),
                replyToSenderName = reply?.senderName,
            ).onFailure { e ->
                AppLogger.e(TAG, "Send sticker failed", e)
                _state.update { it.copy(error = e.message ?: "Error al enviar el sticker") }
            }
        }
    }

    private fun toggleMute() {
        val newMuted = !_state.value.isMuted
        _state.update { it.copy(isMuted = newMuted) }
        viewModelScope.launch {
            catchResult { conversationDao.updateMuted(conversationId, newMuted) }
                .onFailure { e ->
                    _state.update { it.copy(isMuted = !newMuted) }
                    AppLogger.e(TAG, "Toggle mute failed", e)
                }
        }
    }

    private fun confirmEdit() {
        val editingMsg = _state.value.editingMessage ?: return
        val newContent = _state.value.inputText.trim()
        if (newContent.isBlank() || newContent == editingMsg.content) {
            _state.update { it.copy(editingMessage = null, inputText = "") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(editingMessage = null, inputText = "") }
            messageRepository.editMessage(editingMsg.id, newContent)
                .onFailure { e ->
                    AppLogger.e(TAG, "Edit message failed", e)
                    _state.update { it.copy(error = "No se pudo editar el mensaje") }
                }
        }
    }

    private fun leaveGroup() {
        val userId = _state.value.currentUserId ?: return
        viewModelScope.launch {
            leaveGroupUseCase(conversationId, userId)
                .onSuccess { _effect.send(ChatEffect.NavigateBack) }
                .onFailure { e ->
                    AppLogger.e(TAG, "Leave group failed", e)
                    _state.update { it.copy(error = e.message ?: "Error al salir del grupo") }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        AppLogger.d(TAG, "onCleared conv=$conversationId")
        recordingTimerJob?.cancel()
        remoteSyncJob?.cancel()
        catchResult { recorder?.apply { stop(); release() } }
        recorder = null
    }

    companion object {
        private const val TAG = "ChatViewModel"
    }
}
