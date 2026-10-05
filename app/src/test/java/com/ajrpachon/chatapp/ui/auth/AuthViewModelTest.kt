package com.ajrpachon.chatapp.ui.auth

import com.ajrpachon.chatapp.domain.model.IntegrityResultBO
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.repository.AuthRepository
import com.ajrpachon.chatapp.domain.repository.FcmTokenRepository
import com.ajrpachon.chatapp.domain.repository.SessionInfo
import com.ajrpachon.chatapp.domain.repository.UserRepository
import com.ajrpachon.chatapp.domain.usecase.SetUsernameUseCase
import com.ajrpachon.chatapp.ui.common.UiText
import com.ajrpachon.chatapp.util.MainDispatcherRule
import com.ajrpachon.chatapp.utils.SessionGuard
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.security.MessageDigest

class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val setUsernameUseCase = mockk<SetUsernameUseCase>(relaxed = true)
    private val fcmTokenRepository = mockk<FcmTokenRepository>(relaxed = true)
    private val sessionGuard = mockk<SessionGuard>(relaxed = true)

    private val signedInUser = UserBO(
        id = "u1",
        email = "a@b.c",
        username = "ana",
        displayName = "Ana",
        avatarUrl = null,
        createdAt = Instant.fromEpochMilliseconds(0L),
    )

    @Before
    fun setUp() {
        coEvery { authRepository.checkIntegrity() } returns IntegrityResultBO.Passed
        coEvery { authRepository.getCurrentSessionInfo() } returns null
    }

    private fun buildViewModel() = AuthViewModel(authRepository, userRepository, setUsernameUseCase, fcmTokenRepository, sessionGuard)

    private fun sha256Hex(raw: String) = MessageDigest.getInstance("SHA-256")
        .digest(raw.toByteArray())
        .joinToString("") { "%02x".format(it) }

    /** Starts a Google sign-in and returns the nonce hash the ViewModel asked the screen to use. */
    private suspend fun AuthViewModel.startGoogleSignIn(): String {
        onIntent(AuthIntent.SignInWithGoogle)
        val effect = effect.first { it is AuthEffect.RequestGoogleCredential }
        return (effect as AuthEffect.RequestGoogleCredential).hashedNonce
    }

    @Test
    fun `SignInWithGoogle starts loading and asks the screen for a credential with a hashed nonce`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()

        val hashed = vm.startGoogleSignIn()

        assertTrue(vm.state.value.isLoading)
        assertEquals(64, hashed.length)
        assertTrue(hashed.all { it in "0123456789abcdef" })
    }

    @Test
    fun `GoogleTokenReceived signs in with the raw nonce whose hash was requested, then goes home`() = runTest(mainDispatcherRule.scheduler) {
        coEvery { authRepository.getCurrentSessionInfo() } returnsMany listOf(null, SessionInfo(userId = "u1", email = "a@b.c"))
        coEvery { userRepository.fetchProfileFromRemote("u1") } returns signedInUser
        val vm = buildViewModel()
        advanceUntilIdle()
        val hashed = vm.startGoogleSignIn()

        vm.onIntent(AuthIntent.GoogleTokenReceived("id-token"))
        advanceUntilIdle()

        val rawNonce = slot<String>()
        coVerify { authRepository.signInWithGoogle("id-token", capture(rawNonce)) }
        assertEquals(hashed, sha256Hex(rawNonce.captured))
        assertTrue(vm.effect.first { it is AuthEffect.NavigateToHome } is AuthEffect.NavigateToHome)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `a token with no sign-in in flight is ignored`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(AuthIntent.GoogleTokenReceived("stray-token"))
        advanceUntilIdle()

        coVerify(exactly = 0) { authRepository.signInWithGoogle(any(), any()) }
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `a failed Supabase sign-in shows the error and stops loading`() = runTest(mainDispatcherRule.scheduler) {
        coEvery { authRepository.signInWithGoogle(any(), any()) } throws IllegalStateException("bad nonce")
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.startGoogleSignIn()

        vm.onIntent(AuthIntent.GoogleTokenReceived("id-token"))
        advanceUntilIdle()

        assertEquals(UiText.Dynamic("bad nonce"), vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `no Google account on the device opens the add-account screen without an error`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.startGoogleSignIn()

        vm.onIntent(AuthIntent.GoogleSignInFailed(message = null, noCredential = true))

        assertTrue(vm.effect.first { it is AuthEffect.OpenAddGoogleAccount } is AuthEffect.OpenAddGoogleAccount)
        assertEquals(null, vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `any other credential failure shows its message`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.startGoogleSignIn()

        vm.onIntent(AuthIntent.GoogleSignInFailed(message = "sheet dismissed", noCredential = false))

        assertEquals(UiText.Dynamic("sheet dismissed"), vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `a cancelled request stops loading and drops the pending nonce`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.startGoogleSignIn()

        vm.onIntent(AuthIntent.GoogleSignInCancelled)
        vm.onIntent(AuthIntent.GoogleTokenReceived("late-token"))
        advanceUntilIdle()

        assertFalse(vm.state.value.isLoading)
        coVerify(exactly = 0) { authRepository.signInWithGoogle(any(), any()) }
    }
}
