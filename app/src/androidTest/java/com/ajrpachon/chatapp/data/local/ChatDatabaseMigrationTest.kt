package com.ajrpachon.chatapp.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the app's real migration chain (`allMigrations` in DatabaseBuilder.kt — 36
 * migrations, v1 -> v37) end-to-end against the actual exported schemas in `app/schemas/`. This
 * is the check `chatapp-room-migration`'s own "Verifying" step never runs: that a database
 * created at an OLD version, holding real data, survives the full chain to today's version
 * without Room's own schema validation failing and without losing that data.
 *
 * Deliberately does NOT use SQLCipher (`SupportOpenHelperFactory`) — encryption is a separate,
 * already-covered concern (see the on-device check performed when sqlcipher-android was bumped
 * this session). The actual migration risk lives entirely in each `Migration`'s SQL, which is
 * identical whether the underlying store is encrypted or not.
 *
 * Requires `Instrumentation`, so this can only run as a `connectedAndroidTest` against a real
 * device/emulator, never as a local JVM unit test — every constructor overload of
 * `androidx.room.testing.MigrationTestHelper` in this Room version takes `Instrumentation`,
 * including the KMP/driver-based one used below.
 */
@RunWith(AndroidJUnit4::class)
class ChatDatabaseMigrationTest {

    private val testDbName = "chat-migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        file = InstrumentationRegistry.getInstrumentation().targetContext.getDatabasePath(testDbName),
        driver = BundledSQLiteDriver(),
        databaseClass = ChatDatabase::class,
        databaseFactory = {
            Room.databaseBuilder<ChatDatabase>(
                context = InstrumentationRegistry.getInstrumentation().targetContext,
                name = testDbName,
            ).setDriver(BundledSQLiteDriver()).build()
        },
        autoMigrationSpecs = emptyList(),
    )

    @Test
    fun migrate1To37_realDataSurvivesTheFullChain() {
        // Arrange: a v1 database with one real row in `messages` — the v1 schema (per
        // app/schemas/.../1.json) is just id, conversationId, senderId, content, isRead,
        // createdAt.
        val v1 = helper.createDatabase(1)
        v1.execSQL(
            "INSERT INTO messages (id, conversationId, senderId, content, isRead, createdAt) " +
                "VALUES ('msg-1', 'conv-1', 'user-1', 'hola desde v1', 0, 1000)"
        )
        v1.close()

        // Act: replay every registered migration, 1 -> 37 — validated against the real
        // app/schemas/.../37.json export. runMigrationsAndValidate fails loudly on any mismatch
        // between what the migrations actually produce and what Room's own schema for v37
        // expects (a missing column, a wrong type, an index that doesn't match, etc.).
        val migrated = helper.runMigrationsAndValidate(37, allMigrations.toList())

        // Assert: the v1 row is still there and unharmed after all 36 migrations.
        val statement = migrated.prepare("SELECT content FROM messages WHERE id = 'msg-1'")
        try {
            assertTrue("expected the v1 row to still exist after migrating to v37", statement.step())
            assertEquals("hola desde v1", statement.getText(0))
        } finally {
            statement.close()
        }
        migrated.close()
    }
}
