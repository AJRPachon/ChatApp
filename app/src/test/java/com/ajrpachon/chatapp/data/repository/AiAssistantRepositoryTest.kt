package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.remote.dto.AiRequestDTO
import com.ajrpachon.chatapp.data.remote.source.AiAssistantRemoteSource
import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
import com.ajrpachon.chatapp.utils.AnalyticsEvents
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiAssistantRepositoryTest {

    private val remote = mockk<AiAssistantRemoteSource>()
    private val analytics = mockk<AnalyticsTracker>(relaxed = true)
    private val repo = AiAssistantRepository(remote, analytics)

    private fun usage(action: String) =
        mapOf(AnalyticsEvents.PARAM_ACTION to action)

    @Test
    fun `summarize sends the snippets with the summarize action and logs the usage`() = runTest {
        coEvery { remote.ask(any()) } returns "resumen"

        val result = repo.summarize(listOf("hola", "qué tal"))

        assertEquals("resumen", result.getOrNull())
        coVerify { remote.ask(AiRequestDTO(action = "summarize", messages = listOf("hola", "qué tal"))) }
        verify { analytics.logEvent(AnalyticsEvents.AI_ASSISTANT_USED, usage(AnalyticsEvents.ACTION_SUMMARIZE)) }
    }

    @Test
    fun `suggestReply sends the last message as the only snippet`() = runTest {
        coEvery { remote.ask(any()) } returns "claro"

        val result = repo.suggestReply("¿vienes?")

        assertEquals("claro", result.getOrNull())
        coVerify { remote.ask(AiRequestDTO(action = "suggest", messages = listOf("¿vienes?"))) }
        verify { analytics.logEvent(AnalyticsEvents.AI_ASSISTANT_USED, usage(AnalyticsEvents.ACTION_SUGGEST_REPLY)) }
    }

    @Test
    fun `freeform sends the prompt and no messages`() = runTest {
        coEvery { remote.ask(any()) } returns "respuesta"

        val result = repo.freeform("cuéntame un chiste")

        assertEquals("respuesta", result.getOrNull())
        coVerify { remote.ask(AiRequestDTO(action = "freeform", prompt = "cuéntame un chiste")) }
        verify { analytics.logEvent(AnalyticsEvents.AI_ASSISTANT_USED, usage(AnalyticsEvents.ACTION_FREEFORM)) }
    }

    @Test
    fun `a failing call returns a failure and logs nothing`() = runTest {
        coEvery { remote.ask(any()) } throws IllegalStateException("502")

        val result = repo.summarize(listOf("x"))

        assertTrue(result.isFailure)
        assertEquals("502", result.exceptionOrNull()?.message)
        verify(exactly = 0) { analytics.logEvent(any(), any()) }
    }
}
