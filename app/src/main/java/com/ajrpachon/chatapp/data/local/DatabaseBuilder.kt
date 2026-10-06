package com.ajrpachon.chatapp.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlinx.coroutines.Dispatchers
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

private val migration1To2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE messages ADD COLUMN imageUrl TEXT")
    }
}

private val migration2To3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE conversations ADD COLUMN otherUserId TEXT")
    }
}

private val migration3To4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE messages ADD COLUMN audioUrl TEXT")
    }
}

private val migration4To5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE messages ADD COLUMN replyToId TEXT")
        connection.execSQL("ALTER TABLE messages ADD COLUMN replyToContent TEXT")
        connection.execSQL("ALTER TABLE messages ADD COLUMN replyToSenderName TEXT")
    }
}

private val migration5To6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE messages ADD COLUMN callType TEXT")
        connection.execSQL("ALTER TABLE messages ADD COLUMN callStatus TEXT")
        connection.execSQL("ALTER TABLE messages ADD COLUMN callDuration INTEGER")
    }
}

private val migration6To7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE messages ADD COLUMN gifUrl TEXT")
        connection.execSQL("ALTER TABLE messages ADD COLUMN stickerUrl TEXT")
    }
}

private val migration7To8 = object : Migration(7, 8) {
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

private val migration8To9 = object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE conversations ADD COLUMN isMuted INTEGER NOT NULL DEFAULT 0")
    }
}

private val migration9To10 = object : Migration(9, 10) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE conversations ADD COLUMN historyVisibleFrom INTEGER NOT NULL DEFAULT 0")
    }
}

private val migration10To11 = object : Migration(10, 11) {
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

        seedStickerPacks(connection)
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
        .addMigrations(*allMigrations)
        .addCallback(stickerSeedCallback)
        .build()
}

private val migration33To34 = object : Migration(33, 34) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE users ADD COLUMN publicKey TEXT DEFAULT NULL")
    }
}

private val migration34To35 = object : Migration(34, 35) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE polls ADD COLUMN allowMultiple INTEGER NOT NULL DEFAULT 0")

        // poll_votes' primary key needs optionId added so a user can hold multiple votes in
        // the same poll (allowMultiple polls). Room requires a full table rebuild for PK
        // changes; existing rows are preserved via the copy below (no data loss).
        connection.execSQL(
            """CREATE TABLE poll_votes_new (
                pollId TEXT NOT NULL,
                userId TEXT NOT NULL,
                optionId TEXT NOT NULL,
                PRIMARY KEY(pollId, userId, optionId)
            )"""
        )
        connection.execSQL(
            "INSERT INTO poll_votes_new (pollId, userId, optionId) SELECT pollId, userId, optionId FROM poll_votes"
        )
        connection.execSQL("DROP TABLE poll_votes")
        connection.execSQL("ALTER TABLE poll_votes_new RENAME TO poll_votes")
    }
}

private val migration35To36 = object : Migration(35, 36) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE messages ADD COLUMN audioDurationMs INTEGER")
    }
}

private val migration36To37 = object : Migration(36, 37) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE user_status ADD COLUMN videoUrl TEXT DEFAULT NULL")
    }
}

private val migration37To38 = object : Migration(37, 38) {
    override fun migrate(connection: SQLiteConnection) {
        // Reply-to-status (WhatsApp-style story replies): a snapshot of the status carried on
        // the reply message itself, not a live reference — see StatusReplyContextBO's doc.
        connection.execSQL("ALTER TABLE messages ADD COLUMN replyToStatusId TEXT DEFAULT NULL")
        connection.execSQL("ALTER TABLE messages ADD COLUMN replyToStatusOwnerId TEXT DEFAULT NULL")
        connection.execSQL("ALTER TABLE messages ADD COLUMN replyToStatusText TEXT DEFAULT NULL")
        connection.execSQL("ALTER TABLE messages ADD COLUMN replyToStatusImageUrl TEXT DEFAULT NULL")
        connection.execSQL("ALTER TABLE messages ADD COLUMN replyToStatusVideoUrl TEXT DEFAULT NULL")
        connection.execSQL("ALTER TABLE messages ADD COLUMN replyToStatusBackgroundColor INTEGER DEFAULT NULL")
        connection.execSQL("ALTER TABLE messages ADD COLUMN replyToStatusExpiresAt INTEGER DEFAULT NULL")
    }
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

private val migration32To33 = object : Migration(32, 33) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE messages ADD COLUMN sendStatus TEXT NOT NULL DEFAULT 'sent'")
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

private val migration38To39 = object : Migration(38, 39) {
    override fun migrate(connection: SQLiteConnection) {
        // Self-healing migration: some dev/QA builds reached DATABASE_VERSION 38 while the
        // on-disk schema still lacked index_broadcast_list_members_listId (the version number
        // was reused mid-development before the app's first release, so migration28To29 — which
        // already creates this index — never ran against those particular installs). Re-issuing
        // the same idempotent CREATE INDEX IF NOT EXISTS here heals any device stuck in that
        // inconsistent v38-without-index state, without touching data.
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_broadcast_list_members_listId ON broadcast_list_members(listId)")
    }
}

private val migration39To40 = object : Migration(39, 40) {
    override fun migrate(connection: SQLiteConnection) {
        // Persist the recorded amplitude waveform so a received voice message can render its
        // sender's real waveform at rest, not just a synthetic per-URL one.
        connection.execSQL("ALTER TABLE messages ADD COLUMN audioAmplitudes TEXT DEFAULT NULL")
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

/**
 * Every registered migration, oldest to newest — the single source of truth `buildChatDatabase`
 * wires into `.addMigrations(...)`, and what `ChatDatabaseMigrationTest`
 * (`app/src/androidTest/.../data/local/`) replays end-to-end against real exported schemas.
 * `internal` rather than `private` so the instrumented test source set can see it. Declared last
 * in the file (not next to `buildChatDatabase`) because Kotlin initializes top-level properties
 * in textual declaration order within a file — referencing a migration val declared further
 * down than this one would fail to compile ("Variable must be initialized"), since several of
 * this file's migration vals aren't in ascending-version order (migration26To27 etc. are
 * declared after migration33To34..migration36To37).
 */
internal val allMigrations = arrayOf(
    migration1To2, migration2To3, migration3To4, migration4To5,
    migration5To6, migration6To7, migration7To8, migration8To9,
    migration9To10, migration10To11, migration11To12,
    migration12To13, migration13To14, migration14To15, migration15To16, migration16To17,
    migration17To18, migration18To19, migration19To20, migration20To21, migration21To22, migration22To23, migration23To24, migration24To25, migration25To26, migration26To27, migration27To28, migration28To29, migration29To30, migration30To31, migration31To32, migration32To33, migration33To34, migration34To35, migration35To36, migration36To37, migration37To38, migration38To39, migration39To40,
)
