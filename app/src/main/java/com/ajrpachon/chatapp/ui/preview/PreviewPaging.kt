package com.ajrpachon.chatapp.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.flowOf

private val previewLoadStates = LoadStates(
    refresh = LoadState.NotLoading(endOfPaginationReached = true),
    prepend = LoadState.NotLoading(endOfPaginationReached = true),
    append = LoadState.NotLoading(endOfPaginationReached = true),
)

/** Static [LazyPagingItems] for previews of composables that take paged data. */
@Composable
internal fun <T : Any> rememberPreviewPagingItems(items: List<T>): LazyPagingItems<T> {
    val flow = remember(items) { flowOf(PagingData.from(items, previewLoadStates)) }
    return flow.collectAsLazyPagingItems()
}
