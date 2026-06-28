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

private val migration15To16 = object : Migration(15, 16) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE conversations ADD COLUMN mutedUntil INTEGER NOT NULL DEFAULT 0")
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
            migration17To18, migration18To19,
        )
        .build()
}
