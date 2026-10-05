package com.ajrpachon.chatapp.ui.chat

import com.ajrpachon.chatapp.domain.model.GroupMemberBO

/**
 * Pure helpers behind group `@mention` autocomplete ([ChatState.mentionSuggestions],
 * `ChatIntent.SelectMention`) and in-bubble highlighting ([ChatBubbleContent]). Kept free of
 * Compose/Android so the matching and insertion rules are unit-testable on their own.
 *
 * A mention "query" is only active while the caret-side token (the text after the last
 * whitespace) starts with `@` and contains nothing but username characters — so `a@b.com` or
 * `@ana ` (already completed, trailing space) never trigger suggestions.
 */
internal object ChatMentions {

    // Same character set SetUsernameUseCase allows (`[a-z0-9_]`), matched case-insensitively
    // because users may type capitals. The lookbehind skips the `@` inside e-mail addresses.
    private val MENTION_REGEX = Regex("(?<![\\w@])@[A-Za-z0-9_]+")

    private fun isUsernameChar(c: Char) = c.isLetterOrDigit() || c == '_'

    private fun trailingToken(text: String): String =
        text.substring(text.indexOfLast { it.isWhitespace() } + 1)

    /** The partial username being typed after `@` (possibly empty for a bare `@`), or null. */
    fun activeQuery(text: String): String? {
        val token = trailingToken(text)
        if (!token.startsWith("@")) return null
        val query = token.drop(1)
        return query.takeIf { q -> q.all(::isUsernameChar) }
    }

    /**
     * Members matching the active query by username or display name (case-insensitive), with
     * prefix matches first. Excludes [currentUserId] and members without a username. Empty when
     * no query is active.
     */
    fun suggestions(text: String, members: List<GroupMemberBO>, currentUserId: String?): List<GroupMemberBO> {
        val query = activeQuery(text)?.lowercase() ?: return emptyList()
        return members
            .filter { it.userId != currentUserId && it.username.isNotBlank() }
            .filter { it.username.lowercase().contains(query) || it.displayName.lowercase().contains(query) }
            .sortedByDescending { it.username.lowercase().startsWith(query) || it.displayName.lowercase().startsWith(query) }
    }

    /**
     * Replaces the active `@query` token at the end of [text] with `@username ` (trailing space
     * closes the query, so suggestions disappear). Returns [text] unchanged if no query is active.
     */
    fun insert(text: String, username: String): String {
        if (activeQuery(text) == null) return text
        return text.dropLast(trailingToken(text).length) + "@$username "
    }

    /** Character ranges of every `@username` mention in a message body, for highlighting. */
    fun mentionRanges(text: String): List<IntRange> =
        MENTION_REGEX.findAll(text).map { it.range }.toList()
}
