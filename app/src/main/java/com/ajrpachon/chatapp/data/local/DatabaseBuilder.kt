package com.ajrpachon.chatapp.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlinx.coroutines.Dispatchers
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE messages ADD COLUMN imageUrl TEXT")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE conversations ADD COLUMN otherUserId TEXT")
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE messages ADD COLUMN audioUrl TEXT")
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE messages ADD COLUMN replyToId TEXT")
        connection.execSQL("ALTER TABLE messages ADD COLUMN replyToContent TEXT")
        connection.execSQL("ALTER TABLE messages ADD COLUMN replyToSenderName TEXT")
    }
}

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE messages ADD COLUMN callType TEXT")
        connection.execSQL("ALTER TABLE messages ADD COLUMN callStatus TEXT")
        connection.execSQL("ALTER TABLE messages ADD COLUMN callDuration INTEGER")
    }
}

private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE messages ADD COLUMN gifUrl TEXT")
        connection.execSQL("ALTER TABLE messages ADD COLUMN stickerUrl TEXT")
    }
}

private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE conversations ADD COLUMN description TEXT")
        connection.execSQL("ALTER TABLE conversations ADD COLUMN groupAvatarUrl TEXT")
        connection.execSQL(
            """CREATE TABLE IF NOT EXISTS group_members (
                conversationId TEXT NOT NULL,
                userId TEXT NOT NULL,
                displayName TEXT NOT NULL,
                username TEXT NOT NULL,
                avatarUrl TEXT,
                role TEXT NOT NULL,
                joinedAt INTEGER NOT NULL,
                PRIMARY KEY(conversationId, userId)
            )"""
        )
    }
}

private val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE conversations ADD COLUMN isMuted INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE conversations ADD COLUMN historyVisibleFrom INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE messages ADD COLUMN isEncrypted INTEGER NOT NULL DEFAULT 0")
    }
}

private val migration11To12 = object : Migration(11, 12) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE users ADD COLUMN lastSeen INTEGER")
        connection.execSQL("ALTER TABLE users ADD COLUMN showOnlineStatus INTEGER NOT NULL DEFAULT 1")
    }
}

private val migration12To13 = object : Migration(12, 13) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE messages ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
    }
}

private val migration13To14 = object : Migration(13, 14) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE messages ADD COLUMN isEdited INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE messages ADD COLUMN editedAt INTEGER")
    }
}

private val migration16To17 = object : Migration(16, 17) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE messages ADD COLUMN expiresAt INTEGER")
    }
}

private val migration17To18 = object : Migration(17, 18) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE messages ADD COLUMN fileUrl TEXT")
        connection.execSQL("ALTER TABLE messages ADD COLUMN fileName TEXT")
        connection.execSQL("ALTER TABLE messages ADD COLUMN fileSize INTEGER")
        connection.execSQL("ALTER TABLE messages ADD COLUMN fileMimeType TEXT")
    }
}

private val migration18To19 = object : Migration(18, 19) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE messages ADD COLUMN videoUrl TEXT")
    }
}

private val migration19To20 = object : Migration(19, 20) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """CREATE TABLE IF NOT EXISTS user_status (
                id TEXT NOT NULL PRIMARY KEY,
                userId TEXT NOT NULL,
                text TEXT,
                imageUrl TEXT,
                backgroundColor INTEGER NOT NULL DEFAULT ${0xFF1976D2L},
                createdAt INTEGER NOT NULL,
                expiresAt INTEGER NOT NULL
            )"""
        )
    }
}

private val migration15To16 = object : Migration(15, 16) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE conversations ADD COLUMN mutedUntil INTEGER NOT NULL DEFAULT 0")
    }
}

private val migration20To21 = object : Migration(20, 21) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE conversations ADD COLUMN is_archived INTEGER NOT NULL DEFAULT 0")
    }
}

private val migration21To22 = object : Migration(21, 22) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE messages ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
    }
}

private val migration22To23 = object : Migration(22, 23) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE messages ADD COLUMN isSaved INTEGER NOT NULL DEFAULT 0")
    }
}

