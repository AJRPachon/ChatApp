package com.ajrpachon.chatapp.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

fun buildChatDatabase(context: Context): ChatDatabase =
    Room.databaseBuilder<ChatDatabase>(
        context = context.applicationContext,
        name = "chat.db",
    )
        .setDriver(BundledSQLiteDriver())
        .build()
