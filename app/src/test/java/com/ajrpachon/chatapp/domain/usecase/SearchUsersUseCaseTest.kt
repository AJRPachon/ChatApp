package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchUsersUseCaseTest {

    private val userRepository = mockk<UserRepository>()
    private val useCase = SearchUsersUseCase(userRepository)

    @Test
    fun `returns empty list and skips repository when query is blank`() = runTest {
        val result = useCase("   ")

        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { userRepository.searchByUsername(any()) }
    }

    @Test
    fun `returns empty list and skips repository when query is empty`() = runTest {
        val result = useCase("")

        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { userRepository.searchByUsername(any()) }
    }

    @Test
    fun `delegates to repository when query is not blank`() = runTest {
        val users = listOf(mockk<UserBO>(relaxed = true))
        coEvery { userRepository.searchByUsername("alice") } returns users

        val result = useCase("alice")

        assertEquals(users, result)
        coVerify { userRepository.searchByUsername("alice") }
    }
}
