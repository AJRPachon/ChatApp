package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.repository.GroupRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddGroupMemberUseCaseTest {

    private val groupRepository = mockk<GroupRepository>()
    private val useCase = AddGroupMemberUseCase(groupRepository)

    @Test
    fun `returns success and delegates to repository`() = runTest {
        coEvery { groupRepository.addMember("conv-1", "user-1", true) } returns Unit

        val result = useCase("conv-1", "user-1", true)

        assertTrue(result.isSuccess)
        coVerify { groupRepository.addMember("conv-1", "user-1", true) }
    }

    @Test
    fun `returns failure when repository throws`() = runTest {
        coEvery { groupRepository.addMember("conv-1", "user-1", false) } throws RuntimeException("boom")

        val result = useCase("conv-1", "user-1", false)

        assertTrue(result.isFailure)
        assertEquals("boom", result.exceptionOrNull()?.message)
    }
}
