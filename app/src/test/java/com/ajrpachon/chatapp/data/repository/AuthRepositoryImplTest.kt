package com.ajrpachon.chatapp.data.repository

import android.content.Context
import com.ajrpachon.chatapp.data.local.ChatDatabase
import com.ajrpachon.chatapp.data.remote.dto.AuthSessionDTO
import com.ajrpachon.chatapp.data.remote.dto.MfaAssuranceDTO
import com.ajrpachon.chatapp.data.remote.dto.TotpEnrollmentDTO
import com.ajrpachon.chatapp.data.remote.source.AuthRemoteSource
import com.ajrpachon.chatapp.domain.model.IntegrityResultBO
import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
import com.ajrpachon.chatapp.domain.repository.CrashReporter
import com.ajrpachon.chatapp.domain.repository.MfaAssuranceLevel
import com.ajrpachon.chatapp.domain.repository.SessionInfo
import com.ajrpachon.chatapp.domain.repository.TotpEnrollment
import com.ajrpachon.chatapp.domain.repository.AnalyticsEvents
import com.ajrpachon.chatapp.utils.E2EEKeyManager
import com.ajrpachon.chatapp.utils.SessionGuard
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryImplTest {

    private val context = mockk<Context>(relaxed = true)
    private val remote = mockk<AuthRemoteSource>(relaxed = true)
    private val chatDatabase = mockk<ChatDatabase>(relaxed = true)
    private val sessionGuard = mockk<SessionGuard>(relaxed = true)
    private val crashReporter = mockk<CrashReporter>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)

    private val repo = AuthRepositoryImpl(context, remote, chatDatabase, sessionGuard, crashReporter, analyticsTracker)

    @Before
    fun setUp() {
        mockkObject(E2EEKeyManager)
        every { E2EEKeyManager.deleteKeyPair(any()) } just runs
    }

    @After
    fun tearDown() = unmockkObject(E2EEKeyManager)

    // ── session info ──────────────────────────────────────────────────────────

    @Test
    fun `getCurrentSessionInfo maps the remote session`() = runTest {
        every { remote.getCurrentSession() } returns AuthSessionDTO(userId = "u1", email = "a@b.c")

        assertEquals(SessionInfo(userId = "u1", email = "a@b.c"), repo.getCurrentSessionInfo())
    }

    @Test
    fun `getCurrentSessionInfo is null when there is no session`() = runTest {
        every { remote.getCurrentSession() } returns null

        assertNull(repo.getCurrentSessionInfo())
    }

    // ── sign in / up ──────────────────────────────────────────────────────────

    @Test
    fun `signInWithEmail signs in, tags crash reports with the user and logs a login`() = runTest {
        every { remote.getCurrentUserId() } returns "u1"

        repo.signInWithEmail("a@b.c", "secret")

        coVerifyOrder {
            remote.signInWithEmail("a@b.c", "secret")
            crashReporter.setUserId("u1")
            analyticsTracker.logEvent(
                AnalyticsEvents.LOGIN,
                mapOf(AnalyticsEvents.PARAM_METHOD to AnalyticsEvents.METHOD_EMAIL),
            )
        }
    }

    @Test
    fun `signInWithEmail does not log a login when the sign in fails`() = runTest {
        coEvery { remote.signInWithEmail(any(), any()) } throws IllegalStateException("bad credentials")

        val result = runCatching { repo.signInWithEmail("a@b.c", "wrong") }

        assertTrue(result.isFailure)
        verify(exactly = 0) { analyticsTracker.logEvent(AnalyticsEvents.LOGIN, any()) }
        verify(exactly = 0) { crashReporter.setUserId(any()) }
    }

    @Test
    fun `signUpWithEmail returns whether the remote already opened a session and logs a sign up`() = runTest {
        coEvery { remote.signUpWithEmail("a@b.c", "secret") } returns false

        assertFalse(repo.signUpWithEmail("a@b.c", "secret"))
        verify {
            analyticsTracker.logEvent(
                AnalyticsEvents.SIGN_UP,
                mapOf(AnalyticsEvents.PARAM_METHOD to AnalyticsEvents.METHOD_EMAIL),
            )
        }
    }

    @Test
    fun `signInWithGoogle signs in and logs a login with the google method`() = runTest {
        every { remote.getCurrentUserId() } returns "u9"

        repo.signInWithGoogle(idToken = "tok", rawNonce = "n")

        coVerify { remote.signInWithGoogle("tok", "n") }
        verify { crashReporter.setUserId("u9") }
        verify {
            analyticsTracker.logEvent(
                AnalyticsEvents.LOGIN,
                mapOf(AnalyticsEvents.PARAM_METHOD to AnalyticsEvents.METHOD_GOOGLE),
            )
        }
    }

    // ── sign out ──────────────────────────────────────────────────────────────

    @Test
    fun `signOut signs out locally, clears the crash reporter user and logs a logout`() = runTest {
        repo.signOut()

        coVerify { remote.signOut(false) }
        verify { crashReporter.setUserId(null) }
        verify { analyticsTracker.logEvent(AnalyticsEvents.LOGOUT) }
    }

    @Test
    fun `signOutAll signs out of every device`() = runTest {
        repo.signOutAll()

        coVerify { remote.signOut(true) }
        verify { crashReporter.setUserId(null) }
        verify { analyticsTracker.logEvent(AnalyticsEvents.LOGOUT) }
    }

    // ── delete account ────────────────────────────────────────────────────────

    @Test
    fun `deleteAccount deletes remotely first, then wipes everything local`() = runTest {
        every { remote.getCurrentUserId() } returns "u1"

        repo.deleteAccount()

        coVerifyOrder {
            remote.deleteAccount()
            chatDatabase.clearAllTables()
            E2EEKeyManager.deleteKeyPair("u1")
            sessionGuard.clearSession()
            remote.clearLocalSession()
        }
    }

    @Test
    fun `deleteAccount leaves local data untouched when the server call fails`() = runTest {
        every { remote.getCurrentUserId() } returns "u1"
        coEvery { remote.deleteAccount() } throws IllegalStateException("429")

        val result = runCatching { repo.deleteAccount() }

        assertTrue(result.isFailure)
        verify(exactly = 0) { chatDatabase.clearAllTables() }
        verify(exactly = 0) { E2EEKeyManager.deleteKeyPair(any()) }
        verify(exactly = 0) { sessionGuard.clearSession() }
        coVerify(exactly = 0) { remote.clearLocalSession() }
    }

    @Test
    fun `deleteAccount skips the key pair when there was no signed-in user id`() = runTest {
        every { remote.getCurrentUserId() } returns null

        repo.deleteAccount()

        verify(exactly = 0) { E2EEKeyManager.deleteKeyPair(any()) }
        verify { sessionGuard.clearSession() }
    }

    // ── integrity ─────────────────────────────────────────────────────────────

    @Test
    fun `checkIntegrity runs the check with the application context`() = runTest {
        coEvery { remote.checkIntegrity(context) } returns IntegrityResultBO.Failed("rooted")

        assertEquals(IntegrityResultBO.Failed("rooted"), repo.checkIntegrity())
    }

    // ── MFA ───────────────────────────────────────────────────────────────────

    @Test
    fun `getMfaAssuranceLevel maps the remote levels`() = runTest {
        coEvery { remote.getMfaAssuranceLevel() } returns MfaAssuranceDTO(current = "aal1", next = "aal2")

        assertEquals(MfaAssuranceLevel(current = "aal1", next = "aal2"), repo.getMfaAssuranceLevel())
    }

    @Test
    fun `enrollTotp maps the enrollment data`() = runTest {
        coEvery { remote.enrollTotp() } returns TotpEnrollmentDTO(factorId = "f1", qrCodeSvg = "<svg/>", secret = "ABC")

        assertEquals(TotpEnrollment(factorId = "f1", qrCodeSvg = "<svg/>", secret = "ABC"), repo.enrollTotp())
    }

    @Test
    fun `MFA challenge calls are passed straight to the remote source`() = runTest {
        coEvery { remote.getVerifiedTotpFactorId() } returns "f1"
        coEvery { remote.createMfaChallenge("f1") } returns "ch1"

        assertEquals("f1", repo.getVerifiedTotpFactorId())
        assertEquals("ch1", repo.createMfaChallenge("f1"))
        repo.verifyMfaChallenge("f1", "ch1", "123456")
        repo.unenrollFactor("f1")

        coVerify { remote.verifyMfaChallenge("f1", "ch1", "123456") }
        coVerify { remote.unenrollFactor("f1") }
    }
}
