package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.emoji.EmojiCategoryDTO
import com.ajrpachon.chatapp.data.local.entity.ReactionDBO
import com.ajrpachon.chatapp.data.local.entity.ScheduledMessageDBO
import com.ajrpachon.chatapp.data.remote.dto.AuthSessionDTO
import com.ajrpachon.chatapp.data.remote.dto.GiphyGifDTO
import com.ajrpachon.chatapp.data.remote.dto.GiphyImageDataDTO
import com.ajrpachon.chatapp.data.remote.dto.GiphyImagesDTO
import com.ajrpachon.chatapp.data.remote.dto.MfaAssuranceDTO
import com.ajrpachon.chatapp.data.remote.dto.ReactionRemoteDTO
import com.ajrpachon.chatapp.data.remote.dto.TotpEnrollmentDTO
import com.ajrpachon.chatapp.domain.model.EmojiCategoryBO
import com.ajrpachon.chatapp.domain.model.GiphyGifBO
import com.ajrpachon.chatapp.domain.model.ReactionBO
import com.ajrpachon.chatapp.domain.model.ScheduledMessageBO
import com.ajrpachon.chatapp.domain.repository.MfaAssuranceLevel
import com.ajrpachon.chatapp.domain.repository.SessionInfo
import com.ajrpachon.chatapp.domain.repository.TotpEnrollment
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthMapperTest {

    @Test
    fun `session keeps the user id and a nullable email`() {
        assertEquals(SessionInfo("u1", "a@b.c"), AuthSessionDTO("u1", "a@b.c").toBO())
        assertEquals(SessionInfo("u1", null), AuthSessionDTO("u1", null).toBO())
    }

    @Test
    fun `mfa assurance keeps current and next levels`() {
        assertEquals(MfaAssuranceLevel("aal1", "aal2"), MfaAssuranceDTO("aal1", "aal2").toBO())
    }

    @Test
    fun `totp enrollment keeps factor id, qr and secret`() {
        assertEquals(
            TotpEnrollment("f1", "<svg/>", "JBSWY3DP"),
            TotpEnrollmentDTO("f1", "<svg/>", "JBSWY3DP").toBO(),
        )
    }
}

class EmojiMapperTest {

    @Test
    fun `category maps its name, icon and emojis in order`() {
        val dto = EmojiCategoryDTO(category = "Caras", icon = "😀", emojis = listOf("😀", "😂", "🙂"))

        assertEquals(EmojiCategoryBO("Caras", "😀", listOf("😀", "😂", "🙂")), dto.toDomain())
    }
}

class GiphyMapperTest {

    @Test
    fun `gif uses the small fixed-height image as preview and the original as full`() {
        val dto = GiphyGifDTO(
            images = GiphyImagesDTO(
                fixedHeightSmall = GiphyImageDataDTO("https://g/small.gif"),
                original = GiphyImageDataDTO("https://g/original.gif"),
            ),
        )

        assertEquals(GiphyGifBO(previewUrl = "https://g/small.gif", fullUrl = "https://g/original.gif"), dto.toDomain())
    }
}

class ReactionMapperTest {

    @Test
    fun `remote and local reactions map to the same model`() {
        val expected = ReactionBO(messageId = "m1", userId = "u1", emoji = "👍")

        assertEquals(expected, ReactionRemoteDTO("m1", "u1", "👍").toBO())
        assertEquals(expected, ReactionDBO("m1", "u1", "👍").toBO())
    }
}

class ScheduledMessageMapperTest {

    @Test
    fun `entity maps every field to the domain model`() {
        val dbo = ScheduledMessageDBO("id1", "c1", "me", "hola", scheduledAtMs = 100L, createdAt = 50L)

        assertEquals(ScheduledMessageBO("id1", "c1", "me", "hola", scheduledAtMs = 100L, createdAt = 50L), dbo.toDomain())
    }
}
