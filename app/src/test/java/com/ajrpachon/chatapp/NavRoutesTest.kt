package com.ajrpachon.chatapp

import androidx.navigation3.runtime.NavKey
import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavRoutesTest {

    private val routes: List<NavKey> = listOf(
        AuthRoute,
        ConversationListRoute,
        ChatRoute("c1", "Ana", isGroup = true, highlightMessageId = "m1"),
        InvitationsRoute,
        NewChatRoute,
        ProfileRoute,
        CallRoute("call", "c1", "room", "video", "Ana", isOutgoing = true),
        CreateGroupRoute,
        UserInfoRoute("u1"),
        GroupInfoRoute("c1", "Team", groupAvatarUrl = null, groupDescription = "desc"),
        BroadcastListRoute,
        UsageStatsRoute,
        SessionAuditRoute,
        AppLockRoute,
        BackupRoute,
        PdfViewerRoute("https://x/y.pdf", "y.pdf"),
        GlobalSearchRoute,
        ChatMediaGalleryRoute("c1", "Team"),
        StatusViewerRoute("u1", initialStatusId = "s1"),
    )

    private val navRoutesSource: String by lazy {
        listOf("src/main/java", "app/src/main/java")
            .map { File(it, "com/ajrpachon/chatapp/NavRoutes.kt") }
            .first { it.exists() }
            .readText()
    }

    @Test
    fun `every route survives a serialization round trip`() {
        routes.forEach { route ->
            @Suppress("UNCHECKED_CAST")
            val serializer = kotlinx.serialization.serializer(route::class.java) as kotlinx.serialization.KSerializer<NavKey>
            val json = Json.encodeToString(serializer, route)
            assertEquals(route, Json.decodeFromString(serializer, json))
        }
    }

    @Test
    fun `the list above covers every route declared in NavRoutes`() {
        val declared = Regex("""@Serializable data (?:object|class) (\w+Route)""").findAll(navRoutesSource)
            .map { it.groupValues[1] }.toSet()
        assertEquals(declared, routes.map { it::class.simpleName }.toSet())
    }

    @Test
    fun `every declared route has an entry in one of the feature providers`() {
        val declared = Regex("""@Serializable data (?:object|class) (\w+Route)""").findAll(navRoutesSource)
            .map { it.groupValues[1] }.toSet()
        val handled = Regex("""(?:is )?(\w+Route)\s*->""").findAll(navRoutesSource).map { it.groupValues[1] }.toSet()
        assertTrue("Routes without an entry: ${declared - handled}", handled.containsAll(declared))
    }
}
