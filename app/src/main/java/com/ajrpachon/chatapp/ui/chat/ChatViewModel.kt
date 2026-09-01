package com.ajrpachon.chatapp.ui.chat

import android.app.Application
import android.net.Uri
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ajrpachon.chatapp.domain.model.ChatTheme
import com.ajrpachon.chatapp.domain.repository.AiAssistantRepository
import com.ajrpachon.chatapp.domain.repository.ChatThemeRepository
import com.ajrpachon.chatapp.domain.repository.ContactRepository
import com.ajrpachon.chatapp.domain.repository.DraftRepository
import com.ajrpachon.chatapp.domain.repository.IncognitoRepository
import com.ajrpachon.chatapp.domain.repository.PollRepository
import com.ajrpachon.chatapp.domain.repository.WallpaperRepository
import com.ajrpachon.chatapp.domain.model.CallType
import com.ajrpachon.chatapp.domain.model.GroupMemberBO
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.repository.CallRepository
import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import com.ajrpachon.chatapp.domain.repository.GroupRepository
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import com.ajrpachon.chatapp.domain.repository.PendingMessageRepository
import com.ajrpachon.chatapp.domain.repository.ReactionRepository
import com.ajrpachon.chatapp.domain.repository.ScheduledMessageRepository
import com.ajrpachon.chatapp.domain.repository.TypingRepository
import com.ajrpachon.chatapp.domain.repository.UserRepository
import com.ajrpachon.chatapp.domain.usecase.ExportConversationUseCase
import com.ajrpachon.chatapp.domain.usecase.GetGroupMembersUseCase
import com.ajrpachon.chatapp.domain.usecase.GetUriMetadataUseCase
import com.ajrpachon.chatapp.domain.usecase.LeaveGroupUseCase
import com.ajrpachon.chatapp.domain.usecase.ReadUriAsBytesUseCase
import com.ajrpachon.chatapp.domain.usecase.SendInvitationUseCase
import com.ajrpachon.chatapp.domain.usecase.SendMessageUseCase
import com.ajrpachon.chatapp.ui.common.BaseViewModel
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.AudioTranscriber
import com.ajrpachon.chatapp.utils.ClipboardProtection
import com.ajrpachon.chatapp.utils.LinkPreviewFetcher
import com.ajrpachon.chatapp.utils.NetworkMonitor
import com.ajrpachon.chatapp.utils.TranslationManager
import com.ajrpachon.chatapp.utils.catchResult
import com.ajrpachon.chatapp.worker.MessageRetryWorker
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.TimeUnit

data class ChatArgs(val conversationId: String, val otherUserName: String)

