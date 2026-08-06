package com.ajrpachon.chatapp.domain.usecase

import app.cash.turbine.test
import com.ajrpachon.chatapp.domain.model.GroupMemberBO
import com.ajrpachon.chatapp.domain.repository.GroupRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetGroupMembersUseCaseTest {

    private val groupRepository = mockk<GroupRepository>()
    private val useCase = GetGroupMembersUseCase(groupRepository)

    @Test
    fun `returns empty flow without querying repository when conversationId is blank`() = runTest {
        useCase("   ").test {
            awaitComplete()
        }
        verify(exactly = 0) { groupRepository.observeMembers(any()) }
    }

    @Test
    fun `returns empty flow without querying repository when conversationId is empty`() = runTest {
        useCase("").test {
            awaitComplete()
        }
        verify(exactly = 0) { groupRepository.observeMembers(any()) }
    }

    @Test
    fun `delegates to repository when conversationId is valid`() = runTest {
        val members = listOf(mockk<GroupMemberBO>(relaxed = true))
        every { groupRepository.observeMembers("conv-1") } returns flowOf(members)

        useCase("conv-1").test {
            val emitted = awaitItem()
            org.junit.Assert.assertEquals(members, emitted)
            awaitComplete()
        }
    }
}
