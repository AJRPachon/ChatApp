package com.ajrpachon.chatapp.service

object ActiveChatTracker {
    @Volatile var activeConversationId: String? = null
}
