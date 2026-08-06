package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.repository.GroupRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PromoteGroupMemberUseCaseTest {

    private val groupRepository = mockk<GroupRepository>()
    private val useCase = PromoteGroupMemberUseCase(groupRepository)

    @Test
    fun `promote returns success and delegates to repository`() = runTest {
        coEvery { groupRepository.promoteMember("conv-1", "user-1") } returns Unit

        val result = useCase.promote("conv-1", "user-1")

        assertTrue(result.isSuccess)
        coVerify { groupRepository.promoteMember("conv-1", "user-1") }
    }

    @Test
    fun `promote returns failure when repository throws`() = runTest {
        coEvery { groupRepository.promoteMember("conv-1", "user-1") } throws RuntimeException("not admin")

        val result = useCase.promote("conv-1", "user-1")

        assertTrue(result.isFailure)
        assertEquals("not admin", result.exceptionOrNull()?.message)
    }

    @Test
    fun `demote returns success and delegates to repository`() = runTest {
        coEvery { groupRepository.demoteMember("conv-1", "user-1") } returns Unit

        val result = useCase.demote("conv-1", "user-1")

        assertTrue(result.isSuccess)
        coVerify { groupRepository.demoteMember("conv-1", "user-1") }
    }

    @Test
    fun `demote returns failure when repository throws`() = runTest {
        coEvery { groupRepository.demoteMember("conv-1", "user-1") } throws RuntimeException("last admin")

        val result = useCase.demote("conv-1", "user-1")

        assertTrue(result.isFailure)
        assertEquals("last admin", result.exceptionOrNull()?.message)
    }
}
