package com.ajrpachon.chatapp.data.repository

import android.app.Application
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class FirebaseAnalyticsTrackerTest {

    private val analytics = mockk<FirebaseAnalytics>(relaxed = true)
    private val tracker = FirebaseAnalyticsTracker(analytics)

    @Test
    fun `logEvent keeps each parameter with its own type`() {
        val bundle = slot<Bundle>()

        tracker.logEvent(
            "sample_event",
            mapOf("text" to "hola", "count" to 3, "big" to 5_000_000_000L, "ratio" to 0.5, "flag" to true),
        )

        verify { analytics.logEvent(eq("sample_event"), capture(bundle)) }
        with(bundle.captured) {
            assertEquals("hola", getString("text"))
            assertEquals(3, getInt("count"))
            assertEquals(5_000_000_000L, getLong("big"))
            assertEquals(0.5, getDouble("ratio"), 0.0)
            assertEquals(true, getBoolean("flag"))
        }
    }

    @Test
    fun `a parameter of any other type is sent as its string form`() {
        val bundle = slot<Bundle>()

        tracker.logEvent("sample_event", mapOf("list" to listOf(1, 2)))

        verify { analytics.logEvent(eq("sample_event"), capture(bundle)) }
        assertEquals("[1, 2]", bundle.captured.getString("list"))
    }

    @Test
    fun `logEvent without parameters sends an empty bundle`() {
        val bundle = slot<Bundle>()

        tracker.logEvent("bare_event")

        verify { analytics.logEvent(eq("bare_event"), capture(bundle)) }
        assertEquals(0, bundle.captured.size())
    }

    @Test
    fun `user property and user id are forwarded`() {
        tracker.setUserProperty("theme", "dark")
        tracker.setUserId("u1")
        tracker.setUserId(null)

        verify { analytics.setUserProperty("theme", "dark") }
        verify { analytics.setUserId("u1") }
        verify { analytics.setUserId(null) }
    }
}
