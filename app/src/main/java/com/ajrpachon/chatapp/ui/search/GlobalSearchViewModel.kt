package com.ajrpachon.chatapp.ui.search

import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.ui.common.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class GlobalSearchViewModel(
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository,
) : BaseViewModel<GlobalSearchState, Nothing>(GlobalSearchState()) {

    init {
        viewModelScope.launch {
            state
                .map { it.query }
                .distinctUntilChanged()
                .debounce(300)
                .flatMapLatest { query ->
                    flow {
                        if (query.length < 2) {
                            emit(GlobalSearchState(query = query))
                            return@flow
                        }
                        emit(GlobalSearchState(query = query, isLoading = true))
                        val messages = messageRepository.searchAllMessages(query)
                        val results = messages.map { msg ->
                            val conversationName = conversationRepository.getById(msg.conversationId)?.name ?: "Chat"
                            GlobalSearchResultItem(
                                messageId = msg.id,
                                conversationId = msg.conversationId,
                                conversationName = conversationName,
                                content = msg.content,
                                createdAtMs = msg.createdAt.toEpochMilliseconds(),
                            )
                        }
                        emit(GlobalSearchState(query = query, results = results, isLoading = false))
                    }
                }
                .collect { newState -> updateState { newState } }
        }
    }

    fun onIntent(intent: GlobalSearchIntent) {
        when (intent) {
            is GlobalSearchIntent.QueryChanged -> updateState { it.copy(query = intent.query) }
        }
    }
}
