package com.ajrpachon.chatapp.ui.call

import android.app.Application
import io.livekit.android.LiveKit
import io.livekit.android.room.Room

/**
 * Creates the LiveKit [Room] for a call. `LiveKit.create` needs an application Context; keeping that
 * here lets [CallViewModel] stay free of Android types.
 */
class LiveKitRoomFactory(private val application: Application) {
    fun create(): Room = LiveKit.create(application)
}
