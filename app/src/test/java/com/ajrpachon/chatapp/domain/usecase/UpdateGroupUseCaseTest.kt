package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.repository.GroupRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateGroupUseCaseTest {

    private val groupRepository = mockk<GroupRepository>()
    private val useCase = UpdateGroupUseCase(groupRepository)

    @Test
    fun `trims name with leading and trailing spaces before passing to repository`() = runTest {
        coEvery { groupRepository.updateGroup("conv-1", "Trimmed Name", null, null) } returns Unit

        useCase("conv-1", name = "  Trimmed Name  ")

        coVerify { groupRepository.updateGroup("conv-1", "Trimmed Name", null, null) }
    }

    @Test
    fun `passes null when name is blank (only spaces)`() = runTest {
        coEvery { groupRepository.updateGroup("conv-1", null, null, null) } returns Unit

        useCase("conv-1", name = "   ")

        coVerify { groupRepository.updateGroup("conv-1", null, null, null) }
    }

    @Test
    fun `passes null when name is empty string`() = runTest {
        coEvery { groupRepository.updateGroup("conv-1", null, null, null) } returns Unit

        useCase("conv-1", name = "")

        coVerify { groupRepository.updateGroup("conv-1", null, null, null) }
    }

    @Test
    fun `returns success when repository succeeds`() = runTest {
        coEvery { groupRepository.updateGroup("conv-1", "New Name", "Desc", null) } returns Unit

        val result = useCase("conv-1", name = "New Name", description = "Desc")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `passes all fields to repository`() = runTest {
        coEvery { groupRepository.updateGroup("conv-1", "Group Name", "Description", "https://avatar.url") } returns Unit

        useCase("conv-1", name = "Group Name", description = "Description", avatarUrl = "https://avatar.url")

        coVerify { groupRepository.updateGroup("conv-1", "Group Name", "Description", "https://avatar.url") }
    }

    @Test
    fun `returns failure when repository throws`() = runTest {
        coEvery { groupRepository.updateGroup(any(), any(), any(), any()) } throws RuntimeException("Update failed")

        val result = useCase("conv-1", name = "New Name")

        assertTrue(result.isFailure)
        assertEquals("Update failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun `all params are optional - passes nulls when not provided`() = runTest {
        coEvery { groupRepository.updateGroup("conv-1", null, null, null) } returns Unit

        val result = useCase("conv-1")

        assertTrue(result.isSuccess)
        coVerify { groupRepository.updateGroup("conv-1", null, null, null) }
    }
}