private val migration23To24 = object : Migration(23, 24) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """CREATE TABLE IF NOT EXISTS polls (
                id TEXT NOT NULL PRIMARY KEY,
                conversationId TEXT NOT NULL,
                question TEXT NOT NULL,
                createdBy TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )"""
        )
        connection.execSQL(
            """CREATE TABLE IF NOT EXISTS poll_options (
                id TEXT NOT NULL PRIMARY KEY,
                pollId TEXT NOT NULL,
                text TEXT NOT NULL,
                voteCount INTEGER NOT NULL DEFAULT 0
            )"""
        )
        connection.execSQL(
            """CREATE TABLE IF NOT EXISTS poll_votes (
                pollId TEXT NOT NULL,
                userId TEXT NOT NULL,
                optionId TEXT NOT NULL,
                PRIMARY KEY(pollId, userId)
            )"""
        )
    }
}

private val migration24To25 = object : Migration(24, 25) {
    override fun migrate(connection: SQLiteConnection) {
        val base = "https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72"
        connection.execSQL(
            """CREATE TABLE IF NOT EXISTS sticker_packs (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                coverUrl TEXT NOT NULL,
                is_installed INTEGER NOT NULL DEFAULT 0
            )"""
        )
        connection.execSQL(
            """CREATE TABLE IF NOT EXISTS stickers (
                id TEXT NOT NULL PRIMARY KEY,
                pack_id TEXT NOT NULL,
                imageUrl TEXT NOT NULL,
                tags TEXT NOT NULL DEFAULT '',
                FOREIGN KEY(pack_id) REFERENCES sticker_packs(id) ON DELETE CASCADE
            )"""
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_stickers_pack_id ON stickers(pack_id)")

        connection.execSQL("INSERT OR IGNORE INTO sticker_packs VALUES('pack_animals','Lindos Animales','$base/1f436.png',1)")
        listOf("s_a1" to "1f436","s_a2" to "1f431","s_a3" to "1f43b","s_a4" to "1f98a","s_a5" to "1f43c","s_a6" to "1f981","s_a7" to "1f42f","s_a8" to "1f428","s_a9" to "1f438","s_a10" to "1f419").forEach { (id, code) ->
            connection.execSQL("INSERT OR IGNORE INTO stickers VALUES('$id','pack_animals','$base/$code.png','animales')")
        }

        connection.execSQL("INSERT OR IGNORE INTO sticker_packs VALUES('pack_faces','Expresiones','$base/1f602.png',1)")
        listOf("s_f1" to "1f602","s_f2" to "1f970","s_f3" to "1f62d","s_f4" to "1f624","s_f5" to "1f929","s_f6" to "1f60e","s_f7" to "1f97a","s_f8" to "1f631","s_f9" to "1f914","s_f10" to "1f644").forEach { (id, code) ->
            connection.execSQL("INSERT OR IGNORE INTO stickers VALUES('$id','pack_faces','$base/$code.png','expresiones caras')")
        }

        connection.execSQL("INSERT OR IGNORE INTO sticker_packs VALUES('pack_party','Celebración','$base/1f389.png',1)")
        listOf("s_p1" to "1f389","s_p2" to "1f38a","s_p3" to "1f3c6","s_p4" to "2b50","s_p5" to "1f525","s_p6" to "1f4af","s_p7" to "1f388","s_p8" to "1f942","s_p9" to "1f37e","s_p10" to "1f381").forEach { (id, code) ->
            connection.execSQL("INSERT OR IGNORE INTO stickers VALUES('$id','pack_party','$base/$code.png','celebración fiesta')")
        }

        connection.execSQL("INSERT OR IGNORE INTO sticker_packs VALUES('pack_travel','Viajes','$base/2708.png',0)")
        listOf("s_t1" to "2708","s_t2" to "1f30d","s_t3" to "1f3d6","s_t4" to "1f5fc","s_t5" to "1f3df","s_t6" to "1f6eb","s_t7" to "1f30a","s_t8" to "26fa","s_t9" to "1f3a1","s_t10" to "1f697").forEach { (id, code) ->
            connection.execSQL("INSERT OR IGNORE INTO stickers VALUES('$id','pack_travel','$base/$code.png','viajes aventura')")
        }
    }
}

private val migration25To26 = object : Migration(25, 26) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE conversations ADD COLUMN disappearing_mode_seconds INTEGER NOT NULL DEFAULT 0")
    }
}

