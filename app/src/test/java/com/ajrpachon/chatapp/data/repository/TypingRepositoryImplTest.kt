package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.remote.dto.TypingPresenceDTO
import com.ajrpachon.chatapp.data.remote.source.TypingRemoteSource
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TypingRepositoryImplTest {

    private val remote = mockk<TypingRemoteSource>(relaxed = true)
    private val repo = TypingRepositoryImpl(remote)

    private fun presence(userId: String, name: String, typing: Boolean) =
        TypingPresenceDTO(isTyping = typing, userId = userId, userName = name)

    @Test
    fun `observeTypingNames keeps only other users who are typing`() = runTest {
        every { remote.observePresences("conv1") } returns flowOf(
            listOf(
                presence("me", "Yo", typing = true),
                presence("u2", "Ana", typing = true),
                presence("u3", "Bea", typing = false),
            ),
        )

        val emissions = repo.observeTypingNames("conv1", currentUserId = "me").toList()

        assertEquals(listOf(listOf("Ana")), emissions)
    }

    @Test
    fun `observeTypingNames drops repeated names`() = runTest {
        every { remote.observePresences("conv1") } returns flowOf(
            listOf(presence("u2", "Ana", true), presence("u4", "Ana", true), presence("u3", "Bea", true)),
        )

        assertEquals(listOf(listOf("Ana", "Bea")), repo.observeTypingNames("conv1", "me").toList())
    }

    @Test
    fun `observeTypingNames re-emits for every presence update`() = runTest {
        every { remote.observePresences("conv1") } returns flowOf(
            listOf(presence("u2", "Ana", true)),
            listOf(presence("u2", "Ana", false)),
        )

        assertEquals(listOf(listOf("Ana"), emptyList()), repo.observeTypingNames("conv1", "me").toList())
    }

    @Test
    fun `sendTypingState publishes the presence of the user`() = runTest {
        repo.sendTypingState("conv1", userId = "me", userName = "Yo", isTyping = true)

        coVerify { remote.track("conv1", TypingPresenceDTO(isTyping = true, userId = "me", userName = "Yo")) }
    }

    @Test
    fun `subscribeChannel and close are passed to the remote source`() = runTest {
        repo.subscribeChannel("conv1")
        repo.close("conv1")

        coVerify { remote.subscribe("conv1") }
        coVerify { remote.close("conv1") }
    }
}
