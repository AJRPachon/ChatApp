package com.ajrpachon.chatapp.ui.conversations

import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import com.ajrpachon.chatapp.domain.repository.DraftRepository
import com.ajrpachon.chatapp.domain.repository.ThemeRepository
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.domain.usecase.LeaveGroupUseCase
import com.ajrpachon.chatapp.domain.usecase.ObserveConversationsUseCase
import com.ajrpachon.chatapp.domain.usecase.ObserveInvitationsUseCase
import com.ajrpachon.chatapp.service.FcmTokenManager
import com.ajrpachon.chatapp.service.PresenceManager
import com.ajrpachon.chatapp.ui.common.BaseViewModel
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.NetworkMonitor
import com.ajrpachon.chatapp.utils.catchResult
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

@Suppress("LongParameterList")
class ConversationListViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val observeConversationsUseCase: ObserveConversationsUseCase,
    private val observeInvitationsUseCase: ObserveInvitationsUseCase,
    private val conversationRepository: ConversationRepository,
    private val leaveGroupUseCase: LeaveGroupUseCase,
    private val fcmTokenManager: FcmTokenManager,
    private val presenceManager: PresenceManager,
    private val draftRepository: DraftRepository,
    private val notificationSoundRepository: com.ajrpachon.chatapp.domain.repository.NotificationSoundRepository,
    private val networkMonitor: NetworkMonitor,
    private val themeRepository: ThemeRepository,
) : BaseViewModel<ConversationListState, ConversationListEffect>(ConversationListState()) {

    init {
        presenceManager.start()
        viewModelScope.launch { fcmTokenManager.syncToken() }
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                updateState { it.copy(isOnline = online) }
            }
        }
        viewModelScope.launch {
            themeRepository.observe().collect { theme ->
                updateState { it.copy(themePreference = theme) }
            }
        }
        viewModelScope.launch {
            draftRepository.getAllDrafts().collect { drafts ->
                updateState { it.copy(drafts = drafts) }
            }
        }
        viewModelScope.launch {
            catchResult {
                val user = getCurrentUserUseCase().filterNotNull().first()
                updateState { it.copy(currentUserId = user.id, isLoading = false) }
                supervisorScope {
                    launch {
                        observeInvitationsUseCase(user.id).collect { invitations ->
                            updateState { it.copy(pendingInvitationsCount = invitations.size) }
                        }
                    }
                    launch {
                        observeConversationsUseCase(user.id).collect { convs ->
                            updateState { current ->
                                val sorted = sortedConversations(convs, current.sortByUnread)
                                current.copy(
                                    conversations = sorted,
                                    filteredConversations = applyFilter(sorted, current.searchQuery, current.selectedFilter),
                                )
                            }
                        }
                    }
                    launch {
                        conversationRepository.observeArchivedConversations(user.id).collect { convs ->
                            updateState { it.copy(archivedConversations = convs) }
                        }
                    }
                }
            }.onFailure { e ->
                AppLogger.e("ConversationListViewModel", "Observe conversations failed", e)
                updateState { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun onIntent(intent: ConversationListIntent) {
        when (intent) {
            is ConversationListIntent.OpenConversation ->
                sendEffect(ConversationListEffect.NavigateToChat(intent.conversationId, intent.conversationName, intent.isGroup))
            is ConversationListIntent.DismissError ->
                updateState { it.copy(error = null) }
            is ConversationListIntent.DeleteConversation ->
                viewModelScope.launch {
                    catchResult { conversationRepository.deleteConversation(intent.conversationId) }
                        .onFailure { e -> updateState { it.copy(error = e.message) } }
                }
            is ConversationListIntent.ToggleMute ->
                viewModelScope.launch {
                    catchResult { conversationRepository.toggleMute(intent.conversationId, intent.muted) }
                        .onFailure { e -> updateState { it.copy(error = e.message) } }
                }
            is ConversationListIntent.ClearChat ->
                viewModelScope.launch {
                    catchResult { conversationRepository.clearChat(intent.conversationId) }
                        .onFailure { e -> updateState { it.copy(error = e.message) } }
                }
            is ConversationListIntent.LeaveGroup ->
                viewModelScope.launch {
                    val userId = state.value.currentUserId ?: return@launch
                    leaveGroupUseCase(intent.conversationId, userId)
                        .onFailure { e -> updateState { it.copy(error = e.message) } }
                }
            is ConversationListIntent.ToggleSortByUnread -> {
                updateState { current ->
                    val newSort = !current.sortByUnread
                    val sorted = sortedConversations(current.conversations, newSort)
                    current.copy(
                        sortByUnread = newSort,
                        conversations = sorted,
                        filteredConversations = applyFilter(sorted, current.searchQuery, current.selectedFilter),
                    )
                }
            }
            is ConversationListIntent.SetFilter ->
                updateState { current ->
                    val newFilter = if (current.selectedFilter == intent.filter) ConversationFilter.ALL else intent.filter
                    current.copy(
                        selectedFilter = newFilter,
                        filteredConversations = applyFilter(current.conversations, current.searchQuery, newFilter),
                    )
                }
            is ConversationListIntent.SearchQueryChanged ->
                updateState { current ->
                    current.copy(
                        searchQuery = intent.query,
                        filteredConversations = applyFilter(current.conversations, intent.query, current.selectedFilter),
                    )
                }
            is ConversationListIntent.ToggleSearch ->
                updateState { current ->
                    if (current.isSearchActive) {
                        current.copy(
                            isSearchActive = false,
                            searchQuery = "",
                            filteredConversations = applyFilter(current.conversations, "", current.selectedFilter),
                        )
                    } else {
                        current.copy(isSearchActive = true)
                    }
                }
            is ConversationListIntent.ArchiveConversation ->
                viewModelScope.launch {
                    catchResult { conversationRepository.archiveConversation(intent.conversationId, intent.archived) }
                        .onFailure { e -> updateState { it.copy(error = e.message) } }
                }
            is ConversationListIntent.ShowArchivedSheet ->
                updateState { it.copy(showArchivedSheet = true) }
            is ConversationListIntent.DismissArchivedSheet ->
                updateState { it.copy(showArchivedSheet = false) }
            is ConversationListIntent.ShowSoundPicker ->
                updateState { it.copy(soundPickerConversationId = intent.conversationId) }
            is ConversationListIntent.DismissSoundPicker ->
                updateState { it.copy(soundPickerConversationId = null) }
            is ConversationListIntent.SetNotificationSound ->
                viewModelScope.launch {
                    catchResult { notificationSoundRepository.set(intent.conversationId, intent.sound) }
                        .onFailure { e -> updateState { it.copy(error = e.message) } }
                    updateState { it.copy(soundPickerConversationId = null) }
                }
            is ConversationListIntent.SetTheme ->
                viewModelScope.launch {
                    catchResult { themeRepository.set(intent.theme) }
                        .onFailure { e -> updateState { it.copy(error = e.message) } }
                }
        }
    }

    private fun applyFilter(
        convs: List<com.ajrpachon.chatapp.domain.model.ConversationBO>,
        searchQuery: String,
        filter: ConversationFilter,
    ): List<com.ajrpachon.chatapp.domain.model.ConversationBO> {
        val bySearch = if (searchQuery.isBlank()) convs
        else convs.filter { it.name.contains(searchQuery, ignoreCase = true) }
        return when (filter) {
            ConversationFilter.ALL -> bySearch
            ConversationFilter.UNREAD -> bySearch.filter { it.unreadCount > 0 }
            ConversationFilter.GROUPS -> bySearch.filter { it.isGroup }
            ConversationFilter.DIRECT -> bySearch.filter { !it.isGroup }
        }
    }

    private fun sortedConversations(
        convs: List<com.ajrpachon.chatapp.domain.model.ConversationBO>,
        sortByUnread: Boolean,
    ): List<com.ajrpachon.chatapp.domain.model.ConversationBO> {
        return if (sortByUnread) {
            val withUnread = convs.filter { it.unreadCount > 0 }.sortedByDescending { it.unreadCount }
            val withoutUnread = convs.filter { it.unreadCount == 0 }.sortedByDescending { it.updatedAt }
            withUnread + withoutUnread
        } else {
            convs.sortedByDescending { it.updatedAt }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // presenceManager.close() only cancels its internal CoroutineScope (no suspending
        // I/O of its own), so it is safe to call synchronously here, unlike typingRepository.close()
        // in ChatViewModel which performs a network call and needs NonCancellable.
        presenceManager.close()
    }
}
