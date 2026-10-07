package com.ajrpachon.chatapp.data.repository

import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class FirebaseCrashReporterTest {

    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
    private val reporter = FirebaseCrashReporter(crashlytics)

    @Test
    fun `exceptions and breadcrumbs are forwarded`() {
        val failure = IllegalStateException("boom")

        reporter.recordException(failure)
        reporter.log("something happened")
        reporter.setCustomKey("screen", "chat")

        verify { crashlytics.recordException(failure) }
        verify { crashlytics.log("something happened") }
        verify { crashlytics.setCustomKey("screen", "chat") }
    }

    @Test
    fun `a missing user id is reported as an empty string`() {
        reporter.setUserId("u1")
        reporter.setUserId(null)

        verify { crashlytics.setUserId("u1") }
        verify { crashlytics.setUserId("") }
    }
}
