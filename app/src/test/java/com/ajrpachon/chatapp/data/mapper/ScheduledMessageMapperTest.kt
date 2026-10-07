package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.local.entity.ScheduledMessageDBO
import com.ajrpachon.chatapp.domain.model.ScheduledMessageBO
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduledMessageMapperTest {

    @Test
    fun `entity maps every field to the domain model`() {
        val dbo = ScheduledMessageDBO("id1", "c1", "me", "hola", scheduledAtMs = 100L, createdAt = 50L)

        assertEquals(ScheduledMessageBO("id1", "c1", "me", "hola", scheduledAtMs = 100L, createdAt = 50L), dbo.toDomain())
    }
}
