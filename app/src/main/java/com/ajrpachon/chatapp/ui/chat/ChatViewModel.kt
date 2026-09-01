package com.ajrpachon.chatapp.ui.chat

import android.annotation.SuppressLint
import android.app.Application
import android.location.LocationManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
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
import com.ajrpachon.chatapp.domain.model.LocationMessageFormat
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
import com.ajrpachon.chatapp.domain.model.UserRelationship
import com.ajrpachon.chatapp.domain.usecase.ExportConversationUseCase
import com.ajrpachon.chatapp.domain.usecase.GetGroupMembersUseCase
import com.ajrpachon.chatapp.domain.usecase.GetUriMetadataUseCase
import com.ajrpachon.chatapp.domain.usecase.LeaveGroupUseCase
import com.ajrpachon.chatapp.domain.usecase.ReadUriAsBytesUseCase
import com.ajrpachon.chatapp.domain.usecase.SendInvitationResult
import com.ajrpachon.chatapp.domain.usecase.SendInvitationUseCase
import com.ajrpachon.chatapp.domain.usecase.SendMessageUseCase
import com.ajrpachon.chatapp.ui.common.BaseViewModel
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.AudioTranscriber
import com.ajrpachon.chatapp.utils.ClipboardProtection
import com.ajrpachon.chatapp.utils.LinkPreviewFetcher
import com.ajrpachon.chatapp.utils.NetworkMonitor
import com.ajrpachon.chatapp.utils.TranslationManager
import com.ajrpachon.chatapp.utils.UploadLimits
import com.ajrpachon.chatapp.utils.catchResult
import com.ajrpachon.chatapp.worker.MessageRetryWorker
import com.ajrpachon.chatapp.worker.ScheduledMessageWorker
import kotlinx.coroutines.Dispatchers
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

    private var groupMembers: List<GroupMemberBO> = emptyList()
    private var recorder: MediaRecorder? = null
    private var recordingTimerJob: Job? = null
    private var remoteSyncJob: Job? = null
    private var typingResetJob: Job? = null
    private var typingPresenceJob: Job? = null
    private var draftSaveJob: Job? = null
    private val memberOnlineStatuses = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private var memberObserveJob: Job? = null
    private val observedPollIds = mutableSetOf<String>()
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
            is ChatIntent.SendImages -> sendImages(intent.uris)
            is ChatIntent.SendFile -> sendFile(intent.uri)
            is ChatIntent.SendVideo -> sendVideo(intent.uri)
            is ChatIntent.StartRecording -> startRecording()
            is ChatIntent.StopRecording -> stopRecording()
            is ChatIntent.DiscardAudio -> discardAudio()
            is ChatIntent.SendAudio -> sendAudio()
            is ChatIntent.StartCall -> startCall(intent.callType)
            is ChatIntent.DismissError -> updateState { it.copy(error = null) }
            is ChatIntent.SetReply -> updateState { it.copy(replyingTo = intent.message) }
            is ChatIntent.CancelReply -> updateState { it.copy(replyingTo = null) }
            is ChatIntent.OpenStickerPicker -> updateState { it.copy(showStickerPicker = true) }
            is ChatIntent.CloseStickerPicker -> updateState { it.copy(showStickerPicker = false) }
            is ChatIntent.SendGif -> sendGif(intent.url)
            is ChatIntent.SendSticker -> sendSticker(intent.emoji)
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
            is ChatIntent.SearchQueryChanged -> searchMessages(intent.query)
            is ChatIntent.ToggleReaction -> toggleReaction(intent.messageId, intent.emoji)
            is ChatIntent.JumpToMessage -> jumpToMessage(intent.messageId)
            is ChatIntent.ShowExpiryDialog -> updateState { it.copy(expiryDialogMessageId = intent.messageId) }
            is ChatIntent.DismissExpiryDialog -> updateState { it.copy(expiryDialogMessageId = null) }
            is ChatIntent.SetExpiry -> setExpiry(intent.messageId, intent.expiresAt)
            is ChatIntent.ToggleMessageSelection -> toggleMessageSelection(intent.messageId)
            is ChatIntent.ClearSelection -> updateState { it.copy(selectedMessageIds = emptySet()) }
            is ChatIntent.DeleteSelectedMessages -> deleteSelectedMessages()
            is ChatIntent.ShowForwardDialog -> showForwardDialog(intent.message)
            is ChatIntent.DismissForwardDialog -> updateState { it.copy(showForwardDialog = false, forwardingMessage = null, forwardableConversations = emptyList()) }
            is ChatIntent.ForwardMessage -> forwardMessage(intent.targetConversationId)
            is ChatIntent.ShowForwardSelectionDialog -> showForwardSelectionDialog()
            is ChatIntent.DismissForwardSelectionDialog -> updateState { it.copy(showForwardSelectionDialog = false, forwardableConversations = emptyList()) }
            is ChatIntent.ForwardSelectedMessages -> forwardSelectedMessages(intent.targetConversationId)
            is ChatIntent.SendLocation -> sendLocationMessage(intent.mapsUrl)
            is ChatIntent.FetchAndSendLocation -> fetchAndSendLocation()
            is ChatIntent.TranslateMessage -> translateMessage(intent.messageId, intent.text)
            is ChatIntent.DismissTranslation -> updateState { it.copy(translatedTexts = it.translatedTexts - intent.messageId) }
            is ChatIntent.TranscribeAudio -> transcribeAudio(intent.messageId)
            is ChatIntent.PinMessage -> pinMessage(intent.messageId)
            is ChatIntent.UnpinMessage -> unpinMessage(intent.messageId)
            is ChatIntent.SaveMessage -> viewModelScope.launch { catchResult { messageRepository.setSaved(intent.messageId, true) } }
            is ChatIntent.UnsaveMessage -> viewModelScope.launch { catchResult { messageRepository.setSaved(intent.messageId, false) } }
            is ChatIntent.OpenCreatePollSheet -> updateState { it.copy(showCreatePollSheet = true) }
            is ChatIntent.DismissCreatePollSheet -> updateState { it.copy(showCreatePollSheet = false) }
            is ChatIntent.CreatePoll -> createPoll(intent.question, intent.options, intent.allowMultiple)
            is ChatIntent.VotePoll -> votePoll(intent.pollId, intent.optionId)
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
            is ChatIntent.ScheduleMessage -> scheduleMessage(intent.scheduledAt)
            is ChatIntent.ShowScheduledSheet -> updateState { it.copy(showScheduledSheet = true) }
            is ChatIntent.DismissScheduledSheet -> updateState { it.copy(showScheduledSheet = false) }
            is ChatIntent.CancelScheduledMessage -> cancelScheduledMessage(intent.id)
            is ChatIntent.OpenAiSheet -> updateState { it.copy(showAiSheet = true, aiSuggestion = null) }
            is ChatIntent.DismissAiSheet -> updateState { it.copy(showAiSheet = false, aiSuggestion = null) }
            is ChatIntent.AiSummarize -> aiSummarize()
            is ChatIntent.AiSuggestReply -> aiSuggestReply()
            is ChatIntent.AiFreeform -> aiFreeform(intent.prompt)
            is ChatIntent.InsertAiSuggestion -> {
                val suggestion = state.value.aiSuggestion ?: return
                updateState { it.copy(inputText = suggestion, showAiSheet = false, aiSuggestion = null) }
            }
            is ChatIntent.OpenWallpaperPicker -> updateState { it.copy(showWallpaperPicker = true) }
            is ChatIntent.DismissWallpaperPicker -> updateState { it.copy(showWallpaperPicker = false) }
            is ChatIntent.SetWallpaperColor -> viewModelScope.launch { wallpaperRepository.setWallpaperColor(conversationId, intent.color) }
            is ChatIntent.SendContact -> sendContact(intent.name, intent.phone)
            is ChatIntent.ContactSelected -> handleContactSelected(intent.uri)
            is ChatIntent.RetryMessage -> enqueueMessageRetry()
            is ChatIntent.CheckContactRelationship -> checkContactRelationship(intent.phone)
            is ChatIntent.ContactCardPrimaryAction -> contactCardPrimaryAction(intent.phone)
            is ChatIntent.ObservePoll -> observePoll(intent.pollId)
            is ChatIntent.DetectedUrlChanged -> fetchLinkPreview(intent.url)
        }
    }

    private fun checkContactRelationship(phone: String) {
        if (phone.isBlank() || state.value.contactPhoneLookups.containsKey(phone)) return
        val currentId = currentUserId ?: return
        updateState {
            it.copy(contactPhoneLookups = it.contactPhoneLookups + (phone to ContactPhoneLookup(isLoading = true)))
        }
        viewModelScope.launch {
            val lookup = catchResult {
                val user = userRepository.findUserByPhone(phone)
                val relationship = user?.let { sendInvitationUseCase.checkRelationship(currentId, it.id) }
                ContactPhoneLookup(resolvedUser = user, relationship = relationship)
            }.getOrDefault(ContactPhoneLookup())
            updateState { it.copy(contactPhoneLookups = it.contactPhoneLookups + (phone to lookup)) }
        }
    }

    private fun contactCardPrimaryAction(phone: String) {
        val lookup = state.value.contactPhoneLookups[phone]
        val resolvedUser = lookup?.resolvedUser
        if (resolvedUser == null) {
            val text = "¡Únete a ChatApp y hablamos! 💬"
            viewModelScope.launch { sendEffect(ChatEffect.InviteContact(phone, text)) }
            return
        }
        viewModelScope.launch {
            when (val result = sendInvitationUseCase(resolvedUser)) {
                is SendInvitationResult.Sent -> {
                    updateState {
                        it.copy(contactPhoneLookups = it.contactPhoneLookups + (phone to lookup.copy(relationship = UserRelationship.PENDING_SENT)))
                    }
                    sendEffect(ChatEffect.ShowSnackbar("¡Invitación enviada!"))
                }
                is SendInvitationResult.AlreadySent -> {
                    updateState {
                        it.copy(contactPhoneLookups = it.contactPhoneLookups + (phone to lookup.copy(relationship = UserRelationship.PENDING_SENT)))
                    }
                    sendEffect(ChatEffect.ShowSnackbar("Invitación enviada · Pendiente de respuesta"))
                }
                is SendInvitationResult.PendingReceived -> {
                    updateState {
                        it.copy(contactPhoneLookups = it.contactPhoneLookups + (phone to lookup.copy(relationship = UserRelationship.PENDING_RECEIVED)))
                    }
                    sendEffect(ChatEffect.ShowSnackbar("Ya tienes una invitación pendiente de esta persona"))
                }
                is SendInvitationResult.NavigateToChat -> {
                    updateState {
                        it.copy(contactPhoneLookups = it.contactPhoneLookups + (phone to lookup.copy(relationship = UserRelationship.CONNECTED)))
                    }
                    sendEffect(ChatEffect.NavigateToConversation(result.conversationId, result.name))
                }
                is SendInvitationResult.Blocked -> {
                    updateState {
                        it.copy(contactPhoneLookups = it.contactPhoneLookups + (phone to lookup.copy(relationship = UserRelationship.BLOCKED)))
                    }
                    sendEffect(ChatEffect.ShowSnackbar("No puedes enviar una invitación a este contacto"))
                }
                is SendInvitationResult.Failure -> {
                    AppLogger.e(TAG, "contactCardPrimaryAction failed: ${result.message}")
                    sendEffect(ChatEffect.ShowSnackbar(result.message))
                }
            }
        }
    }

    /**
     * Starts observing a poll's question/options/current-user-vote, keeping
     * [ChatState.pollUiStates] up to date. Called from PollBubble (via intent)
     * the first time a `poll:<id>` message is rendered — idempotent per pollId
     * for the lifetime of this ViewModel.
     */
    private fun observePoll(pollId: String) {
        if (!observedPollIds.add(pollId)) return
        viewModelScope.launch {
            catchResult {
                pollRepository.observePollById(pollId).collect { poll ->
                    updateState { s ->
                        val current = s.pollUiStates[pollId] ?: PollUiState()
                        s.copy(pollUiStates = s.pollUiStates + (pollId to current.copy(poll = poll)))
                    }
                }
            }
        }
        viewModelScope.launch {
            catchResult {
                pollRepository.observeOptionsByPollId(pollId).collect { options ->
                    updateState { s ->
                        val current = s.pollUiStates[pollId] ?: PollUiState()
                        s.copy(pollUiStates = s.pollUiStates + (pollId to current.copy(options = options)))
                    }
                }
            }
        }
        val uid = currentUserId ?: return
        viewModelScope.launch {
            catchResult {
                pollRepository.observeVotes(pollId, uid).collect { votes ->
                    updateState { s ->
                        val current = s.pollUiStates[pollId] ?: PollUiState()
                        s.copy(pollUiStates = s.pollUiStates + (pollId to current.copy(userVotes = votes)))
                    }
                }
            }
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

    private fun handleContactSelected(uri: android.net.Uri) {
        viewModelScope.launch {
            catchResult {
                val contact = contactRepository.getContactByUri(uri.toString())
                if (contact != null) {
                    sendContact(contact.name, contact.phoneNumber)
                }
            }.onFailure { e ->
                AppLogger.e(TAG, "handleContactSelected failed", e)
                updateState { it.copy(error = "No se pudo leer el contacto") }
            }
        }
    }

    private fun sendContact(name: String, phone: String) {
        val userId = state.value.currentUserId ?: return
        val reply = state.value.replyingTo
        val content = "contact:{\"name\":${org.json.JSONObject.quote(name)},\"phone\":${org.json.JSONObject.quote(phone)}}"
        viewModelScope.launch {
            sendEffect(ChatEffect.ScrollToBottom)
            updateState { it.copy(replyingTo = null) }
            sendMessageUseCase(conversationId, userId, content,
                replyToId = reply?.id, replyToContent = reply?.replySnippet(), replyToSenderName = reply?.senderName,
            ).onFailure { e -> AppLogger.e(TAG, "sendContact failed", e); updateState { it.copy(error = e.message ?: "Error al enviar el contacto") } }
        }
    }

    private fun selectMention(member: GroupMemberBO) {
        val currentText = state.value.inputText
        val lastAtIndex = currentText.lastIndexOf('@')
        val newText = if (lastAtIndex >= 0) currentText.substring(0, lastAtIndex) + "@${member.username} "
                      else currentText + "@${member.username} "
        updateState { it.copy(inputText = newText, mentionSuggestions = emptyList(), showMentionSuggestions = false) }
    }

    @SuppressLint("MissingPermission")
    private fun fetchAndSendLocation() {
        val lm = application.getSystemService(LocationManager::class.java)
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        val location = providers.firstNotNullOfOrNull { provider ->
            runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
        }
        if (location != null) {
            val url = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
            sendLocationMessage(url)
        } else {
            viewModelScope.launch { sendEffect(ChatEffect.ShowSnackbar("No se pudo obtener la ubicacion")) }
        }
    }

    private fun sendLocationMessage(mapsUrl: String) {
        val userId = state.value.currentUserId ?: return
        val reply = state.value.replyingTo
        viewModelScope.launch {
            sendEffect(ChatEffect.ScrollToBottom)
            updateState { it.copy(replyingTo = null) }
            sendMessageUseCase(conversationId, userId, LocationMessageFormat.format(mapsUrl),
                replyToId = reply?.id, replyToContent = reply?.replySnippet(), replyToSenderName = reply?.senderName,
            ).onFailure { e -> updateState { it.copy(error = e.message ?: "Error") } }
        }
    }

    private var searchJob: Job? = null

    private fun searchMessages(query: String) {
        updateState { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) { updateState { it.copy(searchResults = emptyList(), isSearching = false) }; return }
        searchJob = viewModelScope.launch {
            updateState { it.copy(isSearching = true) }
            delay(300L)
            val uid = currentUserId ?: return@launch
            val results = catchResult { messageRepository.searchMessages(conversationId, uid, query) }.getOrDefault(emptyList())
            updateState { it.copy(searchResults = results, isSearching = false) }
        }
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

    private fun jumpToMessage(messageId: String) {
        updateState { it.copy(isSearchActive = false, searchQuery = "", searchResults = emptyList(), highlightedMessageId = messageId) }
        viewModelScope.launch {
            delay(2_000)
            updateState { if (it.highlightedMessageId == messageId) it.copy(highlightedMessageId = null) else it }
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

    private fun sendImages(uris: List<Uri>) {
        val userId = state.value.currentUserId ?: return
        val reply = state.value.replyingTo
        viewModelScope.launch {
            sendEffect(ChatEffect.ScrollToBottom)
            updateState { it.copy(replyingTo = null) }
            val metadataByUri = uris.map { uri ->
                val metadata = catchResult {
                    withContext(Dispatchers.IO) { getUriMetadataUseCase(uri.toString()) }
                }.getOrNull()
                uri to metadata
            }
            val totalBytes = metadataByUri.sumOf { (_, metadata) -> metadata?.size?.takeIf { it >= 0 } ?: 0L }
            // Only smooth batches that would render as ImageGroupBubble (>2 images) — that's
            // where the per-message paging updates cause the reported bubble-shape jumping.
            // Single/double sends already render fine as each message lands.
            val showBatchPlaceholder = metadataByUri.size > 2
            updateState {
                it.copy(
                    mediaUploadProgress = MediaUploadProgress(totalCount = metadataByUri.size, completedCount = 0, totalBytes = totalBytes),
                    pendingImageUris = if (showBatchPlaceholder) metadataByUri.map { (uri, _) -> uri } else emptyList(),
                    suppressedImageMessageIds = emptySet(),
                )
            }
            for ((index, entry) in metadataByUri.withIndex()) {
                val (uri, metadata) = entry
                val bytes = catchResult {
                    readUriAsBytesUseCase(uri.toString())
                }.getOrNull()
                if (bytes != null) {
                    catchResult {
                        val mimeType = metadata?.mimeType ?: "image/jpeg"
                        val imageUrl = messageRepository.uploadImage(conversationId, bytes, mimeType)
                        val replyForImage = if (index == 0) reply else null
                        sendMessageUseCase(conversationId, userId, "", imageUrl,
                            replyToId = replyForImage?.id, replyToContent = replyForImage?.replySnippet(), replyToSenderName = replyForImage?.senderName,
                        ).getOrThrow()
                    }.onSuccess { message ->
                        if (showBatchPlaceholder) {
                            updateState { it.copy(suppressedImageMessageIds = it.suppressedImageMessageIds + message.id) }
                        }
                    }.onFailure { e -> AppLogger.e(TAG, "sendImages failed", e); updateState { it.copy(error = e.message ?: "Error uploading image") } }
                } else {
                    AppLogger.e(TAG, "sendImages: could not read bytes for $uri")
                    updateState { it.copy(error = "No se pudo leer la imagen") }
                }
                updateState { it.copy(mediaUploadProgress = it.mediaUploadProgress?.copy(completedCount = index + 1)) }
            }
            updateState { it.copy(mediaUploadProgress = null, pendingImageUris = emptyList(), suppressedImageMessageIds = emptySet()) }
        }
    }

    private fun startRecording() {
        val outputFilePath = java.io.File.createTempFile("audio_", ".m4a", application.cacheDir).absolutePath
        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(application)
                  else @Suppress("DEPRECATION") MediaRecorder()
        catchResult {
            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFilePath)
                prepare(); start()
            }
            recorder = rec
            updateState { it.copy(audioState = AudioState(isRecording = true, pendingFilePath = outputFilePath)) }
            val startMs = System.currentTimeMillis()
            recordingTimerJob = viewModelScope.launch {
                while (true) {
                    delay(50)
                    val elapsed = System.currentTimeMillis() - startMs
                    val amp = catchResult { (recorder?.maxAmplitude ?: 0).toFloat() / 32767f }.getOrDefault(0f)
                    updateState { s ->
                        // Keep the full history (not just a recent window) so the post-recording
                        // preview waveform can reflect the whole recording, not just its tail.
                        val newHistory = s.audioState.amplitudeHistory + amp
                        s.copy(audioState = s.audioState.copy(recordingDurationMs = elapsed, amplitudeHistory = newHistory))
                    }
                }
            }
        }.onFailure { e ->
            AppLogger.e(TAG, "Recording failed", e)
            catchResult { rec.release() }
            updateState { it.copy(error = "No se pudo iniciar la grabacion") }
        }
    }

    private fun stopRecording() {
        recordingTimerJob?.cancel(); recordingTimerJob = null
        val durationMs = state.value.audioState.recordingDurationMs
        catchResult { recorder?.apply { stop(); release() } }; recorder = null
        updateState { it.copy(audioState = it.audioState.copy(isRecording = false, recordingDurationMs = durationMs)) }
    }

    private fun discardAudio() {
        state.value.audioState.pendingFilePath?.let { path ->
            catchResult { java.io.File(path).delete() }
        }
        updateState { it.copy(audioState = AudioState()) }
    }

    private fun sendAudio() {
        val userId = state.value.currentUserId ?: return
        val filePath = state.value.audioState.pendingFilePath ?: return
        val durationMs = state.value.audioState.recordingDurationMs
        val reply = state.value.replyingTo
        viewModelScope.launch {
            sendEffect(ChatEffect.ScrollToBottom)
            updateState { it.copy(audioState = it.audioState.copy(isUploading = true), replyingTo = null) }
            draftSaveJob?.cancel(); draftRepository.saveDraft(conversationId, "")
            catchResult {
                val bytes = withContext(Dispatchers.IO) { java.io.File(filePath).readBytes() }
                val audioUrl = messageRepository.uploadAudio(conversationId, bytes)
                sendMessageUseCase(conversationId, userId, "", audioUrl = audioUrl, audioDurationMs = durationMs,
                    replyToId = reply?.id, replyToContent = reply?.replySnippet(), replyToSenderName = reply?.senderName,
                )
                catchResult { java.io.File(filePath).delete() }
                updateState { it.copy(audioState = AudioState()) }
            }.onFailure { e ->
                updateState { it.copy(audioState = it.audioState.copy(isUploading = false), error = e.message ?: "Error al enviar el audio") }
            }
        }
    }

    private fun sendFile(uri: Uri) {
        val userId = state.value.currentUserId ?: return
        val reply = state.value.replyingTo
        viewModelScope.launch {
            sendEffect(ChatEffect.ScrollToBottom)
            updateState { it.copy(isUploadingFile = true, replyingTo = null) }
            catchResult {
                val metadata = withContext(Dispatchers.IO) { getUriMetadataUseCase(uri.toString()) }
                val mimeType = metadata.mimeType ?: "application/octet-stream"
                val displayName = metadata.displayName ?: "archivo"
                val fileSize = metadata.size
                val bytes = readUriAsBytesUseCase(uri.toString())
                val fileUrl = messageRepository.uploadFile(conversationId, bytes, displayName, mimeType)
                sendMessageUseCase(conversationId, userId, "", fileUrl = fileUrl, fileName = displayName,
                    fileSize = fileSize, fileMimeType = mimeType,
                    replyToId = reply?.id, replyToContent = reply?.replySnippet(), replyToSenderName = reply?.senderName,
                ).getOrThrow()
            }.onFailure { e -> AppLogger.e(TAG, "sendFile failed", e); updateState { it.copy(error = e.message ?: "Error al enviar el archivo") } }
            updateState { it.copy(isUploadingFile = false) }
        }
    }

    private fun sendVideo(uri: Uri) {
        val userId = state.value.currentUserId ?: return
        val reply = state.value.replyingTo
        viewModelScope.launch {
            sendEffect(ChatEffect.ScrollToBottom)
            updateState { it.copy(isUploadingFile = true, replyingTo = null) }
            catchResult {
                val fileSize = withContext(Dispatchers.IO) { getUriMetadataUseCase(uri.toString()) }.size
                check(fileSize == null || fileSize <= UploadLimits.VIDEO_MAX_BYTES) {
                    "El video supera el tamaño máximo permitido (50 MB)"
                }
                val bytes = readUriAsBytesUseCase(uri.toString())
                val videoUrl = messageRepository.uploadVideo(conversationId, bytes)
                sendMessageUseCase(conversationId, userId, "", videoUrl = videoUrl,
                    replyToId = reply?.id, replyToContent = reply?.replySnippet(), replyToSenderName = reply?.senderName,
                ).getOrThrow()
            }.onFailure { e -> AppLogger.e(TAG, "sendVideo failed", e); updateState { it.copy(error = e.message ?: "Error al enviar el video") } }
            updateState { it.copy(isUploadingFile = false) }
        }
    }

    private fun startCall(typeStr: String) {
        val callType = if (typeStr == "video") CallType.VIDEO else CallType.AUDIO
        val isGroup = state.value.isGroup
        viewModelScope.launch {
            catchResult {
                val call = if (isGroup) callRepository.createGroupCall(conversationId, callType)
                           else callRepository.createCall(conversationId, state.value.otherUserId ?: return@catchResult, callType)
                sendEffect(ChatEffect.NavigateToCall(call))
            }.onFailure { e -> updateState { it.copy(error = e.message ?: "Error al iniciar la llamada") } }
        }
    }

    private fun sendGif(url: String) {
        val userId = state.value.currentUserId ?: return
        val reply = state.value.replyingTo
        viewModelScope.launch {
            sendEffect(ChatEffect.ScrollToBottom)
            updateState { it.copy(showStickerPicker = false, replyingTo = null) }
            sendMessageUseCase(conversationId, userId, "", gifUrl = url,
                replyToId = reply?.id, replyToContent = reply?.replySnippet(), replyToSenderName = reply?.senderName,
            ).onFailure { e -> updateState { it.copy(error = e.message ?: "Error al enviar el GIF") } }
        }
    }

    private fun sendSticker(emoji: String) {
        val userId = state.value.currentUserId ?: return
        val reply = state.value.replyingTo
        viewModelScope.launch {
            sendEffect(ChatEffect.ScrollToBottom)
            updateState { it.copy(showStickerPicker = false, replyingTo = null) }
            sendMessageUseCase(conversationId, userId, "", stickerUrl = emoji,
                replyToId = reply?.id, replyToContent = reply?.replySnippet(), replyToSenderName = reply?.senderName,
            ).onFailure { e -> updateState { it.copy(error = e.message ?: "Error al enviar el sticker") } }
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

    private fun showForwardDialog(message: MessageBO) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            catchResult {
                val conversations = conversationRepository.observeConversations(uid).first().filter { it.id != conversationId }
                updateState { it.copy(showForwardDialog = true, forwardingMessage = message, forwardableConversations = conversations) }
            }.onFailure { updateState { it.copy(error = "No se pudo cargar las conversaciones") } }
        }
    }

    private fun forwardMessage(targetConversationId: String) {
        val uid = currentUserId ?: return
        val message = state.value.forwardingMessage ?: return
        updateState { it.copy(showForwardDialog = false, forwardingMessage = null, forwardableConversations = emptyList()) }
        viewModelScope.launch {
            catchResult {
                messageRepository.sendMessage(conversationId = targetConversationId, senderId = uid, content = message.content,
                    imageUrl = message.imageUrl, audioUrl = message.audioUrl, audioDurationMs = message.audioDurationMs,
                    gifUrl = message.gifUrl, stickerUrl = message.stickerUrl,
                )
                sendEffect(ChatEffect.ShowSnackbar("Mensaje reenviado"))
            }.onFailure { updateState { it.copy(error = "No se pudo reenviar") } }
        }
    }

    private fun showForwardSelectionDialog() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            catchResult {
                val conversations = conversationRepository.observeConversations(uid).first().filter { it.id != conversationId }
                updateState { it.copy(showForwardSelectionDialog = true, forwardableConversations = conversations) }
            }.onFailure { updateState { it.copy(error = "No se pudo cargar las conversaciones") } }
        }
    }

    private fun forwardSelectedMessages(targetConversationId: String) {
        val uid = currentUserId ?: return
        val ids = state.value.selectedMessageIds.toSet()
        updateState { it.copy(showForwardSelectionDialog = false, forwardableConversations = emptyList(), selectedMessageIds = emptySet()) }
        viewModelScope.launch {
            val allMessages = catchResult { withContext(Dispatchers.IO) { messageRepository.getAllMessages(conversationId, uid) } }.getOrDefault(emptyList())
            val toForward = allMessages.filter { it.id in ids }
            var forwarded = 0
            for (message in toForward) {
                catchResult {
                    messageRepository.sendMessage(conversationId = targetConversationId, senderId = uid, content = message.content,
                        imageUrl = message.imageUrl, audioUrl = message.audioUrl, audioDurationMs = message.audioDurationMs,
                    gifUrl = message.gifUrl, stickerUrl = message.stickerUrl,
                    )
                    forwarded++
                }
            }
            if (forwarded > 0) sendEffect(ChatEffect.ShowSnackbar("$forwarded mensaje(s) reenviado(s)"))
            else updateState { it.copy(error = "No se pudieron reenviar") }
        }
    }

    private fun createPoll(question: String, options: List<String>, allowMultiple: Boolean) {
        val userId = state.value.currentUserId ?: return
        updateState { it.copy(showCreatePollSheet = false) }
        viewModelScope.launch {
            catchResult {
                val pollId = pollRepository.createPoll(
                    conversationId = conversationId,
                    question = question,
                    createdBy = userId,
                    options = options,
                    allowMultiple = allowMultiple,
                )
                sendMessageUseCase(conversationId, userId, "poll:$pollId")
            }.onFailure { e -> AppLogger.e(TAG, "createPoll failed", e); updateState { it.copy(error = "No se pudo crear la encuesta") } }
        }
    }

    private fun votePoll(pollId: String, optionId: String) {
        val userId = state.value.currentUserId ?: return
        viewModelScope.launch {
            catchResult { pollRepository.vote(pollId, userId, optionId) }
                .onFailure { e -> AppLogger.e(TAG, "votePoll failed", e); updateState { it.copy(error = "No se pudo registrar el voto") } }
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

    private fun transcribeAudio(messageId: String) {
        viewModelScope.launch {
            val result = catchResult { audioTranscriber.transcribeFromMic() }.getOrDefault("Transcripcion no disponible")
            updateState { s -> s.copy(audioTranscriptions = s.audioTranscriptions + (messageId to result)) }
        }
    }

    private fun translateMessage(messageId: String, text: String) {
        if (messageId in state.value.translatingMessageIds) return
        updateState { it.copy(translatingMessageIds = it.translatingMessageIds + messageId) }
        viewModelScope.launch {
            catchResult { translationManager.translate(text) }
                .onSuccess { translated ->
                    updateState { it.copy(translatedTexts = it.translatedTexts + (messageId to translated), translatingMessageIds = it.translatingMessageIds - messageId) }
                }
                .onFailure { updateState { it.copy(translatingMessageIds = it.translatingMessageIds - messageId, error = "No se pudo traducir") } }
        }
    }

    private fun toggleIncognito() {
        if (state.value.isIncognito) viewModelScope.launch { incognitoRepository.setIncognito(conversationId, false) }
        else updateState { it.copy(showIncognitoInfoDialog = true) }
    }

    private fun confirmIncognito() {
        updateState { it.copy(showIncognitoInfoDialog = false) }
        viewModelScope.launch { incognitoRepository.setIncognito(conversationId, true) }
    }

    private fun scheduleMessage(scheduledAt: Long) {
        val text = state.value.inputText.trim()
        val userId = state.value.currentUserId
        if (userId == null || text.isBlank()) {
            updateState { it.copy(showScheduleDialog = false, error = "Escribe un mensaje antes de programarlo") }
            return
        }
        updateState { it.copy(showScheduleDialog = false, inputText = "", scheduledAtMs = scheduledAt) }
        draftSaveJob?.cancel()
        viewModelScope.launch {
            draftRepository.saveDraft(conversationId, "")
            val msgId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            catchResult {
                scheduledMessageRepository.schedule(
                    id = msgId,
                    conversationId = conversationId,
                    senderId = userId,
                    text = text,
                    scheduledAtMs = scheduledAt,
                    createdAt = now,
                )
            }
                .onSuccess {
                    val delayMs = (scheduledAt - System.currentTimeMillis()).coerceAtLeast(0L)
                    workManager.enqueue(
                        OneTimeWorkRequestBuilder<ScheduledMessageWorker>()
                            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                            .addTag(ScheduledMessageWorker.WORK_TAG).build()
                    )
                    sendEffect(ChatEffect.ShowSnackbar("Mensaje programado"))
                }
                .onFailure { updateState { it.copy(error = "No se pudo programar el mensaje", inputText = text) } }
        }
    }

    private fun cancelScheduledMessage(id: String) {
        viewModelScope.launch {
            workManager.cancelAllWorkByTag("scheduled_$id")
            catchResult { scheduledMessageRepository.deleteById(id) }
        }
    }

    private fun aiSummarize() {
        val uid = currentUserId ?: return
        updateState { it.copy(isAiLoading = true) }
        viewModelScope.launch {
            val snippets = catchResult { messageRepository.getAllMessages(conversationId, uid).takeLast(20).map { it.content } }.getOrDefault(emptyList())
            aiAssistantRepository.summarize(snippets)
                .onSuccess { result -> updateState { it.copy(aiSuggestion = result, isAiLoading = false) } }
                .onFailure { e -> updateState { it.copy(isAiLoading = false, error = e.message) } }
        }
    }

    private fun aiSuggestReply() {
        val uid = currentUserId ?: return
        updateState { it.copy(isAiLoading = true) }
        viewModelScope.launch {
            val last = catchResult { messageRepository.getAllMessages(conversationId, uid).lastOrNull { it.senderId != uid }?.content ?: "" }.getOrDefault("")
            aiAssistantRepository.suggestReply(last)
                .onSuccess { result -> updateState { it.copy(aiSuggestion = result, isAiLoading = false) } }
                .onFailure { e -> updateState { it.copy(isAiLoading = false, error = e.message) } }
        }
    }

    private fun aiFreeform(prompt: String) {
        updateState { it.copy(isAiLoading = true) }
        viewModelScope.launch {
            aiAssistantRepository.freeform(prompt)
                .onSuccess { result -> updateState { it.copy(aiSuggestion = result, isAiLoading = false) } }
                .onFailure { e -> updateState { it.copy(isAiLoading = false, error = e.message) } }
        }
    }

    override fun onCleared() {
        super.onCleared()
        recordingTimerJob?.cancel(); remoteSyncJob?.cancel(); typingResetJob?.cancel()
        typingPresenceJob?.cancel(); draftSaveJob?.cancel()
        viewModelScope.launch {
            withContext(NonCancellable) { catchResult { typingRepository.close(conversationId) } }
        }
        catchResult { recorder?.apply { stop(); release() } }; recorder = null
    }

    companion object {
        private const val TAG = "ChatViewModel"
    }
}
