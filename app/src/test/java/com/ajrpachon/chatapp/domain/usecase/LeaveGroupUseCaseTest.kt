package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.repository.GroupRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeaveGroupUseCaseTest {

    private val groupRepository = mockk<GroupRepository>()
    private val useCase = LeaveGroupUseCase(groupRepository)

    @Test
    fun `returns success when repository succeeds`() = runTest {
        coEvery { groupRepository.leaveGroup("conv1", "user1") } returns Unit

        val result = useCase("conv1", "user1")

        assertTrue(result.isSuccess)
        coVerify { groupRepository.leaveGroup("conv1", "user1") }
    }

    @Test
    fun `returns failure when repository throws`() = runTest {
        coEvery { groupRepository.leaveGroup(any(), any()) } throws RuntimeException("server error")

        val result = useCase("conv1", "user1")

        assertTrue(result.isFailure)
        assertEquals("server error", result.exceptionOrNull()?.message)
    }

    @Test(expected = CancellationException::class)
    fun `rethrows CancellationException`() = runTest {
        coEvery { groupRepository.leaveGroup(any(), any()) } throws CancellationException("cancelled")

        useCase("conv1", "user1")
    }
}
