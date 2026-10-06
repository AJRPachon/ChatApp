package com.ajrpachon.chatapp.data.local

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

private const val TWEMOJI_BASE = "https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72"

/**
 * The built-in sticker packs. Room calls this when it creates the database, so a fresh install has
 * them; `INSERT OR IGNORE` keeps it harmless if a pack already exists. Three packs start installed
 * and `pack_travel` starts as an available-but-not-installed pack in the store.
 */
internal fun seedStickerPacks(connection: SQLiteConnection) {
    val base = TWEMOJI_BASE
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
