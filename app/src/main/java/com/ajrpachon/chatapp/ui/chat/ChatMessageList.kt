package com.ajrpachon.chatapp.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.model.ReactionBO
import com.ajrpachon.chatapp.ui.components.ChatMessagesSkeleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * [ChatScreen]'s `Scaffold` content: the message list itself (paged, with the pending-image-
 * batch placeholder and image-group/suppressed-message logic), the loading skeleton, the
 * scroll-to-bottom FAB, and the search overlay. Extracted per
 * docs/chat-viewmodel-decomposition.md Phase 2.
 *
 * [viewerUrls]/[viewerInitialIndex]/[showViewer]/[reactionDetailMessageId] are [MutableState]
 * because [ChatDialogHost] (rendered elsewhere in ChatScreen) owns the image viewer and
 * reaction-details sheet these bubbles open — same reasoning as ChatDialogHost's own doc.
 */
@Composable
internal fun ChatMessageList(
    state: ChatState,
    vm: ChatViewModel,
    lazyPagingItems: LazyPagingItems<MessageBO>,
    listState: LazyListState,
    scope: CoroutineScope,
    reactions: Map<String, List<ReactionBO>>,
    chatThemeColors: ChatThemeColors,
    highlightedMessageId: String?,
    showScrollToBottom: Boolean,
    innerPadding: PaddingValues,
    onScrollToMessage: (String) -> Unit,
    onOpenPdf: (url: String, filename: String) -> Unit,
    viewerUrls: MutableState<List<String>>,
    viewerInitialIndex: MutableState<Int>,
    showViewer: MutableState<Boolean>,
    reactionDetailMessageId: MutableState<String?>,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                state.wallpaperColor?.let { Color(it) }
                    ?: MaterialTheme.colorScheme.background
            )
    ) {
        val isInitialLoad = lazyPagingItems.loadState.refresh is LoadState.Loading
            && lazyPagingItems.itemCount == 0
        if (isInitialLoad) {
            ChatMessagesSkeleton(
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 8.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
            )
        }
        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 8.dp,
                start = 16.dp,
                end = 16.dp,
            ),
            // No verticalArrangement spacing here on purpose: hidden items (suppressed
            // in-flight batch messages, isInsideGroup images) render nothing but still
            // occupy a slot for key stability. Arrangement.spacedBy adds its gap between
            // every pair of items regardless of their rendered height, so N hidden items
            // would stack N gaps of empty space. Spacing is applied per-item below instead,
            // only on items that actually render content.
        ) {
            // reverseLayout=true → the first item in this content lambda sits at the visual
            // bottom (newest). Rendering the placeholder here keeps it "below" real messages
            // without needing negative indices into the paging data.
            if (state.pendingImageUris.isNotEmpty()) {
                item(key = "pending-image-batch") {
                    Box(Modifier.padding(top = 8.dp)) {
                        PendingImageBatchBubble(
                            uris = state.pendingImageUris,
                            progress = state.mediaUploadProgress,
                        )
                    }
                }
            }
            items(
                count = lazyPagingItems.itemCount,
                key = lazyPagingItems.itemKey { it.id },
            ) { index ->
                val message = lazyPagingItems[index] ?: return@items
                // Messages from the batch currently rendered by the placeholder above are
                // hidden here to avoid a duplicate/jumping bubble while uploads are in flight.
                if (message.id in state.suppressedImageMessageIds) return@items

                // Determine if this item is already covered by a group rendered at a lower index.
                // With reverseLayout=true and DESC order, index 0 = newest (bottom).
                // Index i-1 was composed before i, so it's already in the snapshot.
                // Skip back over any suppressed (in-flight batch) messages so an older, already
                // finished group isn't mistakenly treated as continuing into the new batch.
                var prevIndex = index - 1
                while (prevIndex >= 0 && lazyPagingItems[prevIndex]?.id in state.suppressedImageMessageIds) prevIndex--
                val prevMessage = if (prevIndex >= 0) lazyPagingItems[prevIndex] else null
                val isInsideGroup = prevMessage != null
                    && message.imageUrl != null && message.audioUrl == null
                    && prevMessage.imageUrl != null && prevMessage.audioUrl == null
                    && prevMessage.senderId == message.senderId

                if (!isInsideGroup) {
                Box(Modifier.padding(top = 8.dp)) {
                    val isImageGroupStart = message.imageUrl != null && message.audioUrl == null
                    if (isImageGroupStart) {
                        // Collect consecutive images from the same sender starting at this index.
                        // Accessing lazyPagingItems[j] triggers loading of the next page if j is
                        // near the page boundary — ensuring the group is always complete.
                        val group = mutableListOf(message)
                        var j = index + 1
                        while (j < lazyPagingItems.itemCount) {
                            val next = lazyPagingItems[j] ?: break
                            if (next.id in state.suppressedImageMessageIds) break
                            if (next.imageUrl != null && next.audioUrl == null && next.senderId == message.senderId) {
                                group.add(next)
                                j++
                            } else break
                        }
                        if (group.size > 2) {
                            ImageGroupBubble(
                                messages = group,
                                onImageClick = { idx ->
                                    viewerUrls.value = group.mapNotNull { it.imageUrl }
                                    viewerInitialIndex.value = idx
                                    showViewer.value = true
                                },
                                onReply = { vm.onIntent(ChatIntent.SetReply(group.first())) },
                            )
                        } else {
                            MessageBubble(
                                message = message,
                                isGroup = state.isGroup,
                                onImageClick = { url ->
                                    viewerUrls.value = listOf(url)
                                    viewerInitialIndex.value = 0
                                    showViewer.value = true
                                },
                                onReply = { vm.onIntent(ChatIntent.SetReply(message)) },
                                isHighlighted = message.id == highlightedMessageId,
                                onReplyClick = onScrollToMessage,
                                onDelete = if (message.isFromMe) {{ vm.onIntent(ChatIntent.DeleteMessage(message.id)) }} else null,
                                onEdit = if (message.isFromMe && message.content.isNotBlank()) {{ vm.onIntent(ChatIntent.StartEdit(message)) }} else null,
                                onSelfDestruct = if (message.isFromMe) {{ vm.onIntent(ChatIntent.ShowExpiryDialog(message.id)) }} else null,
                                isSelected = message.id in state.selectedMessageIds,
                                isMultiSelectActive = state.isMultiSelectActive,
                                onToggleSelect = { vm.onIntent(ChatIntent.ToggleMessageSelection(message.id)) },
                                onForward = { vm.onIntent(ChatIntent.ShowForwardDialog(message)) },
                                outgoingBubbleColor = chatThemeColors.bubbleColor,
                                onOpenPdf = onOpenPdf,
                                onVote = { optionId -> vm.onIntent(ChatIntent.VotePoll(message.content.removePrefix("poll:"), optionId)) },
                                onRetryMessage = { vm.onIntent(ChatIntent.RetryMessage(it)) },
                                onCopy = { vm.onIntent(ChatIntent.CopyMessageContent(it)) },
                                contactPhoneLookups = contactPhoneOf(message.content)?.let { phone ->
                                    state.contactCard.lookups[phone]?.let { mapOf(phone to it) }
                                } ?: emptyMap(),
                                onCheckContactRelationship = { vm.onIntent(ChatIntent.CheckContactRelationship(it)) },
                                onContactCardPrimaryAction = { vm.onIntent(ChatIntent.ContactCardPrimaryAction(it)) },
                                pollUiStates = pollIdOf(message.content)?.let { id ->
                                    state.poll.uiStates[id]?.let { mapOf(id to it) }
                                } ?: emptyMap(),
                                onObservePoll = { pollId -> vm.onIntent(ChatIntent.ObservePoll(pollId)) },
                                linkPreviews = state.linkPreviews,
                                onDetectedUrl = { url -> vm.onIntent(ChatIntent.DetectedUrlChanged(url)) },
                            )
                        }
                    } else {
                        MessageBubble(
                            message = message,
                            isGroup = state.isGroup,
                            onImageClick = { url ->
                                viewerUrls.value = listOf(url)
                                viewerInitialIndex.value = 0
                                showViewer.value = true
                            },
                            onReply = { vm.onIntent(ChatIntent.SetReply(message)) },
                            isHighlighted = message.id == highlightedMessageId,
                            onReplyClick = onScrollToMessage,
                            onDelete = if (message.isFromMe) {{ vm.onIntent(ChatIntent.DeleteMessage(message.id)) }} else null,
                            onEdit = if (message.isFromMe && message.content.isNotBlank()) {{ vm.onIntent(ChatIntent.StartEdit(message)) }} else null,
                            onSelfDestruct = if (message.isFromMe) {{ vm.onIntent(ChatIntent.ShowExpiryDialog(message.id)) }} else null,
                            onForward = { vm.onIntent(ChatIntent.ShowForwardDialog(message)) },
                            messageReactions = reactions[message.id] ?: emptyList(),
                            currentUserId = state.currentUserId,
                            onToggleReaction = { emoji -> vm.onIntent(ChatIntent.ToggleReaction(message.id, emoji)) },
                            isSelected = message.id in state.selectedMessageIds,
                            isMultiSelectActive = state.isMultiSelectActive,
                            onToggleSelect = { vm.onIntent(ChatIntent.ToggleMessageSelection(message.id)) },
                            outgoingBubbleColor = chatThemeColors.bubbleColor,
                            onOpenPdf = onOpenPdf,
                            onVote = { optionId -> vm.onIntent(ChatIntent.VotePoll(message.content.removePrefix("poll:"), optionId)) },
                            onShowReactionDetails = { reactionDetailMessageId.value = message.id },
                            onRetryMessage = { vm.onIntent(ChatIntent.RetryMessage(it)) },
                            onCopy = { vm.onIntent(ChatIntent.CopyMessageContent(it)) },
                            contactPhoneLookups = contactPhoneOf(message.content)?.let { phone ->
                                state.contactCard.lookups[phone]?.let { mapOf(phone to it) }
                            } ?: emptyMap(),
                            onCheckContactRelationship = { vm.onIntent(ChatIntent.CheckContactRelationship(it)) },
                            onContactCardPrimaryAction = { vm.onIntent(ChatIntent.ContactCardPrimaryAction(it)) },
                            pollUiStates = pollIdOf(message.content)?.let { id ->
                                state.poll.uiStates[id]?.let { mapOf(id to it) }
                            } ?: emptyMap(),
                            onObservePoll = { pollId -> vm.onIntent(ChatIntent.ObservePoll(pollId)) },
                            linkPreviews = state.linkPreviews,
                            onDetectedUrl = { url -> vm.onIntent(ChatIntent.DetectedUrlChanged(url)) },
                        )
                    }
                }
                }
                // isInsideGroup → render nothing; slot still exists for key stability + paging trigger
            }
            // With reverseLayout=true, this item appears at the visual TOP — shown while
            // loading older pages as the user scrolls up through history.
            item(key = "paging-load-more") {
                if (lazyPagingItems.loadState.append is LoadState.Loading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = showScrollToBottom,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = innerPadding.calculateBottomPadding() + 16.dp),
        ) {
            SmallFloatingActionButton(onClick = { scope.launch { listState.animateScrollToItem(0) } }) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.chat_scroll_to_bottom))
            }
        }
        if (state.isSearchActive) {
            MessageSearchOverlay(
                query = state.searchQuery,
                results = state.searchResults,
                isSearching = state.isSearching,
                topPadding = innerPadding.calculateTopPadding(),
                onQueryChange = { vm.onIntent(ChatIntent.SearchQueryChanged(it)) },
                onClose = { vm.onIntent(ChatIntent.CloseSearch) },
                onJump = { vm.onIntent(ChatIntent.JumpToMessage(it)) },
            )
        }
    }
}
