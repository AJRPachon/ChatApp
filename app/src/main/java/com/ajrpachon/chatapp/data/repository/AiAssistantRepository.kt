package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.remote.dto.AiRequestDTO
import com.ajrpachon.chatapp.data.remote.source.AiAssistantRemoteSource
import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
// This class shares its simple name with the domain interface it implements — aliased to avoid
// a same-name clash (there is no "...Impl" suffix on this one, unlike its sibling repositories).
import com.ajrpachon.chatapp.domain.repository.AiAssistantRepository as AiAssistantRepositoryContract
import com.ajrpachon.chatapp.utils.AnalyticsEvents

class AiAssistantRepository(
    private val remoteSource: AiAssistantRemoteSource,
    private val analyticsTracker: AnalyticsTracker,
) : AiAssistantRepositoryContract {

    override suspend fun summarize(messageSnippets: List<String>): Result<String> = runCatching {
        remoteSource.ask(AiRequestDTO(action = "summarize", messages = messageSnippets))
    }.onSuccess { logUsage(AnalyticsEvents.ACTION_SUMMARIZE) }

    override suspend fun suggestReply(lastMessage: String): Result<String> = runCatching {
        remoteSource.ask(AiRequestDTO(action = "suggest", messages = listOf(lastMessage)))
    }.onSuccess { logUsage(AnalyticsEvents.ACTION_SUGGEST_REPLY) }

    override suspend fun freeform(prompt: String): Result<String> = runCatching {
        remoteSource.ask(AiRequestDTO(action = "freeform", prompt = prompt))
    }.onSuccess { logUsage(AnalyticsEvents.ACTION_FREEFORM) }

    private fun logUsage(action: String) {
        analyticsTracker.logEvent(AnalyticsEvents.AI_ASSISTANT_USED, mapOf(AnalyticsEvents.PARAM_ACTION to action))
    }
}
