package com.ajrpachon.chatapp.ui.call

import com.ajrpachon.chatapp.domain.model.CallStatus
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.model.OutgoingMessageBO
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
import com.ajrpachon.chatapp.domain.repository.CallRepository
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.domain.usecase.SendMessageUseCase
import com.ajrpachon.chatapp.util.MainDispatcherRule
import io.livekit.android.events.EventListenable
import io.livekit.android.events.ParticipantEvent
import io.livekit.android.events.RoomEvent
import io.livekit.android.room.Room
import io.livekit.android.room.participant.LocalParticipant
import io.livekit.android.room.track.screencapture.ScreenCaptureParams
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import java.util.concurrent.TimeUnit

class CallViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val timeout: Timeout = Timeout(30, TimeUnit.SECONDS)

    private val roomFactory = mockk<LiveKitRoomFactory>()
    private val callRepository = mockk<CallRepository>(relaxed = true)
    private val getCurrentUser = mockk<GetCurrentUserUseCase>()
    private val sendMessage = mockk<SendMessageUseCase>()
    private val analytics = mockk<AnalyticsTracker>(relaxed = true)

    private val room = mockk<Room>(relaxed = true)
    private val localParticipant = mockk<LocalParticipant>(relaxed = true)
    private val roomEvents = MutableSharedFlow<RoomEvent>(extraBufferCapacity = 8)
    private val participantEvents = MutableSharedFlow<ParticipantEvent>(extraBufferCapacity = 8)
    private val hangupSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    private val remoteStatus = MutableSharedFlow<CallStatus>(extraBufferCapacity = 8)
    private val sentMessages = mutableListOf<OutgoingMessageBO>()

    @Before
    fun setUp() {
        every { getCurrentUser() } returns MutableStateFlow(
            UserBO("me", "me@test.com", "me", "Me", null, Instant.fromEpochMilliseconds(0)),
        )
        every { roomFactory.create() } returns room
        every { room.events } returns object : EventListenable<RoomEvent> {
            override val events = roomEvents
        }
        every { room.localParticipant } returns localParticipant
        every { localParticipant.events } returns object : EventListenable<ParticipantEvent> {
            override val events = participantEvents
        }
        every { room.remoteParticipants } returns emptyMap()
        coEvery { callRepository.fetchLivekitToken(any(), any()) } returns "token"
        every { callRepository.observeHangupSignal(any()) } returns hangupSignal
        every { callRepository.observeCallStatus(any()) } returns remoteStatus
        coEvery { sendMessage(capture(sentMessages)) } returns Result.success(mockk<MessageBO>())
    }

    private fun buildViewModel(
        isOutgoing: Boolean = true,
        isGroup: Boolean = false,
        callType: String = "audio",
    ) = CallViewModel(
        args = CallArgs(
            callId = "call1",
            conversationId = "c1",
            roomName = "room1",
            callType = callType,
            isOutgoing = isOutgoing,
            isGroup = isGroup,
        ),
        roomFactory = roomFactory,
        callRepository = callRepository,
        getCurrentUserUseCase = getCurrentUser,
        sendMessageUseCase = sendMessage,
        analyticsTracker = analytics,
        livekitUrl = "wss://example.test",
    )

    @Test
    fun `an outgoing call connects to the room and starts ringing`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel(isOutgoing = true)
        runCurrent()

        assertEquals(CallPhase.RINGING, vm.state.value.phase)
        assertEquals(room, vm.state.value.room)
        coVerify { callRepository.fetchLivekitToken("room1", "me") }
        coVerify { room.connect("wss://example.test", "token", any()) }
        coVerify(exactly = 0) { callRepository.acceptCall(any()) }
    }

    @Test
    fun `an incoming call accepts after connecting and becomes active`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel(isOutgoing = false)
        runCurrent()

        assertEquals(CallPhase.ACTIVE, vm.state.value.phase)
        coVerify { callRepository.acceptCall("call1") }
        vm.hangUp() // stops the duration timer so runTest can finish
    }

    @Test
    fun `an incoming call counts its duration in seconds`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel(isOutgoing = false)
        runCurrent()

        advanceTimeBy(3_100)

        assertEquals(3, vm.state.value.durationSeconds)
        vm.hangUp()
    }

    @Test
    fun `a failure to accept does not stop an incoming call from becoming active`() =
        runTest(mainDispatcherRule.scheduler) {
            coEvery { callRepository.acceptCall(any()) } throws IllegalStateException("db down")

            val vm = buildViewModel(isOutgoing = false)
            runCurrent()

            assertEquals(CallPhase.ACTIVE, vm.state.value.phase)
            vm.hangUp()
        }

    @Test
    fun `a failure to fetch the token ends in the error phase with the message`() =
        runTest(mainDispatcherRule.scheduler) {
            coEvery { callRepository.fetchLivekitToken(any(), any()) } throws IllegalStateException("token denied")

            val vm = buildViewModel()
            runCurrent()

            assertEquals(CallPhase.ERROR, vm.state.value.phase)
            assertEquals("token denied", vm.state.value.error)
        }

    @Test
    fun `waiting too long for the current user ends in the error phase`() = runTest(mainDispatcherRule.scheduler) {
        every { getCurrentUser() } returns emptyFlow()

        val vm = buildViewModel()
        runCurrent()
        advanceTimeBy(5_100)

        assertEquals(CallPhase.ERROR, vm.state.value.phase)
        assertNotNull(vm.state.value.error)
    }

    @Test
    fun `an unanswered outgoing call is hung up as missed after twenty seconds`() =
        runTest(mainDispatcherRule.scheduler) {
            val vm = buildViewModel()
            runCurrent()

            advanceTimeBy(20_100)

            assertEquals(CallPhase.ENDED, vm.state.value.phase)
            coVerify(timeout = IO_TIMEOUT_MS) { callRepository.endCall("call1") }
            coVerify(timeout = IO_TIMEOUT_MS) { sendMessage(any()) }
            assertEquals("missed", sentMessages.single().callStatus)
        }

    @Test
    fun `hanging up an active call sends the hangup signal, ends it and logs it with its duration`() =
        runTest(mainDispatcherRule.scheduler) {
            val vm = buildViewModel(isOutgoing = true)
            runCurrent()
            roomEvents.emit(participantConnected())
            runCurrent()
            assertEquals(CallPhase.ACTIVE, vm.state.value.phase)
            advanceTimeBy(2_100)

            vm.hangUp()
            runCurrent()

            assertEquals(CallPhase.ENDED, vm.state.value.phase)
            coVerify(timeout = IO_TIMEOUT_MS) { callRepository.sendHangupSignal("call1") }
            coVerify(timeout = IO_TIMEOUT_MS) { callRepository.endCall("call1") }
            coVerify(timeout = IO_TIMEOUT_MS) { sendMessage(any()) }
            val message = sentMessages.single()
            assertEquals("ended", message.callStatus)
            assertEquals(2, message.callDuration)
            assertEquals("c1", message.conversationId)
            assertEquals("me", message.senderId)
        }

    @Test
    fun `hanging up twice only ends the call once`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        runCurrent()

        vm.hangUp()
        vm.hangUp()
        runCurrent()

        coVerify(timeout = IO_TIMEOUT_MS, exactly = 1) { callRepository.endCall("call1") }
    }

    @Test
    fun `the callee never writes the call summary message`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel(isOutgoing = false)
        runCurrent()

        vm.hangUp()
        runCurrent()

        coVerify(timeout = IO_TIMEOUT_MS) { callRepository.endCall("call1") }
        coVerify(exactly = 0) { sendMessage(any()) }
    }

    @Test
    fun `a remote hangup signal ends the call`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        runCurrent()

        hangupSignal.emit(Unit)
        runCurrent()

        assertEquals(CallPhase.ENDED, vm.state.value.phase)
        verify { room.disconnect() }
        coVerify(timeout = IO_TIMEOUT_MS) { sendMessage(any()) }
    }

    @Test
    fun `the other side rejecting is written as a rejected call`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        runCurrent()

        remoteStatus.emit(CallStatus.REJECTED)
        runCurrent()

        assertEquals(CallPhase.ENDED, vm.state.value.phase)
        coVerify(timeout = IO_TIMEOUT_MS) { sendMessage(any()) }
        assertEquals("rejected", sentMessages.single().callStatus)
    }

    @Test
    fun `other remote statuses do not end the call`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        runCurrent()

        remoteStatus.emit(CallStatus.RINGING)
        remoteStatus.emit(CallStatus.ACTIVE)
        runCurrent()

        assertEquals(CallPhase.RINGING, vm.state.value.phase)
    }

    @Test
    fun `a group call does not listen for the one-to-one hangup signal or status`() =
        runTest(mainDispatcherRule.scheduler) {
            buildViewModel(isGroup = true)
            runCurrent()

            verify(exactly = 0) { callRepository.observeHangupSignal(any()) }
            verify(exactly = 0) { callRepository.observeCallStatus(any()) }
        }

    @Test
    fun `a group call only writes a summary when nobody joined`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel(isGroup = true)
        runCurrent()
        roomEvents.emit(participantConnected())
        runCurrent()

        vm.hangUp()
        runCurrent()

        coVerify(timeout = IO_TIMEOUT_MS) { callRepository.endCall("call1") }
        coVerify(exactly = 0) { sendMessage(any()) }
    }

    @Test
    fun `toggling the microphone flips the flag and tells the room`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        runCurrent()

        vm.toggleMic()
        runCurrent()
        assertTrue(vm.state.value.isMicMuted)
        coVerify { localParticipant.setMicrophoneEnabled(false) }

        vm.toggleMic()
        runCurrent()
        assertEquals(false, vm.state.value.isMicMuted)
        coVerify(atLeast = 2) { localParticipant.setMicrophoneEnabled(true) }
    }

    @Test
    fun `a video call enables the camera and an audio call does not`() = runTest(mainDispatcherRule.scheduler) {
        buildViewModel(callType = "audio")
        runCurrent()
        coVerify(exactly = 0) { localParticipant.setCameraEnabled(any()) }

        buildViewModel(callType = "video")
        runCurrent()
        coVerify { localParticipant.setCameraEnabled(true) }
    }

    @Test
    fun `toggling the screen share asks the screen for permission when off`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        runCurrent()

        vm.onIntent(CallIntent.ToggleScreenShare)
        runCurrent()

        assertEquals(CallEffect.RequestScreenShare, vm.effect.first())
    }

    @Test
    fun `starting and stopping the screen share updates the flag`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        runCurrent()

        vm.startScreenShare(mockk<ScreenCaptureParams>())
        runCurrent()
        assertTrue(vm.state.value.isScreenSharing)

        vm.onIntent(CallIntent.ToggleScreenShare)
        runCurrent()
        assertEquals(false, vm.state.value.isScreenSharing)
        coVerify { localParticipant.setScreenShareEnabled(false) }
    }

    @Test
    fun `a failed screen share leaves the flag off`() = runTest(mainDispatcherRule.scheduler) {
        coEvery { localParticipant.setScreenShareEnabled(true, any()) } throws IllegalStateException("denied")
        val vm = buildViewModel()
        runCurrent()

        vm.startScreenShare(mockk<ScreenCaptureParams>())
        runCurrent()

        assertEquals(false, vm.state.value.isScreenSharing)
    }

    @Test
    fun `the room is disconnected when the view model is cleared`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        runCurrent()

        val clear = CallViewModel::class.java.getDeclaredMethod("onCleared").apply { isAccessible = true }
        clear.invoke(vm)

        verify { room.disconnect() }
    }

    private fun participantConnected(): RoomEvent.ParticipantConnected {
        val event = mockk<RoomEvent.ParticipantConnected>(relaxed = true)
        every { event.participant } returns mockk(relaxed = true)
        return event
    }

    private companion object {
        const val IO_TIMEOUT_MS = 3_000L
    }
}
