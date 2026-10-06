package com.ajrpachon.chatapp.ui.chat

import com.ajrpachon.chatapp.domain.model.MessageBO

internal sealed class ChatItem {
    abstract val id: String
    data class Single(val message: MessageBO) : ChatItem() {
        override val id = message.id
    }
    data class Group(val messages: List<MessageBO>) : ChatItem() {
        override val id = "group_${messages.first().id}"
    }
}

private fun MessageBO.isImageOnly() = imageUrl != null && audioUrl == null

/** Index just past the run of image-only messages from the same sender that starts at [start]. */
private fun List<MessageBO>.imageRunEnd(start: Int): Int {
    val senderId = this[start].senderId
    var end = start + 1
    while (end < size && this[end].isImageOnly() && this[end].senderId == senderId) end++
    return end
}

internal fun List<MessageBO>.toChatItems(): List<ChatItem> {
    val result = mutableListOf<ChatItem>()
    var i = 0
    while (i < size) {
        val end = if (this[i].isImageOnly()) imageRunEnd(i) else i + 1
        val run = subList(i, end)
        if (run.size > 2) result.add(ChatItem.Group(run.toList())) else run.mapTo(result) { ChatItem.Single(it) }
        i = end
    }
    return result
}
