package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
import com.ajrpachon.chatapp.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SetUsernameUseCaseTest {

    private val userRepository = mockk<UserRepository>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val useCase = SetUsernameUseCase(userRepository, analyticsTracker)
    private val fakeUser = mockk<UserBO>(relaxed = true)

    @Test
    fun `returns failure when username is shorter than 3 chars`() = runTest {
        val result = useCase("user-1", "ab")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Username too short", result.exceptionOrNull()?.message)
    }

    @Test
    fun `returns failure when username is empty`() = runTest {
        val result = useCase("user-1", "")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `returns failure when username contains uppercase letters`() = runTest {
        val result = useCase("user-1", "UserName")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `returns failure when username contains spaces`() = runTest {
        val result = useCase("user-1", "user name")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `returns failure when username contains @ symbol`() = runTest {
        val result = useCase("user-1", "user@name")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `returns failure when username contains hyphen`() = runTest {
        val result = useCase("user-1", "user-name")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `returns failure when username exceeds 20 chars`() = runTest {
        val result = useCase("user-1", "a".repeat(21))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `returns failure when username is already taken`() = runTest {
        coEvery { userRepository.isUsernameAvailable("taken_name") } returns false

        val result = useCase("user-1", "taken_name")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals("Username already taken", result.exceptionOrNull()?.message)
    }

    @Test
    fun `returns success when username is valid and available`() = runTest {
        coEvery { userRepository.isUsernameAvailable("valid_user") } returns true
        coEvery { userRepository.setUsername("user-1", "valid_user") } returns Result.success(fakeUser)

        val result = useCase("user-1", "valid_user")

        assertTrue(result.isSuccess)
        assertEquals(fakeUser, result.getOrNull())
    }

    @Test
    fun `accepts username with exactly 3 chars`() = runTest {
        coEvery { userRepository.isUsernameAvailable("abc") } returns true
        coEvery { userRepository.setUsername("user-1", "abc") } returns Result.success(fakeUser)

        val result = useCase("user-1", "abc")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `accepts username with digits and underscores`() = runTest {
        coEvery { userRepository.isUsernameAvailable("user_123") } returns true
        coEvery { userRepository.setUsername("user-1", "user_123") } returns Result.success(fakeUser)

        val result = useCase("user-1", "user_123")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `propagates repository failure`() = runTest {
        coEvery { userRepository.isUsernameAvailable("good_user") } returns true
        coEvery { userRepository.setUsername("user-1", "good_user") } returns Result.failure(RuntimeException("Network error"))

        val result = useCase("user-1", "good_user")

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }
}
