package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.domain.model.CallBO
import com.ajrpachon.chatapp.domain.model.CallStatus
import com.ajrpachon.chatapp.domain.model.CallType
import com.ajrpachon.chatapp.util.MainDispatcherRule
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CallRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val supabase = mockk<SupabaseClient>(relaxed = true)
    private val auth = mockk<Auth>(relaxed = true)
    private val postgrest = mockk<Postgrest>(relaxed = true)

    init {
        every { supabase.auth } returns auth
        every { supabase.postgrest } returns postgrest
    }

    private val repo = CallRepositoryImpl(supabase)

    // ── createCall — throws when not authenticated ────────────────────────────

    @Test
    fun `createCall throws when user is not authenticated`() = runTest {
        every { auth.currentUserOrNull() } returns null

        assertFailsWith<IllegalStateException> {
            repo.createCall("conv1", "callee1", CallType.AUDIO)
        }
    }

    // ── CallBO construction — status mapping ──────────────────────────────────

    @Test
    fun `CallBO ringing status maps correctly`() {
        val bo = CallBO(
            id = "id1",
            conversationId = "conv1",
            callerId = "caller1",
            callerName = "Alice",
            calleeId = "callee1",
            type = CallType.AUDIO,
            status = CallStatus.RINGING,
            roomName = "room1",
        )
        assertEquals(CallStatus.RINGING, bo.status)
        assertEquals(CallType.AUDIO, bo.type)
    }

    @Test
    fun `CallBO with video type maps correctly`() {
        val bo = CallBO(
            id = "id2",
            conversationId = "conv1",
            callerId = "caller1",
            callerName = "Alice",
            calleeId = "callee1",
            type = CallType.VIDEO,
            status = CallStatus.ANSWERED,
            roomName = "room2",
        )
        assertEquals(CallType.VIDEO, bo.type)
        assertEquals(CallStatus.ANSWERED, bo.status)
    }
}
