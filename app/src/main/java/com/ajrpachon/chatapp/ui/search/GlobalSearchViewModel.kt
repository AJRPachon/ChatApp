package com.ajrpachon.chatapp.ui.search

import com.ajrpachon.chatapp.data.local.dao.ConversationDao
import com.ajrpachon.chatapp.data.local.dao.MessageDao
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.ui.common.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
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
) : BaseViewModel<GlobalSearchState, Nothing>(GlobalSearchState()) {

    private val _query = MutableStateFlow("")

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
                .collect { newState -> updateState { newState } }
        }
    }

    fun onIntent(intent: GlobalSearchIntent) {
        when (intent) {
            is GlobalSearchIntent.QueryChanged -> {
                _query.value = intent.query
                updateState { it.copy(query = intent.query) }
            }
        }
    }
}
