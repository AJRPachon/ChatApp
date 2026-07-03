package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.repository.GroupRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateGroupUseCaseTest {

    private val groupRepository = mockk<GroupRepository>()
    private val useCase = CreateGroupUseCase(groupRepository)
    private val fakeConversation = mockk<ConversationBO>(relaxed = true)

    @Test
    fun `returns failure when name is blank`() = runTest {
        val result = useCase("   ", null, "creator-id", listOf("user-1"))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("El nombre del grupo no puede estar vacío", result.exceptionOrNull()?.message)
    }

    @Test
    fun `returns failure when name is empty`() = runTest {
        val result = useCase("", null, "creator-id", listOf("user-1"))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `returns failure when participant list is empty`() = runTest {
        val result = useCase("My Group", null, "creator-id", emptyList())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Añade al menos un participante", result.exceptionOrNull()?.message)
    }

    @Test
    fun `returns success when name and participants are valid`() = runTest {
        coEvery {
            groupRepository.createGroup("My Group", null, "creator-id", listOf("user-1", "user-2"))
        } returns fakeConversation

        val result = useCase("My Group", null, "creator-id", listOf("user-1", "user-2"))

        assertTrue(result.isSuccess)
        assertEquals(fakeConversation, result.getOrNull())
    }

    @Test
    fun `trims name before passing to repository`() = runTest {
        coEvery {
            groupRepository.createGroup("Trimmed Group", null, "creator-id", listOf("user-1"))
        } returns fakeConversation

        useCase("  Trimmed Group  ", null, "creator-id", listOf("user-1"))

        coVerify { groupRepository.createGroup("Trimmed Group", null, "creator-id", listOf("user-1")) }
    }

    @Test
    fun `passes trimmed non-blank description to repository`() = runTest {
        coEvery {
            groupRepository.createGroup("Group", "A description", "creator-id", listOf("user-1"))
        } returns fakeConversation

        useCase("Group", "  A description  ", "creator-id", listOf("user-1"))

        coVerify { groupRepository.createGroup("Group", "A description", "creator-id", listOf("user-1")) }
    }

    @Test
    fun `passes null description when description is blank`() = runTest {
        coEvery {
            groupRepository.createGroup("Group", null, "creator-id", listOf("user-1"))
        } returns fakeConversation

        useCase("Group", "   ", "creator-id", listOf("user-1"))

        coVerify { groupRepository.createGroup("Group", null, "creator-id", listOf("user-1")) }
    }

    @Test
    fun `returns failure when repository throws`() = runTest {
        coEvery {
            groupRepository.createGroup(any(), any(), any(), any())
        } throws RuntimeException("DB failure")

        val result = useCase("Group", null, "creator-id", listOf("user-1"))

        assertTrue(result.isFailure)
        assertEquals("DB failure", result.exceptionOrNull()?.message)
    }
}
