package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.local.entity.SessionDBO
import com.ajrpachon.chatapp.domain.model.SessionBO
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionMapperTest {

    private val dbo = SessionDBO(id = "s1", deviceInfo = "Pixel 9", createdAt = 1_000L, lastActiveAt = 2_000L, isCurrent = true)

    @Test
    fun `toBO maps every field`() {
        assertEquals(SessionBO(id = "s1", deviceInfo = "Pixel 9", createdAt = 1_000L, lastActiveAt = 2_000L, isCurrent = true), dbo.toBO())
    }

    @Test
    fun `toDBO round-trips with toBO`() {
        assertEquals(dbo, dbo.toBO().toDBO())
    }
}
