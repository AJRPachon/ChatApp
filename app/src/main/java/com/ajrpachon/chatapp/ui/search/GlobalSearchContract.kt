package com.ajrpachon.chatapp.ui.search

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
