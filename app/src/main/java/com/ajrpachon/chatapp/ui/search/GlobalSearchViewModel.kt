package com.ajrpachon.chatapp.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.data.local.dao.ConversationDao
import com.ajrpachon.chatapp.data.local.dao.MessageDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GlobalSearchResultItem(
    val messageId: String,
    val conversationId: String,
    val conversationName: String,
    val content: String,
    val createdAtMs: Long,
)

data class GlobalSearchState(
    val query: String = "",
    val results: List<GlobalSearchResultItem> = emptyList(),
    val isLoading: Boolean = false,
)

sealed class GlobalSearchIntent {
    data class QueryChanged(val query: String) : GlobalSearchIntent()
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class GlobalSearchViewModel(
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _state = MutableStateFlow(GlobalSearchState())
    val state: StateFlow<GlobalSearchState> = _state

    init {
        viewModelScope.launch {
            _query
                .debounce(300)
                .flatMapLatest { query ->
                    flow {
                        if (query.length < 2) {
                            emit(GlobalSearchState(query = query))
                            return@flow
                        }
                        emit(GlobalSearchState(query = query, isLoading = true))
                        val messages = messageDao.searchAllMessages(query)
                        val results = messages.map { msg ->
                            val conversationName = conversationDao.getById(msg.conversationId)?.name ?: "Chat"
                            GlobalSearchResultItem(
                                messageId = msg.id,
                                conversationId = msg.conversationId,
                                conversationName = conversationName,
                                content = msg.content,
                                createdAtMs = msg.createdAt,
                            )
                        }
                        emit(GlobalSearchState(query = query, results = results, isLoading = false))
                    }
                }
                .collect { _state.value = it }
        }
    }

    fun onIntent(intent: GlobalSearchIntent) {
        when (intent) {
            is GlobalSearchIntent.QueryChanged -> {
                _query.value = intent.query
                _state.update { it.copy(query = intent.query) }
            }
        }
    }
}
