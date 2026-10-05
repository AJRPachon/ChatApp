package com.ajrpachon.chatapp.ui.call

import android.app.NotificationManager
import com.ajrpachon.chatapp.domain.model.CallBO
import com.ajrpachon.chatapp.domain.model.CallStatus
import com.ajrpachon.chatapp.domain.model.CallType
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.repository.CallRepository
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.service.FcmMessageHandler
import com.ajrpachon.chatapp.util.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class IncomingCallViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val notificationManager = mockk<NotificationManager>(relaxed = true)
    private val callRepository = mockk<CallRepository>(relaxed = true)
    private val getCurrentUser = mockk<GetCurrentUserUseCase>()

    private val incoming = MutableSharedFlow<CallBO>(extraBufferCapacity = 8)
    private val hangup = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    private val status = MutableSharedFlow<CallStatus>(extraBufferCapacity = 8)

    private fun call(id: String) = CallBO(
        id = id,
        conversationId = "c1",
        callerId = "u1",
        callerName = "Ana",
        calleeId = "me",
        type = CallType.AUDIO,
        status = CallStatus.RINGING,
        roomName = "room-$id",
    )

    @Before
    fun setUp() {
        every { getCurrentUser() } returns MutableStateFlow(
            UserBO("me", "me@test.com", "me", "Me", null, Instant.fromEpochMilliseconds(0)),
        )
        every { callRepository.observeIncomingCalls("me") } returns incoming
        every { callRepository.observeHangupSignal(any()) } returns hangup
        every { callRepository.observeCallStatus(any()) } returns status
    }

    private fun buildViewModel() = IncomingCallViewModel(notificationManager, callRepository, getCurrentUser)

    @Test
    fun `starts without an incoming call`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()

        assertNull(vm.state.value.incomingCall)
    }

    @Test
    fun `an incoming call from the repository is shown`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()

        incoming.emit(call("k1"))
        advanceUntilIdle()

        assertEquals("k1", vm.state.value.incomingCall?.id)
    }

    @Test
    fun `a hangup signal from the caller dismisses the call`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        incoming.emit(call("k1"))
        advanceUntilIdle()

        hangup.emit(Unit)
        advanceUntilIdle()

        assertNull(vm.state.value.incomingCall)
    }

    @Test
    fun `ended and rejected statuses dismiss the call but ringing does not`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        incoming.emit(call("k1"))
        advanceUntilIdle()

        status.emit(CallStatus.RINGING)
        advanceUntilIdle()
        assertEquals("k1", vm.state.value.incomingCall?.id)

        status.emit(CallStatus.ENDED)
        advanceUntilIdle()
        assertNull(vm.state.value.incomingCall)
    }

    @Test
    fun `a rejected status also dismisses the call`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        incoming.emit(call("k1"))
        advanceUntilIdle()

        status.emit(CallStatus.REJECTED)
        advanceUntilIdle()

        assertNull(vm.state.value.incomingCall)
    }

    @Test
    fun `a newer call replaces the one being shown`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()

        incoming.emit(call("k1"))
        advanceUntilIdle()
        incoming.emit(call("k2"))
        advanceUntilIdle()

        assertEquals("k2", vm.state.value.incomingCall?.id)
    }

    @Test
    fun `dismiss clears the call and cancels the notification`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        incoming.emit(call("k1"))
        advanceUntilIdle()

        vm.onIntent(IncomingCallIntent.Dismiss)

        assertNull(vm.state.value.incomingCall)
        verify { notificationManager.cancel(FcmMessageHandler.CALL_NOTIF_ID) }
    }

    @Test
    fun `accepting hands the call over to the call screen by dismissing it here`() =
        runTest(mainDispatcherRule.scheduler) {
            val vm = buildViewModel()
            advanceUntilIdle()
            incoming.emit(call("k1"))
            advanceUntilIdle()

            vm.onIntent(IncomingCallIntent.Accept("k1"))

            assertNull(vm.state.value.incomingCall)
            verify { notificationManager.cancel(FcmMessageHandler.CALL_NOTIF_ID) }
            coVerify(exactly = 0) { callRepository.rejectCall(any()) }
        }

    @Test
    fun `rejecting tells the repository, cancels the notification and dismisses the call`() =
        runTest(mainDispatcherRule.scheduler) {
            val vm = buildViewModel()
            advanceUntilIdle()
            incoming.emit(call("k1"))
            advanceUntilIdle()

            vm.onIntent(IncomingCallIntent.Reject("k1"))
            advanceUntilIdle()

            coVerify { callRepository.rejectCall("k1") }
            verify { notificationManager.cancel(FcmMessageHandler.CALL_NOTIF_ID) }
            assertNull(vm.state.value.incomingCall)
        }

    @Test
    fun `rejecting a call that is not the one on screen leaves the current call alone`() =
        runTest(mainDispatcherRule.scheduler) {
            val vm = buildViewModel()
            advanceUntilIdle()
            incoming.emit(call("k2"))
            advanceUntilIdle()

            vm.onIntent(IncomingCallIntent.Reject("k1"))
            advanceUntilIdle()

            coVerify { callRepository.rejectCall("k1") }
            assertEquals("k2", vm.state.value.incomingCall?.id)
        }

    @Test
    fun `a failing incoming-call subscription does not crash the view model`() = runTest(mainDispatcherRule.scheduler) {
        every { callRepository.observeIncomingCalls("me") } throws IllegalStateException("offline")

        val vm = buildViewModel()
        advanceUntilIdle()

        assertNull(vm.state.value.incomingCall)
    }

    @Test
    fun `no call is observed until there is a signed in user`() = runTest(mainDispatcherRule.scheduler) {
        every { getCurrentUser() } returns emptyFlow()

        buildViewModel()
        advanceUntilIdle()

        verify(exactly = 0) { callRepository.observeIncomingCalls(any()) }
    }
}
