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
 * Exercises the app's real migration chain (`allMigrations` in DatabaseBuilder.kt, v1 -> v40)
 * end-to-end against the actual exported schemas in `app/schemas/`. This is the check
 * `chatapp-room-migration`'s own "Verifying" step never runs: that a database created at an OLD
 * version, holding real data, survives the full chain to today's version without Room's own
 * schema validation failing and without losing that data.
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

    private companion object {
        /** From this version on, every DB version has an exported schema in app/schemas/. */
        const val FIRST_FULLY_EXPORTED_VERSION = 32

        /**
         * Steps whose result legitimately differs from the next exported schema, and the only thing
         * in which they differ: `index_broadcast_list_members_listId`. The migrations create it
         * from v29 (migration28To29), but the schemas exported for v32-v36 were generated before
         * the entity declared that `@Index` (it appears in 37.json), so 26 -> 32 produces an index
         * 32.json does not list, and 36 -> 37 does not produce the one 37.json lists. That is the
         * same version-number reuse that `migrate38To39_selfHealsMissingBroadcastListMembersIndex`
         * repairs on real installs. The end state is checked by the 1 -> 40 test below.
         */
        val KNOWN_INDEX_MISMATCH_STEPS = setOf(26 to 32, 36 to 37)
        const val KNOWN_INDEX_MISMATCH_TABLE = "broadcast_list_members"
    }

    // Each @Test needs its own on-disk file name: MigrationTestHelper's teardown does not
    // reliably wipe the underlying SQLite file between test methods that share one instrumented
    // process, so two tests pointed at the same file name can leak state across each other
    // (e.g. the second test's createDatabase(1) failing because the file was already left at a
    // later version by a previous test).
    private fun helperFor(dbName: String) = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        file = InstrumentationRegistry.getInstrumentation().targetContext.getDatabasePath(dbName),
        driver = BundledSQLiteDriver(),
        databaseClass = ChatDatabase::class,
        databaseFactory = {
            Room.databaseBuilder<ChatDatabase>(
                context = InstrumentationRegistry.getInstrumentation().targetContext,
                name = dbName,
            ).setDriver(BundledSQLiteDriver()).build()
        },
        autoMigrationSpecs = emptyList(),
    )

    @get:Rule
    val helper = helperFor("chat-migration-test.db")

    @get:Rule
    val selfHealHelper = helperFor("chat-migration-test-38-39.db")

    @get:Rule
    val audioAmplitudesHelper = helperFor("chat-migration-test-39-40.db")

    @Test
    fun migrateBetweenEveryPairOfConsecutiveExportedSchemas() {
        // The 1 -> 40 test below only says the chain as a whole works. This one replays it in the
        // smallest steps the exported schemas allow (from each exported version to the next one),
        // validating each step against that version's own schema, so a failure names the exact
        // migration that broke instead of just "the chain".
        //
        // Not every version has a schema: 13, 14, 22-25 and 27-31 are missing. They cannot be
        // regenerated from git: the feature branches were merged in two batches (e5d9e30 and
        // d10e732, jumping 21 -> 26 -> 32) and the version numbers were reused across branches, so
        // the entities at the commits that say "v13" or "v14" are not the ones the final chain
        // calls v13 and v14 (isDeleted arrives at a different version). The migrations across a
        // gap (12 -> 15, 21 -> 26, 26 -> 32) are still replayed together and validated against the
        // exported schema at the far end.
        val versions = exportedSchemaVersions()
        assertEquals("the first exported schema", 1, versions.first())
        assertEquals("the newest exported schema must be the current DB version", 40, versions.last())
        val failures = versions.zipWithNext().mapNotNull { (from, to) ->
            val dbName = "chat-migration-step-$from-$to.db"
            try {
                val old = helperFor(dbName).createDatabase(from)
                old.close()
                helperFor(dbName).runMigrationsAndValidate(to, allMigrations.toList()).close()
                null
            } catch (e: Throwable) {
                val isKnownIndexMismatch = (from to to) in KNOWN_INDEX_MISMATCH_STEPS &&
                    e.message.orEmpty().contains(KNOWN_INDEX_MISMATCH_TABLE)
                if (isKnownIndexMismatch) null else "$from -> $to: ${e.message?.lineSequence()?.firstOrNull { it.isNotBlank() }}"
            } finally {
                InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(dbName)
            }
        }
        // Report every broken step at once, not just the first.
        val report = failures.joinToString(separator = "\n", prefix = "migration steps that do not produce the next exported schema:\n")
        assertTrue(report, failures.isEmpty())
    }

    @Test
    fun everyVersionFromFirstFullyExportedToCurrentHasASchema() {
        // From v32 on every bump has shipped with its schema. Guards that going forward: a new
        // Migration(X, Y) without the schema export for Y leaves a gap here and fails the test.
        val versions = exportedSchemaVersions().toSet()
        val missing = (FIRST_FULLY_EXPORTED_VERSION..versions.max()).filterNot { it in versions }
        assertTrue("missing exported schemas for versions $missing", missing.isEmpty())
    }

    private fun exportedSchemaVersions(): List<Int> =
        InstrumentationRegistry.getInstrumentation().context.assets
            .list(ChatDatabase::class.qualifiedName!!)!!
            .mapNotNull { it.removeSuffix(".json").toIntOrNull() }
            .sorted()

    @Test
    fun migrate1To40_realDataSurvivesTheFullChain() {
        // Arrange: a v1 database with one real row in `messages` — the v1 schema (per
        // app/schemas/.../1.json) is just id, conversationId, senderId, content, isRead,
        // createdAt.
        val v1 = helper.createDatabase(1)
        v1.execSQL(
            "INSERT INTO messages (id, conversationId, senderId, content, isRead, createdAt) " +
                "VALUES ('msg-1', 'conv-1', 'user-1', 'hola desde v1', 0, 1000)"
        )
        v1.close()

        // Act: replay every registered migration, 1 -> 40 — validated against the real
        // app/schemas/.../40.json export. runMigrationsAndValidate fails loudly on any mismatch
        // between what the migrations actually produce and what Room's own schema for v40
        // expects (a missing column, a wrong type, an index that doesn't match, etc.).
        val migrated = helper.runMigrationsAndValidate(40, allMigrations.toList())

        // Assert: the v1 row is still there and unharmed after all migrations.
        val statement = migrated.prepare("SELECT content FROM messages WHERE id = 'msg-1'")
        try {
            assertTrue("expected the v1 row to still exist after migrating to v40", statement.step())
            assertEquals("hola desde v1", statement.getText(0))
        } finally {
            statement.close()
        }
        migrated.close()
    }

    @Test
    fun migrate38To39_selfHealsMissingBroadcastListMembersIndex() {
        // Arrange: reproduce the real-world regression seen on a physical device — a DB that
        // reached version 38 without index_broadcast_list_members_listId, because the version
        // number was reused mid-development (pre-release) before migration28To29 — which
        // creates that index — actually ran against that particular install. Build a real v38
        // database via `createDatabase`, then explicitly drop the index to simulate that
        // inconsistent state.
        val v38 = selfHealHelper.createDatabase(38)
        v38.execSQL("DROP INDEX IF EXISTS index_broadcast_list_members_listId")
        v38.close()

        // Act: migration38To39 re-issues CREATE INDEX IF NOT EXISTS for that same index. Passing
        // the full registered list is fine and matches how `buildChatDatabase` wires
        // migrations in production — MigrationTestHelper only applies the ones needed to go
        // from the DB's current version (38) to the target (39), i.e. just migration38To39.
        val migrated = selfHealHelper.runMigrationsAndValidate(39, allMigrations.toList())

        // Assert: the index exists again after the self-healing migration.
        val statement = migrated.prepare(
            "SELECT name FROM sqlite_master WHERE type = 'index' " +
                "AND name = 'index_broadcast_list_members_listId'"
        )
        try {
            assertTrue(
                "expected index_broadcast_list_members_listId to be recreated by migration38To39",
                statement.step(),
            )
        } finally {
            statement.close()
        }
        migrated.close()
    }

    @Test
    fun migrate39To40_addsAudioAmplitudesColumnToMessages() {
        // Arrange: a real v39 database (already includes the self-healed broadcast list members
        // index from migration38To39) with one message row inserted before the amplitudes column
        // existed.
        val v39 = audioAmplitudesHelper.createDatabase(39)
        v39.execSQL(
            "INSERT INTO messages (id, conversationId, senderId, content, isRead, createdAt, " +
                "isEncrypted, isDeleted, isEdited, isPinned, isSaved, sendStatus) " +
                "VALUES ('msg-39', 'conv-1', 'user-1', 'hola desde v39', 0, 2000, " +
                "0, 0, 0, 0, 0, 'sent')"
        )
        v39.close()

        // Act: migration39To40 adds the nullable audioAmplitudes TEXT column.
        val migrated = audioAmplitudesHelper.runMigrationsAndValidate(40, allMigrations.toList())

        // Assert: the pre-existing row survives with audioAmplitudes defaulting to NULL.
        val statement = migrated.prepare(
            "SELECT audioAmplitudes FROM messages WHERE id = 'msg-39'"
        )
        try {
            assertTrue("expected the v39 row to still exist after migrating to v40", statement.step())
            assertTrue("expected audioAmplitudes to be NULL by default", statement.isNull(0))
        } finally {
            statement.close()
        }
        migrated.close()
    }
}
