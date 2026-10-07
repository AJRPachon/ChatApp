package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.local.entity.ReactionDBO
import com.ajrpachon.chatapp.data.remote.dto.ReactionRemoteDTO
import com.ajrpachon.chatapp.domain.model.ReactionBO
import org.junit.Assert.assertEquals
import org.junit.Test

class ReactionMapperTest {

    @Test
    fun `remote and local reactions map to the same model`() {
        val expected = ReactionBO(messageId = "m1", userId = "u1", emoji = "👍")

        assertEquals(expected, ReactionRemoteDTO("m1", "u1", "👍").toBO())
        assertEquals(expected, ReactionDBO("m1", "u1", "👍").toBO())
    }
}
