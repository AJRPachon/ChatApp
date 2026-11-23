package com.ajrpachon.chatapp.ui.preview

import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.model.SendStatus

/**
 * Message variants for the chat bubble previews, built on [PreviewData.message]. Urls are
 * placeholders that no Coil request can resolve, so previews never touch the network.
 */
internal object PreviewChatBubbles {

    private const val PLACEHOLDER_URL = "https://preview.invalid"

    fun text(isFromMe: Boolean, isRead: Boolean = true): MessageBO =
        PreviewData.message("m1", "Hola, ¿quedamos mañana a las 10?", isFromMe, isRead)

    fun pending(): MessageBO = text(isFromMe = true, isRead = false).copy(sendStatus = SendStatus.PENDING)

    fun failed(): MessageBO = text(isFromMe = true, isRead = false).copy(sendStatus = SendStatus.FAILED)

    fun reply(isFromMe: Boolean): MessageBO = PreviewData.message("m2", "Perfecto, nos vemos allí", isFromMe).copy(
        replyToId = "m1",
        replyToContent = "Hola, ¿quedamos mañana a las 10?",
        replyToSenderName = PreviewData.OTHER_NAME,
    )

    fun edited(): MessageBO = text(isFromMe = true).copy(isEdited = true, editedAt = 0L)

    fun deleted(isFromMe: Boolean): MessageBO = text(isFromMe).copy(isDeleted = true)

    fun call(isFromMe: Boolean, status: String, type: String = "audio"): MessageBO =
        PreviewData.message("m3", "", isFromMe).copy(callType = type, callStatus = status, callDuration = 125)

    fun pdf(isFromMe: Boolean): MessageBO = PreviewData.message("m4", "", isFromMe).copy(
        fileUrl = "$PLACEHOLDER_URL/informe.pdf",
        fileName = "informe-trimestral.pdf",
        fileSize = 1_240_000L,
        fileMimeType = "application/pdf",
    )

    fun genericFile(isFromMe: Boolean): MessageBO = PreviewData.message("m5", "", isFromMe).copy(
        fileUrl = "$PLACEHOLDER_URL/datos.zip",
        fileName = "datos.zip",
        fileSize = 5_300_000L,
        fileMimeType = "application/zip",
    )

    fun video(isFromMe: Boolean): MessageBO = PreviewData.message("m6", "", isFromMe).copy(
        videoUrl = "$PLACEHOLDER_URL/video.mp4",
    )

    fun sticker(isFromMe: Boolean): MessageBO = PreviewData.message("m7", "", isFromMe).copy(
        stickerUrl = "$PLACEHOLDER_URL/sticker.webp",
    )

    fun contact(isFromMe: Boolean): MessageBO = PreviewData.message(
        "m8",
        "contact:{\"name\":\"Carlos Ruiz\",\"phone\":\"+34600111222\"}",
        isFromMe,
    )

    fun poll(isFromMe: Boolean): MessageBO = PreviewData.message("m9", "poll:poll1", isFromMe)

    fun imageGroup(isFromMe: Boolean, count: Int = 4): List<MessageBO> = List(count) { index ->
        PreviewData.message("img$index", "", isFromMe).copy(imageUrl = "$PLACEHOLDER_URL/foto$index.jpg")
    }
}
