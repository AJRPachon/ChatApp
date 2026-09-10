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

/** Shared timing for the app's motion — one place so every animated spot (screen transitions,
 *  list item insert/move/remove, the chat input bar's mic↔send morph...) moves at a consistent,
 *  deliberately-chosen pace instead of each call site picking its own magic number. */
object MotionConstants {
    /** Screen push/pop (NavDisplay's transitionSpec/popTransitionSpec in MainActivity.kt). */
    const val NAV_TRANSITION_MS = 320
    /** LazyColumn item insert/move/remove (ConversationListScreen, ChatMessageList) — the
     *  animateItem() default is a bit slower/springier than this app's snappier feel calls for. */
    const val LIST_ITEM_ANIM_MS = 260
    /** Small in-place morphs — icon swaps, badges popping in, that kind of micro-interaction. */
    const val MICRO_ANIM_MS = 180
}