private val migration14To15 = object : Migration(14, 15) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """CREATE TABLE IF NOT EXISTS message_reactions (
                messageId TEXT NOT NULL,
                userId TEXT NOT NULL,
                emoji TEXT NOT NULL,
                PRIMARY KEY(messageId, userId, emoji)
            )"""
        )
    }
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
        .addMigrations(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
            MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
            MIGRATION_9_10, MIGRATION_10_11, migration11To12,
            migration12To13, migration13To14, migration14To15, migration15To16, migration16To17,
            migration17To18, migration18To19, migration19To20, migration20To21, migration21To22, migration22To23, migration23To24, migration24To25, migration25To26, migration26To27, migration27To28, migration28To29, migration29To30, migration30To31, migration31To32,
        )
        .build()
}

private val migration26To27 = object : Migration(26, 27) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """CREATE TABLE IF NOT EXISTS message_read_receipts (
                messageId TEXT NOT NULL,
                userId TEXT NOT NULL,
                readAt INTEGER NOT NULL,
                PRIMARY KEY(messageId, userId)
            )"""
        )
    }
}

private val migration27To28 = object : Migration(27, 28) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """CREATE TABLE IF NOT EXISTS folders (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                colorHex TEXT NOT NULL DEFAULT '#6200EE',
                sortOrder INTEGER NOT NULL DEFAULT 0
            )"""
        )
        connection.execSQL(
            """CREATE TABLE IF NOT EXISTS folder_conversations (
                folderId TEXT NOT NULL,
                conversationId TEXT NOT NULL,
                PRIMARY KEY(folderId, conversationId)
            )"""
        )
    }
}

private val migration28To29 = object : Migration(28, 29) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """CREATE TABLE IF NOT EXISTS broadcast_lists (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )"""
        )
        connection.execSQL(
            """CREATE TABLE IF NOT EXISTS broadcast_list_members (
                listId TEXT NOT NULL,
                userId TEXT NOT NULL,
                PRIMARY KEY(listId, userId)
            )"""
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_broadcast_list_members_listId ON broadcast_list_members(listId)")
    }
}

private val migration31To32 = object : Migration(31, 32) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """CREATE TABLE IF NOT EXISTS scheduled_messages (
                id TEXT NOT NULL PRIMARY KEY,
                conversationId TEXT NOT NULL,
                senderId TEXT NOT NULL,
                text TEXT NOT NULL,
                scheduledAtMs INTEGER NOT NULL,
                createdAt INTEGER NOT NULL
            )"""
        )
    }
}

private val migration30To31 = object : Migration(30, 31) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """CREATE TABLE IF NOT EXISTS active_sessions (
                id TEXT NOT NULL PRIMARY KEY,
                deviceInfo TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                lastActiveAt INTEGER NOT NULL,
                isCurrent INTEGER NOT NULL DEFAULT 0
            )"""
        )
    }
}

private val migration29To30 = object : Migration(29, 30) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """CREATE TABLE IF NOT EXISTS chat_events (
                id TEXT NOT NULL PRIMARY KEY,
                conversationId TEXT NOT NULL,
                title TEXT NOT NULL,
                dateMs INTEGER NOT NULL,
                location TEXT,
                createdBy TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )"""
        )
        connection.execSQL(
            """CREATE TABLE IF NOT EXISTS event_rsvps (
                eventId TEXT NOT NULL,
                userId TEXT NOT NULL,
                status TEXT NOT NULL,
                PRIMARY KEY(eventId, userId)
            )"""
        )
    }
}
