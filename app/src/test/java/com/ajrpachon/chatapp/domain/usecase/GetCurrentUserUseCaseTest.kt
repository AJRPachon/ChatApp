package com.ajrpachon.chatapp.domain.usecase

import app.cash.turbine.test
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetCurrentUserUseCaseTest {

    private val userRepository = mockk<UserRepository>()
    private val useCase = GetCurrentUserUseCase(userRepository)

    private val user = mockk<UserBO>(relaxed = true)

    @Test
    fun `emits current user from repository`() = runTest {
        every { userRepository.getCurrentUser() } returns flowOf(user)

        useCase().test {
            assertEquals(user, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `emits null when there is no active session`() = runTest {
        every { userRepository.getCurrentUser() } returns flowOf(null)

        useCase().test {
            assertEquals(null, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `does not re-emit consecutive identical values`() = runTest {
        every { userRepository.getCurrentUser() } returns flowOf(user, user, user)

        useCase().test {
            assertEquals(user, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `re-emits when value changes`() = runTest {
        val otherUser = mockk<UserBO>(relaxed = true)
        every { userRepository.getCurrentUser() } returns flowOf(user, otherUser)

        useCase().test {
            assertEquals(user, awaitItem())
            assertEquals(otherUser, awaitItem())
            awaitComplete()
        }
    }
}