@Suppress("LongParameterList", "TooManyFunctions")
class ChatViewModel(
    args: ChatArgs,
    private val application: Application,
    private val clipboardProtection: ClipboardProtection,
    private val sendMessageUseCase: SendMessageUseCase,
    private val messageRepository: MessageRepository,
    private val pendingMessageRepository: PendingMessageRepository,
    private val callRepository: CallRepository,
    private val userRepository: UserRepository,
    private val getGroupMembersUseCase: GetGroupMembersUseCase,
    private val leaveGroupUseCase: LeaveGroupUseCase,
    private val groupRepository: GroupRepository,
    private val reactionRepository: ReactionRepository,
    private val conversationRepository: ConversationRepository,
    private val scheduledMessageRepository: ScheduledMessageRepository,
    private val typingRepository: TypingRepository,
    private val draftRepository: DraftRepository,
    private val translationManager: TranslationManager,
    private val audioTranscriber: AudioTranscriber,
    private val pollRepository: PollRepository,
    private val contactRepository: ContactRepository,
    private val chatThemeRepository: ChatThemeRepository,
    private val workManager: WorkManager,
    private val incognitoRepository: IncognitoRepository,
    private val aiAssistantRepository: AiAssistantRepository,
    private val wallpaperRepository: WallpaperRepository,
    private val networkMonitor: NetworkMonitor,
    private val sendInvitationUseCase: SendInvitationUseCase,
    private val exportConversationUseCase: ExportConversationUseCase,
    private val linkPreviewFetcher: LinkPreviewFetcher,
    private val getUriMetadataUseCase: GetUriMetadataUseCase,
    private val readUriAsBytesUseCase: ReadUriAsBytesUseCase,
) : BaseViewModel<ChatState, ChatEffect>(ChatState()) {

    private val conversationId = args.conversationId
    private val otherUserName = args.otherUserName
    private val currentUserId: String? = userRepository.getCurrentUserId()

    private val _historyVisibleFrom = MutableStateFlow(0L)

    val reactions: Flow<Map<String, List<com.ajrpachon.chatapp.domain.model.ReactionBO>>> =
        reactionRepository.observeReactions(conversationId)

    val messages: Flow<PagingData<MessageBO>> = _historyVisibleFrom
        .flatMapLatest { since ->
            messageRepository.getMessagesPaged(conversationId, currentUserId ?: "", since)
        }
        .cachedIn(viewModelScope)

    val pinnedMessages: Flow<List<MessageBO>> =
        messageRepository.getPinnedMessages(conversationId, currentUserId ?: "")

    // See docs/chat-viewmodel-decomposition.md — delegates extracted out of this class's flat
    // method list, one concern each, all sharing delegateContext as their only hook back into
    // this ViewModel's state/effects.
    private val delegateContext = ChatDelegateContext(
        getState = { state.value },
        updateState = ::updateState,
        sendEffect = ::sendEffect,
    )
    private val aiDelegate = ChatAiDelegate(
        conversationId = conversationId,
        currentUserId = { currentUserId },
        messageRepository = messageRepository,
        aiAssistantRepository = aiAssistantRepository,
        scope = viewModelScope,
        context = delegateContext,
    )
    private val translationDelegate = ChatTranslationDelegate(
        translationManager = translationManager,
        audioTranscriber = audioTranscriber,
        scope = viewModelScope,
        context = delegateContext,
    )
    private val pollDelegate = ChatPollDelegate(
        conversationId = conversationId,
        currentUserId = { currentUserId },
        pollRepository = pollRepository,
        sendMessageUseCase = sendMessageUseCase,
        scope = viewModelScope,
        context = delegateContext,
    )
    private val schedulingDelegate = ChatSchedulingDelegate(
        conversationId = conversationId,
        scheduledMessageRepository = scheduledMessageRepository,
        draftRepository = draftRepository,
        workManager = workManager,
        cancelDraftSave = { draftSaveJob?.cancel() },
        scope = viewModelScope,
        context = delegateContext,
    )
    private val forwardDelegate = ChatForwardDelegate(
        conversationId = conversationId,
        currentUserId = { currentUserId },
        conversationRepository = conversationRepository,
        messageRepository = messageRepository,
        scope = viewModelScope,
        context = delegateContext,
    )
    private val contactCardDelegate = ChatContactCardDelegate(
        currentUserId = { currentUserId },
        userRepository = userRepository,
        sendInvitationUseCase = sendInvitationUseCase,
        scope = viewModelScope,
        context = delegateContext,
    )
    private val searchDelegate = ChatSearchDelegate(
        conversationId = conversationId,
        currentUserId = { currentUserId },
        messageRepository = messageRepository,
        scope = viewModelScope,
        context = delegateContext,
    )
    private val mediaUploadDelegate = ChatMediaUploadDelegate(
        conversationId = conversationId,
        messageRepository = messageRepository,
        sendMessageUseCase = sendMessageUseCase,
        getUriMetadataUseCase = getUriMetadataUseCase,
        readUriAsBytesUseCase = readUriAsBytesUseCase,
        scope = viewModelScope,
        context = delegateContext,
    )
    private val quickSendDelegate = ChatQuickSendDelegate(
        conversationId = conversationId,
        application = application,
        sendMessageUseCase = sendMessageUseCase,
        contactRepository = contactRepository,
        scope = viewModelScope,
        context = delegateContext,
    )
    private val audioRecordingDelegate = ChatAudioRecordingDelegate(
        conversationId = conversationId,
        application = application,
        messageRepository = messageRepository,
        sendMessageUseCase = sendMessageUseCase,
        clearDraft = { draftSaveJob?.cancel(); draftRepository.saveDraft(conversationId, "") },
        scope = viewModelScope,
        context = delegateContext,
    )

    private var groupMembers: List<GroupMemberBO> = emptyList()
    private var remoteSyncJob: Job? = null
    private var typingResetJob: Job? = null
    private var typingPresenceJob: Job? = null
    private var draftSaveJob: Job? = null
    private val memberOnlineStatuses = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private var memberObserveJob: Job? = null
    private val requestedLinkPreviewUrls = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online -> updateState { it.copy(isOnline = online) } }
        }
        viewModelScope.launch {
            memberOnlineStatuses.collect { map ->
                updateState { it.copy(onlineMemberCount = map.values.count { v -> v }) }
            }
        }
        viewModelScope.launch { catchResult { messageRepository.deleteExpiredMessages() } }
        viewModelScope.launch {
            scheduledMessageRepository.observeAll().collect { all ->
                val forThisConversation = all.filter { it.conversationId == conversationId }
                updateState { it.copy(scheduledMessageCount = forThisConversation.size, scheduledMessages = forThisConversation) }
            }
        }
        viewModelScope.launch {
            incognitoRepository.isIncognito(conversationId).collect { incognito ->
                updateState { it.copy(isIncognito = incognito) }
            }
        }
        viewModelScope.launch {
            val draft = draftRepository.getDraft(conversationId).first()
            if (draft.isNotBlank()) updateState { it.copy(inputText = draft) }
        }
        viewModelScope.launch {
            chatThemeRepository.observe(conversationId).collect { theme -> updateState { it.copy(chatTheme = theme) } }
        }
        viewModelScope.launch {
            wallpaperRepository.getWallpaperColor(conversationId).collect { color -> updateState { it.copy(wallpaperColor = color) } }
        }
        updateState { it.copy(conversationTitle = otherUserName) }
        val uid = currentUserId
        if (uid != null) {
            updateState { it.copy(currentUserId = uid) }
            viewModelScope.launch {
                val conv = conversationRepository.getById(conversationId)
                val otherUserId = conv?.otherUserId
                val isGroup = conv?.isGroup == true
                val historyVisibleFrom = conv?.historyVisibleFrom ?: 0L
                updateState {
                    it.copy(
                        otherUserId = otherUserId,
                        isGroup = isGroup,
                        groupAvatarUrl = conv?.groupAvatarUrl,
                        isCurrentUserMember = true,
                        isMuted = conv?.isMuted == true,
                        mutedUntil = conv?.mutedUntil ?: 0L,
                        disappearingModeSeconds = conv?.disappearingModeSeconds ?: 0L,
                    )
                }
                _historyVisibleFrom.value = historyVisibleFrom
                startRemoteSync(historyVisibleFrom)
                startTypingPresence(uid)
                launch {
                    catchResult { messageRepository.markAsRead(conversationId, uid) }
                    catchResult { conversationRepository.resetUnreadCount(conversationId) }
                }
                if (otherUserId != null) {
                    launch {
                        userRepository.observeUserById(otherUserId).collect { user ->
                            updateState {
                                it.copy(
                                    otherUserAvatarUrl = user?.avatarUrl ?: it.otherUserAvatarUrl,
                                    isOtherUserOnline = user?.isOnline() == true,
                                    otherUserLastSeenMs = user?.lastSeen?.toEpochMilliseconds(),
                                )
                            }
                        }
                    }
                    launch { catchResult { userRepository.getUserById(otherUserId) } }
                }
                launch {
                    conversationRepository.observeById(conversationId).collect { c ->
                        if (c != null) {
                            updateState {
                                it.copy(
                                    groupAvatarUrl = c.groupAvatarUrl,
                                    conversationTitle = if (c.isGroup) c.name.takeIf { n -> n.isNotBlank() } ?: it.conversationTitle else it.conversationTitle,
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
                    catchResult {
                        getGroupMembersUseCase(conversationId).collect { members ->
                            groupMembers = members
                            val isMember = members.any { it.userId == uid }
                            updateState { it.copy(isCurrentUserMember = isMember, groupMemberCount = members.size) }
                            memberObserveJob?.cancel()
                            memberObserveJob = launch {
                                memberOnlineStatuses.value = emptyMap()
                                for (member in members) {
                                    launch {
                                        catchResult {
                                            userRepository.observeUserById(member.userId).collect { user ->
                                                memberOnlineStatuses.value = memberOnlineStatuses.value + (member.userId to (user?.isOnline() == true))
                                            }
                                        }
                                    }
                                }
                            }
                            when {
                                isMember && !previousIsMember -> {
                                    launch {
                                        val since = conversationRepository.getById(conversationId)?.historyVisibleFrom ?: 0L
                                        catchResult { messageRepository.syncMessages(conversationId, since) }
                                        _historyVisibleFrom.value = since
                                        startRemoteSync(since)
                                    }
                                }
                                !isMember && previousIsMember -> {
                                    launch { catchResult { messageRepository.clearMessages(conversationId) } }
                                }
                            }
                            previousIsMember = isMember
                        }
                    }.onFailure { e -> AppLogger.e(TAG, "getGroupMembers FAILED", e) }
                }
            }
        } else {
            AppLogger.e(TAG, "getCurrentUserId null -- aborting init")
        }
    }

    private fun startTypingPresence(uid: String) {
        typingPresenceJob?.cancel()
        typingPresenceJob = viewModelScope.launch {
            catchResult {
                typingRepository.observeTypingNames(conversationId, uid)
                    .onEach { names -> updateState { it.copy(typingUserNames = names) } }
                    .launchIn(this)
                typingRepository.subscribeChannel(conversationId)
            }.onFailure { e -> AppLogger.d(TAG, "typing presence failed: ${e.message}") }
        }
    }

    private fun sendTypingPresence(isTyping: Boolean) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            catchResult {
                typingRepository.sendTypingState(
                    conversationId = conversationId,
                    userId = uid,
                    userName = if (isTyping) otherUserName.ifBlank { "Usuario" } else "",
                    isTyping = isTyping,
                )
            }
        }
    }

    private fun startRemoteSync(historyVisibleFrom: Long) {
        remoteSyncJob?.cancel()
        remoteSyncJob = viewModelScope.launch {
            messageRepository.syncRemote(conversationId, historyVisibleFrom).collect { }
        }
    }

    @Suppress("CyclomaticComplexMethod")
    fun onIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.InputChanged -> {
                updateState { it.copy(inputText = intent.text) }
                draftSaveJob?.cancel()
                draftSaveJob = viewModelScope.launch {
                    delay(500)
                    draftRepository.saveDraft(conversationId, intent.text)
                }
                val lastWord = intent.text.substringAfterLast(' ')
                if (state.value.isGroup && lastWord.startsWith("@") && lastWord.length > 1) {
                    val partial = lastWord.removePrefix("@").lowercase()
                    val matches = groupMembers.filter { m ->
                        m.username.lowercase().contains(partial) || m.displayName.lowercase().contains(partial)
                    }
                    updateState { it.copy(mentionSuggestions = matches, showMentionSuggestions = matches.isNotEmpty()) }
                } else {
                    updateState { it.copy(mentionSuggestions = emptyList(), showMentionSuggestions = false) }
                }
                if (intent.text.isNotEmpty()) {
                    sendTypingPresence(true)
                    typingResetJob?.cancel()
                    typingResetJob = viewModelScope.launch { delay(3_000); sendTypingPresence(false) }
                } else {
                    typingResetJob?.cancel(); sendTypingPresence(false)
                }
            }
            is ChatIntent.Send -> if (state.value.editingMessage != null) confirmEdit() else sendMessage()
            is ChatIntent.SendImages -> mediaUploadDelegate.sendImages(intent.uris)
            is ChatIntent.SendFile -> mediaUploadDelegate.sendFile(intent.uri)
            is ChatIntent.SendVideo -> mediaUploadDelegate.sendVideo(intent.uri)
            is ChatIntent.StartRecording -> audioRecordingDelegate.startRecording()
            is ChatIntent.StopRecording -> audioRecordingDelegate.stopRecording()
            is ChatIntent.DiscardAudio -> audioRecordingDelegate.discardAudio()
            is ChatIntent.SendAudio -> audioRecordingDelegate.sendAudio()
            is ChatIntent.StartCall -> startCall(intent.callType)
            is ChatIntent.DismissError -> updateState { it.copy(error = null) }
            is ChatIntent.SetReply -> updateState { it.copy(replyingTo = intent.message) }
            is ChatIntent.CancelReply -> updateState { it.copy(replyingTo = null) }
            is ChatIntent.OpenStickerPicker -> updateState { it.copy(showStickerPicker = true) }
            is ChatIntent.CloseStickerPicker -> updateState { it.copy(showStickerPicker = false) }
            is ChatIntent.SendGif -> quickSendDelegate.sendGif(intent.url)
            is ChatIntent.SendSticker -> quickSendDelegate.sendSticker(intent.emoji)
            is ChatIntent.ToggleMute -> toggleMute()
            is ChatIntent.ShowMuteDialog -> updateState { it.copy(showMuteDialog = true) }
            is ChatIntent.DismissMuteDialog -> updateState { it.copy(showMuteDialog = false) }
            is ChatIntent.MuteFor -> muteFor(intent.mutedUntil)
            is ChatIntent.LeaveGroup -> leaveGroup()
            is ChatIntent.DeleteMessage -> deleteMessage(intent.messageId)
            is ChatIntent.StartEdit -> updateState { it.copy(editingMessage = intent.message, inputText = intent.message.content) }
            is ChatIntent.CancelEdit -> updateState { it.copy(editingMessage = null, inputText = "") }
            is ChatIntent.ConfirmEdit -> confirmEdit()
            is ChatIntent.OpenSearch -> updateState { it.copy(isSearchActive = true, searchQuery = "", searchResults = emptyList()) }
            is ChatIntent.CloseSearch -> updateState { it.copy(isSearchActive = false, searchQuery = "", searchResults = emptyList()) }
            is ChatIntent.SearchQueryChanged -> searchDelegate.searchMessages(intent.query)
            is ChatIntent.ToggleReaction -> toggleReaction(intent.messageId, intent.emoji)
            is ChatIntent.JumpToMessage -> searchDelegate.jumpToMessage(intent.messageId)
            is ChatIntent.ShowExpiryDialog -> updateState { it.copy(expiryDialogMessageId = intent.messageId) }
            is ChatIntent.DismissExpiryDialog -> updateState { it.copy(expiryDialogMessageId = null) }
            is ChatIntent.SetExpiry -> setExpiry(intent.messageId, intent.expiresAt)
            is ChatIntent.ToggleMessageSelection -> toggleMessageSelection(intent.messageId)
            is ChatIntent.ClearSelection -> updateState { it.copy(selectedMessageIds = emptySet()) }
            is ChatIntent.DeleteSelectedMessages -> deleteSelectedMessages()
            is ChatIntent.ShowForwardDialog -> forwardDelegate.showForwardDialog(intent.message)
            is ChatIntent.DismissForwardDialog -> updateState { it.copy(showForwardDialog = false, forwardingMessage = null, forwardableConversations = emptyList()) }
            is ChatIntent.ForwardMessage -> forwardDelegate.forwardMessage(intent.targetConversationId)
            is ChatIntent.ShowForwardSelectionDialog -> forwardDelegate.showForwardSelectionDialog()
            is ChatIntent.DismissForwardSelectionDialog -> updateState { it.copy(showForwardSelectionDialog = false, forwardableConversations = emptyList()) }
            is ChatIntent.ForwardSelectedMessages -> forwardDelegate.forwardSelectedMessages(intent.targetConversationId)
            is ChatIntent.SendLocation -> quickSendDelegate.sendLocationMessage(intent.mapsUrl)
            is ChatIntent.FetchAndSendLocation -> quickSendDelegate.fetchAndSendLocation()
            is ChatIntent.TranslateMessage -> translationDelegate.translateMessage(intent.messageId, intent.text)
            is ChatIntent.DismissTranslation -> updateState { it.copy(translatedTexts = it.translatedTexts - intent.messageId) }
            is ChatIntent.TranscribeAudio -> translationDelegate.transcribeAudio(intent.messageId)
            is ChatIntent.PinMessage -> pinMessage(intent.messageId)
            is ChatIntent.UnpinMessage -> unpinMessage(intent.messageId)
            is ChatIntent.SaveMessage -> viewModelScope.launch { catchResult { messageRepository.setSaved(intent.messageId, true) } }
            is ChatIntent.UnsaveMessage -> viewModelScope.launch { catchResult { messageRepository.setSaved(intent.messageId, false) } }
            is ChatIntent.OpenCreatePollSheet -> updateState { it.copy(showCreatePollSheet = true) }
            is ChatIntent.DismissCreatePollSheet -> updateState { it.copy(showCreatePollSheet = false) }
            is ChatIntent.CreatePoll -> pollDelegate.createPoll(intent.question, intent.options, intent.allowMultiple)
            is ChatIntent.VotePoll -> pollDelegate.votePoll(intent.pollId, intent.optionId)
            is ChatIntent.SetChatTheme -> setChatTheme(intent.theme)
            is ChatIntent.OpenThemePicker -> updateState { it.copy(showThemePicker = true) }
            is ChatIntent.DismissThemePicker -> updateState { it.copy(showThemePicker = false) }
            is ChatIntent.ExportConversation -> exportConversation()
            is ChatIntent.CopyMessageContent -> clipboardProtection.copyWithTimeout("message", intent.content, viewModelScope)
            is ChatIntent.ShowDisappearingModeSheet -> updateState { it.copy(showDisappearingModeSheet = true) }
            is ChatIntent.DismissDisappearingModeSheet -> updateState { it.copy(showDisappearingModeSheet = false) }
            is ChatIntent.SetDisappearingMode -> setDisappearingMode(intent.conversationId, intent.seconds)
            is ChatIntent.SelectMention -> selectMention(intent.member)
            is ChatIntent.ToggleIncognito -> toggleIncognito()
            is ChatIntent.DismissIncognitoDialog -> updateState { it.copy(showIncognitoInfoDialog = false) }
            is ChatIntent.ConfirmIncognito -> confirmIncognito()
            is ChatIntent.OpenScheduleDialog -> updateState { it.copy(showScheduleDialog = true) }
            is ChatIntent.DismissScheduleDialog -> updateState { it.copy(showScheduleDialog = false) }
            is ChatIntent.ScheduleMessage -> schedulingDelegate.scheduleMessage(intent.scheduledAt)
            is ChatIntent.ShowScheduledSheet -> updateState { it.copy(showScheduledSheet = true) }
            is ChatIntent.DismissScheduledSheet -> updateState { it.copy(showScheduledSheet = false) }
            is ChatIntent.CancelScheduledMessage -> schedulingDelegate.cancelScheduledMessage(intent.id)
            is ChatIntent.OpenAiSheet -> updateState { it.copy(showAiSheet = true, aiSuggestion = null) }
            is ChatIntent.DismissAiSheet -> updateState { it.copy(showAiSheet = false, aiSuggestion = null) }
            is ChatIntent.AiSummarize -> aiDelegate.summarize()
            is ChatIntent.AiSuggestReply -> aiDelegate.suggestReply()
            is ChatIntent.AiFreeform -> aiDelegate.freeform(intent.prompt)
            is ChatIntent.InsertAiSuggestion -> {
                val suggestion = state.value.aiSuggestion ?: return
                updateState { it.copy(inputText = suggestion, showAiSheet = false, aiSuggestion = null) }
            }
            is ChatIntent.OpenWallpaperPicker -> updateState { it.copy(showWallpaperPicker = true) }
            is ChatIntent.DismissWallpaperPicker -> updateState { it.copy(showWallpaperPicker = false) }
            is ChatIntent.SetWallpaperColor -> viewModelScope.launch { wallpaperRepository.setWallpaperColor(conversationId, intent.color) }
            is ChatIntent.SendContact -> quickSendDelegate.sendContact(intent.name, intent.phone)
            is ChatIntent.ContactSelected -> quickSendDelegate.handleContactSelected(intent.uri)
            is ChatIntent.RetryMessage -> enqueueMessageRetry()
            is ChatIntent.CheckContactRelationship -> contactCardDelegate.checkContactRelationship(intent.phone)
            is ChatIntent.ContactCardPrimaryAction -> contactCardDelegate.contactCardPrimaryAction(intent.phone)
            is ChatIntent.ObservePoll -> pollDelegate.observePoll(intent.pollId)
            is ChatIntent.DetectedUrlChanged -> fetchLinkPreview(intent.url)
        }
    }

    /**
     * Fetches (and caches in state) the link preview for a URL detected in a plain-text
     * message. [LinkPreviewFetcher] has its own in-memory cache, so re-triggering for the
     * same URL is cheap, but [requestedLinkPreviewUrls] avoids launching duplicate coroutines.
     */
    private fun fetchLinkPreview(url: String) {
        if (!requestedLinkPreviewUrls.add(url)) return
        viewModelScope.launch {
            val preview = catchResult { linkPreviewFetcher.fetchLinkPreview(url) }.getOrNull()
            updateState { it.copy(linkPreviews = it.linkPreviews + (url to preview)) }
        }
    }

    private fun enqueueMessageRetry() {
        val request = OneTimeWorkRequestBuilder<MessageRetryWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(MessageRetryWorker.WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    private fun selectMention(member: GroupMemberBO) {
        val currentText = state.value.inputText
        val lastAtIndex = currentText.lastIndexOf('@')
        val newText = if (lastAtIndex >= 0) currentText.substring(0, lastAtIndex) + "@${member.username} "
                      else currentText + "@${member.username} "
        updateState { it.copy(inputText = newText, mentionSuggestions = emptyList(), showMentionSuggestions = false) }
    }

    private fun setExpiry(messageId: String, expiresAt: Long?) {
        updateState { it.copy(expiryDialogMessageId = null) }
        viewModelScope.launch {
            catchResult { messageRepository.setMessageExpiry(messageId, expiresAt) }
                .onFailure { e -> AppLogger.e(TAG, "setExpiry failed", e) }
        }
    }

    private fun toggleMessageSelection(messageId: String) {
        updateState { s ->
            val updated = if (messageId in s.selectedMessageIds) s.selectedMessageIds - messageId else s.selectedMessageIds + messageId
            s.copy(selectedMessageIds = updated)
        }
    }

    private fun deleteSelectedMessages() {
        val ids = state.value.selectedMessageIds.toSet()
        updateState { it.copy(selectedMessageIds = emptySet()) }
        viewModelScope.launch {
            for (id in ids) {
                messageRepository.deleteMessage(id).onFailure { e -> AppLogger.e(TAG, "deleteMessage $id failed", e) }
            }
        }
    }

    private fun toggleReaction(messageId: String, emoji: String) {
        val uid = currentUserId ?: return
        viewModelScope.launch { catchResult { reactionRepository.toggleReaction(messageId, uid, emoji) } }
    }

    private fun sendMessage() {
        val text = state.value.inputText.trim()
        val userId = state.value.currentUserId ?: return
        if (text.isBlank()) return
        val reply = state.value.replyingTo
        val disappearingSecs = state.value.disappearingModeSeconds
        viewModelScope.launch {
            sendEffect(ChatEffect.ScrollToBottom)
            updateState { it.copy(isSending = true, inputText = "", replyingTo = null) }
            draftSaveJob?.cancel()
            draftRepository.saveDraft(conversationId, "")
            val result = sendMessageUseCase(conversationId, userId, text,
                replyToId = reply?.id, replyToContent = reply?.replySnippet(), replyToSenderName = reply?.senderName,
            )
            result.onSuccess { msg ->
                if (disappearingSecs > 0L) {
                    catchResult { messageRepository.setMessageExpiry(msg.id, System.currentTimeMillis() + disappearingSecs * 1_000L) }
                }
            }
            if (result.isFailure) {
                val e = result.exceptionOrNull()
                AppLogger.e(TAG, "Send failed -- offline retry", e)
                catchResult {
                    pendingMessageRepository.savePendingMessage(
                        id = UUID.randomUUID().toString(),
                        conversationId = conversationId,
                        senderId = userId,
                        content = text,
                        replyToId = reply?.id,
                        replyToContent = reply?.replySnippet(),
                        replyToSenderName = reply?.senderName,
                    )
                }.onFailure { dbErr -> AppLogger.e(TAG, "Failed to save pending message", dbErr) }
                workManager.enqueueUniqueWork(
                    MessageRetryWorker.WORK_NAME, ExistingWorkPolicy.KEEP,
                    OneTimeWorkRequestBuilder<MessageRetryWorker>()
                        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                        .build()
                )
                updateState { it.copy(error = "Sin conexion. El mensaje se enviara cuando vuelva la red.", inputText = text) }
            }
            updateState { it.copy(isSending = false) }
        }
    }

    private fun setDisappearingMode(conversationId: String, seconds: Long) {
        updateState { it.copy(showDisappearingModeSheet = false, disappearingModeSeconds = seconds) }
        viewModelScope.launch {
            catchResult { conversationRepository.setDisappearingMode(conversationId, seconds) }
                .onFailure { e -> AppLogger.e(TAG, "setDisappearingMode failed", e) }
        }
    }

    private fun startCall(typeStr: String) {
        val callType = CallType.fromWire(typeStr)
        val isGroup = state.value.isGroup
        viewModelScope.launch {
            catchResult {
                val call = if (isGroup) callRepository.createGroupCall(conversationId, callType)
                           else callRepository.createCall(conversationId, state.value.otherUserId ?: return@catchResult, callType)
                sendEffect(ChatEffect.NavigateToCall(call))
            }.onFailure { e -> updateState { it.copy(error = e.message ?: "Error al iniciar la llamada") } }
        }
    }

    private fun toggleMute() {
        val newMuted = !state.value.isMuted
        updateState { it.copy(isMuted = newMuted) }
        viewModelScope.launch {
            catchResult { conversationRepository.toggleMute(conversationId, newMuted) }
                .onFailure { updateState { it.copy(isMuted = !newMuted) } }
        }
    }

    private fun muteFor(mutedUntil: Long) {
        updateState { it.copy(showMuteDialog = false, isMuted = mutedUntil != 0L, mutedUntil = mutedUntil) }
        viewModelScope.launch { catchResult { conversationRepository.muteFor(conversationId, mutedUntil) } }
    }

    private fun confirmEdit() {
        val editingMsg = state.value.editingMessage ?: return
        val newContent = state.value.inputText.trim()
        if (newContent.isBlank() || newContent == editingMsg.content) {
            updateState { it.copy(editingMessage = null, inputText = "") }; return
        }
        viewModelScope.launch {
            updateState { it.copy(editingMessage = null, inputText = "") }
            messageRepository.editMessage(editingMsg.id, newContent)
                .onFailure { e -> AppLogger.e(TAG, "Edit failed", e); updateState { it.copy(error = "No se pudo editar") } }
        }
    }

    private fun leaveGroup() {
        val userId = state.value.currentUserId ?: return
        viewModelScope.launch {
            leaveGroupUseCase(conversationId, userId)
                .onSuccess { sendEffect(ChatEffect.NavigateBack) }
                .onFailure { e -> updateState { it.copy(error = e.message ?: "Error al salir del grupo") } }
        }
    }

    private fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessage(messageId)
                .onFailure { e -> AppLogger.e(TAG, "deleteMessage failed", e); updateState { it.copy(error = "No se pudo eliminar") } }
        }
    }

    private fun exportConversation() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            updateState { it.copy(isExporting = true) }
            exportConversationUseCase(conversationId, uid)
                .onSuccess { uriString -> sendEffect(ChatEffect.ShowShareSheet(Uri.parse(uriString))) }
                .onFailure { sendEffect(ChatEffect.ShowSnackbar("No se pudo exportar")) }
            updateState { it.copy(isExporting = false) }
        }
    }

    private fun setChatTheme(theme: ChatTheme) {
        viewModelScope.launch { catchResult { chatThemeRepository.set(conversationId, theme) } }
    }

    private fun pinMessage(messageId: String) {
        viewModelScope.launch { catchResult { messageRepository.setPinned(messageId, true) } }
    }

    private fun unpinMessage(messageId: String) {
        viewModelScope.launch { catchResult { messageRepository.setPinned(messageId, false) } }
    }

    private fun toggleIncognito() {
        if (state.value.isIncognito) viewModelScope.launch { incognitoRepository.setIncognito(conversationId, false) }
        else updateState { it.copy(showIncognitoInfoDialog = true) }
    }

    private fun confirmIncognito() {
        updateState { it.copy(showIncognitoInfoDialog = false) }
        viewModelScope.launch { incognitoRepository.setIncognito(conversationId, true) }
    }

    override fun onCleared() {
        super.onCleared()
        remoteSyncJob?.cancel(); typingResetJob?.cancel()
        typingPresenceJob?.cancel(); draftSaveJob?.cancel()
        viewModelScope.launch {
            withContext(NonCancellable) { catchResult { typingRepository.close(conversationId) } }
        }
        audioRecordingDelegate.cleanup()
    }

    companion object {
        private const val TAG = "ChatViewModel"
    }
}
