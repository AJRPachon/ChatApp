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

internal fun List<MessageBO>.toChatItems(): List<ChatItem> {
    val result = mutableListOf<ChatItem>()
    var i = 0
    while (i < size) {
        val message = this[i]
        if (message.imageUrl != null && message.audioUrl == null) {
            val group = mutableListOf(message)
            var j = i + 1
            while (j < size && this[j].imageUrl != null && this[j].audioUrl == null && this[j].senderId == message.senderId) {
                group.add(this[j])
                j++
            }
            if (group.size > 2) {
                result.add(ChatItem.Group(group))
            } else {
                group.forEach { result.add(ChatItem.Single(it)) }
            }
            i = j
        } else {
            result.add(ChatItem.Single(message))
            i++
        }
    }
    return result
}
