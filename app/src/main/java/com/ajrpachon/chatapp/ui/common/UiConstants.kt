package com.ajrpachon.chatapp.ui.common

import android.Manifest

object CallPermissions {
    val VIDEO = listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
    val AUDIO = listOf(Manifest.permission.RECORD_AUDIO)
    fun forCallType(callType: String): List<String> =
        if (callType == "video") VIDEO else AUDIO
}

object ChatConstants {
    const val DRAFT_PREFIX = "Borrador: "
    const val MAX_UNREAD_DISPLAY = 99
    const val MAX_UNREAD_LABEL = "99+"
    const val SCHEDULED_MESSAGE_DELAY_MS = 1500L
    const val STORY_DURATION_MS = 5_000L
    const val DEEP_LINK_SCHEME = "chatapp"
    const val DEEP_LINK_USER_HOST = "user"
}
