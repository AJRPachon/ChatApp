package com.ajrpachon.chatapp.ui.profile

import com.ajrpachon.chatapp.domain.model.ThemePreference
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.repository.AppLockRepository
import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
import com.ajrpachon.chatapp.domain.repository.AuthRepository
import com.ajrpachon.chatapp.domain.repository.FcmTokenRepository
import com.ajrpachon.chatapp.domain.repository.ThemeRepository
import com.ajrpachon.chatapp.domain.repository.UserRepository
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.util.MainDispatcherRule
import com.ajrpachon.chatapp.util.sharedScheduler
import io.github.jan.supabase.exceptions.RestException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val getCurrentUserUseCase = mockk<GetCurrentUserUseCase>()
    private val fcmTokenRepository = mockk<FcmTokenRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val themeRepository = mockk<ThemeRepository>()
    private val appLockRepository = mockk<AppLockRepository>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)

    private val userBO = UserBO(
        id = "user1",
        email = "user1@example.com",
        username = "user1",
        displayName = "User One",
        avatarUrl = null,
        createdAt = Instant.fromEpochMilliseconds(0),
    )

    @Before
    fun setUp() {
        every { getCurrentUserUseCase() } returns flowOf(userBO)
        every { authRepository.getCurrentUserId() } returns userBO.id
        every { themeRepository.observe() } returns emptyFlow()
        every { appLockRepository.isEnabled } returns emptyFlow()
    }

    private fun buildViewModel() = ProfileViewModel(
        authRepository = authRepository,
        getCurrentUserUseCase = getCurrentUserUseCase,
        fcmTokenRepository = fcmTokenRepository,
        userRepository = userRepository,
        themeRepository = themeRepository,
        appLockRepository = appLockRepository,
        analyticsTracker = analyticsTracker,
    )

    private fun restException(statusCode: Int, message: String? = null): RestException {
        val exception = mockk<RestException>()
        every { exception.statusCode } returns statusCode
        every { exception.message } returns message
        return exception
    }

    @Test
    fun `requestDeleteAccount sends ShowDeleteAccountConfirm effect`() = runTest(sharedScheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.requestDeleteAccount()
        advanceUntilIdle()

        assertEquals(ProfileEffect.ShowDeleteAccountConfirm, vm.effect.first())
    }

    @Test
    fun `deleteAccount success clears local state and navigates to auth`() = runTest(sharedScheduler) {
        coEvery { authRepository.deleteAccount() } returns Unit
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.deleteAccount()
        advanceUntilIdle()

        coVerify { fcmTokenRepository.deleteToken() }
        coVerify { authRepository.deleteAccount() }
        coVerify { userRepository.clearCurrentUser() }
        assertFalse(vm.state.value.isDeletingAccount)
        assertNull(vm.state.value.error)
        assertEquals(ProfileEffect.NavigateToAuth, vm.effect.first())
    }

    @Test
    fun `deleteAccount with 401 shows invalid session message`() = runTest(sharedScheduler) {
        coEvery { authRepository.deleteAccount() } throws restException(401)
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.deleteAccount()
        advanceUntilIdle()

        assertFalse(vm.state.value.isDeletingAccount)
        assertTrue(vm.state.value.error.orEmpty().contains("sesion", ignoreCase = true))
    }

    @Test
    fun `deleteAccount with 429 shows rate limit message`() = runTest(sharedScheduler) {
        coEvery { authRepository.deleteAccount() } throws restException(429)
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.deleteAccount()
        advanceUntilIdle()

        assertFalse(vm.state.value.isDeletingAccount)
        assertTrue(vm.state.value.error.orEmpty().contains("minuto", ignoreCase = true))
    }

    @Test
    fun `deleteAccount with 500 shows generic server error message`() = runTest(sharedScheduler) {
        coEvery { authRepository.deleteAccount() } throws restException(500)
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.deleteAccount()
        advanceUntilIdle()

        assertFalse(vm.state.value.isDeletingAccount)
        assertTrue(vm.state.value.error.orEmpty().contains("servidor", ignoreCase = true))
    }

    @Test
    fun `deleteAccount with unexpected error falls back to exception message`() = runTest(sharedScheduler) {
        coEvery { authRepository.deleteAccount() } throws IllegalStateException("boom")
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.deleteAccount()
        advanceUntilIdle()

        assertFalse(vm.state.value.isDeletingAccount)
        assertEquals("boom", vm.state.value.error)
    }
}
