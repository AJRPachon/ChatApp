package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.repository.GroupRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoveGroupMemberUseCaseTest {

    private val groupRepository = mockk<GroupRepository>()
    private val useCase = RemoveGroupMemberUseCase(groupRepository)

    @Test
    fun `returns success and delegates to repository`() = runTest {
        coEvery { groupRepository.removeMember("conv-1", "user-1") } returns Unit

        val result = useCase("conv-1", "user-1")

        assertTrue(result.isSuccess)
        coVerify { groupRepository.removeMember("conv-1", "user-1") }
    }

    @Test
    fun `returns failure when repository throws`() = runTest {
        coEvery { groupRepository.removeMember("conv-1", "user-1") } throws RuntimeException("cannot remove")

        val result = useCase("conv-1", "user-1")

        assertTrue(result.isFailure)
        assertEquals("cannot remove", result.exceptionOrNull()?.message)
    }
}
