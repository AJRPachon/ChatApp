package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.emoji.EmojiCategoryDTO
import com.ajrpachon.chatapp.domain.model.EmojiCategoryBO
import org.junit.Assert.assertEquals
import org.junit.Test

class EmojiMapperTest {

    @Test
    fun `category maps its name, icon and emojis in order`() {
        val dto = EmojiCategoryDTO(category = "Caras", icon = "😀", emojis = listOf("😀", "😂", "🙂"))

        assertEquals(EmojiCategoryBO("Caras", "😀", listOf("😀", "😂", "🙂")), dto.toDomain())
    }
}
