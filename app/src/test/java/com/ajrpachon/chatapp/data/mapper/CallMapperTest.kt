package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.remote.dto.CallDTO
import com.ajrpachon.chatapp.domain.model.CallStatus
import com.ajrpachon.chatapp.domain.model.CallType
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CallMapperTest {

    private fun dto(type: String = "video", status: String = "active", createdAt: String? = "2026-10-05T10:00:00Z") = CallDTO(
        id = "c1",
        conversationId = "conv1",
        callerId = "u1",
        calleeId = "u2",
        type = type,
        status = status,
        roomName = "room-1",
        createdAt = createdAt,
    )

    @Test
    fun `toBO maps ids, room, wire values and the caller name it is given`() {
        val bo = dto().toBO(callerName = "Ana")

        assertEquals("c1", bo.id)
        assertEquals("conv1", bo.conversationId)
        assertEquals("u1", bo.callerId)
        assertEquals("Ana", bo.callerName)
        assertEquals("u2", bo.calleeId)
        assertEquals("room-1", bo.roomName)
        assertEquals(CallType.VIDEO, bo.type)
        assertEquals(CallStatus.ACTIVE, bo.status)
        assertEquals(Instant.parse("2026-10-05T10:00:00Z"), bo.createdAt)
    }

    @Test
    fun `toBO defaults the caller name to empty`() {
        assertEquals("", dto().toBO().callerName)
    }

    @Test
    fun `toBO falls back for unknown wire values`() {
        val bo = dto(type = "hologram", status = "levitating").toBO()

        assertEquals(CallType.AUDIO, bo.type)
        assertEquals(CallStatus.RINGING, bo.status)
    }

    @Test
    fun `toBO gives a null createdAt when it is missing or malformed`() {
        assertNull(dto(createdAt = null).toBO().createdAt)
        assertNull(dto(createdAt = "not a date").toBO().createdAt)
    }
}
