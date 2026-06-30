package com.ajrpachon.chatapp.domain.model

import com.ajrpachon.chatapp.R

enum class NotificationSound(val resId: Int?) {
    DEFAULT(null),
    CHIME(R.raw.notif_chime),
    POP(R.raw.notif_pop),
    DING(R.raw.notif_ding),
    SILENT(null),
    ;

    val displayName: String
        get() = when (this) {
            DEFAULT -> "Predeterminado"
            CHIME -> "Campana"
            POP -> "Pop"
            DING -> "Ding"
            SILENT -> "Silencio"
        }
}
