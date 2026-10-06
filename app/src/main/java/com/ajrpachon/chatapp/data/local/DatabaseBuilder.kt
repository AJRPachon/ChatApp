package com.ajrpachon.chatapp.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import kotlinx.coroutines.Dispatchers
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

// Room schema history: the database was reset to version 1 on 2026-10-06, before the first public
// release, so there are no migrations yet. From the first published version on, every change to an
// entity needs a `Migration(X, Y)` registered with `addMigrations(...)` in buildChatDatabase and
// the exported `app/schemas/.../Y.json`. See docs/room-schema-history.md.

/** Fills the built-in sticker packs when the database file is first created. */
internal val stickerSeedCallback = object : RoomDatabase.Callback() {
    override fun onCreate(connection: SQLiteConnection) = seedStickerPacks(connection)
}

fun buildChatDatabase(context: Context): ChatDatabase {
    System.loadLibrary("sqlcipher")
    val passphrase = DatabaseKeyProvider.getPassphrase(context.applicationContext)
    return Room.databaseBuilder<ChatDatabase>(
        context = context.applicationContext,
        name = "chat.db",
    )
        .openHelperFactory(SupportOpenHelperFactory(passphrase))
        .setQueryCoroutineContext(Dispatchers.IO)
        .addCallback(stickerSeedCallback)
        .build()
}
