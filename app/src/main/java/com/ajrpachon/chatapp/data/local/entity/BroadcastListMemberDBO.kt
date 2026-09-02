package com.ajrpachon.chatapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index

// The index on `listId` matches `CREATE INDEX index_broadcast_list_members_listId` in
// migration28To29 (DatabaseBuilder.kt) — the entity had drifted from what that migration
// actually creates, which ChatDatabaseMigrationTest caught (a real database that goes through
// the migration chain ends up with an index Room's compiled schema didn't know to expect,
// failing validation on open). Declaring it here keeps the entity's compiled schema in sync
// with what a genuinely-migrated database looks like.
@Entity(
    tableName = "broadcast_list_members",
    primaryKeys = ["listId", "userId"],
    indices = [Index("listId")],
)
data class BroadcastListMemberDBO(
    val listId: String,
    val userId: String,
)
