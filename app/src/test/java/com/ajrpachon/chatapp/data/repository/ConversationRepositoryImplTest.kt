package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.local.dao.ConversationDao
import com.ajrpachon.chatapp.data.local.dao.GroupMemberDao
import com.ajrpachon.chatapp.data.local.dao.MessageDao
import com.ajrpachon.chatapp.data.local.dao.UserDao
import com.ajrpachon.chatapp.data.local.entity.ConversationDBO
import com.ajrpachon.chatapp.data.local.entity.UserDBO
import com.ajrpachon.chatapp.data.remote.dto.ConversationDTO
import com.ajrpachon.chatapp.data.remote.dto.ConversationParticipantWithConvDTO
import com.ajrpachon.chatapp.data.remote.dto.MessageDTO
import com.ajrpachon.chatapp.data.remote.dto.UserDTO
import com.ajrpachon.chatapp.data.remote.source.ConversationRemoteSource
import com.ajrpachon.chatapp.data.remote.source.MessageRemoteSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConversationRepositoryImplTest {

    private val conversationDao = mockk<ConversationDao>(relaxed = true)
    private val userDao = mockk<UserDao>(relaxed = true)
    private val messageDao = mockk<MessageDao>(relaxed = true)
    private val groupMemberDao = mockk<GroupMemberDao>(relaxed = true)
    private val messageRemote = mockk<MessageRemoteSource>(relaxed = true)
    private val remote = mockk<ConversationRemoteSource>(relaxed = true)

    private val repo = ConversationRepositoryImpl(conversationDao, userDao, messageDao, groupMemberDao, messageRemote, remote)

    private fun conversationDto(id: String, isGroup: Boolean, name: String? = null, createdBy: String? = null) =
        ConversationDTO(id = id, name = name, isGroup = isGroup, createdBy = createdBy, updatedAt = "2026-10-05T10:00:00Z")

    private fun participantRow(dto: ConversationDTO) =
        ConversationParticipantWithConvDTO(conversationId = dto.id, joinedAt = "2026-10-01T00:00:00Z", conversation = dto)

    private fun profile(id: String, username: String?, displayName: String) =
        UserDTO(id = id, username = username, displayName = displayName, createdAt = "2026-01-01T00:00:00Z")

    private fun userDbo(id: String, username: String, displayName: String) = UserDBO(
        id = id, email = "", username = username, displayName = displayName, avatarUrl = null, createdAt = 0L,
    )

    private fun capturedConversation(): ConversationDBO {
        val slot = slot<ConversationDBO>()
        coVerify { conversationDao.upsert(capture(slot)) }
        return slot.captured
    }

    // ── syncConversations ─────────────────────────────────────────────────────

    @Test
    fun `sync stores a group with the name it has on the server and does not look for another participant`() = runTest {
        coEvery { remote.fetchParticipantsWithConversations("me") } returns
            listOf(participantRow(conversationDto("g1", isGroup = true, name = "Familia", createdBy = "me")))

        repo.syncConversations("me")

        val stored = capturedConversation()
        assertEquals("g1", stored.id)
        assertEquals("Familia", stored.name)
        assertEquals(true, stored.isGroup)
        assertNull(stored.otherUserId)
        coVerify(exactly = 0) { remote.fetchOtherParticipantId(any(), any()) }
    }

    @Test
    fun `sync resolves a direct chat from the other participant and caches their profile`() = runTest {
        coEvery { remote.fetchParticipantsWithConversations("me") } returns
            listOf(participantRow(conversationDto("d1", isGroup = false, createdBy = "me")))
        coEvery { remote.fetchOtherParticipantId("d1", excludeUserId = "me") } returns "ana"
        coEvery { remote.fetchUserProfile("ana") } returns profile("ana", username = "ana_g", displayName = "Ana García")

        repo.syncConversations("me")

        val stored = capturedConversation()
        assertEquals("ana", stored.otherUserId)
        assertEquals("ana_g", stored.name)
        coVerify { userDao.upsert(match { it.id == "ana" && it.username == "ana_g" }) }
    }

    @Test
    fun `sync names a direct chat after the display name when the profile has no username`() = runTest {
        coEvery { remote.fetchParticipantsWithConversations("me") } returns
            listOf(participantRow(conversationDto("d1", isGroup = false, createdBy = "me")))
        coEvery { remote.fetchOtherParticipantId("d1", "me") } returns "ana"
        coEvery { remote.fetchUserProfile("ana") } returns profile("ana", username = null, displayName = "Ana García")

        repo.syncConversations("me")

        assertEquals("Ana García", capturedConversation().name)
    }

    @Test
    fun `sync falls back to the conversation creator when the participant lookup fails`() = runTest {
        coEvery { remote.fetchParticipantsWithConversations("me") } returns
            listOf(participantRow(conversationDto("d1", isGroup = false, createdBy = "ana")))
        coEvery { remote.fetchOtherParticipantId(any(), any()) } throws IllegalStateException("network")
        coEvery { remote.fetchUserProfile("ana") } returns profile("ana", "ana_g", "Ana")

        repo.syncConversations("me")

        assertEquals("ana", capturedConversation().otherUserId)
    }

    @Test
    fun `sync uses the locally cached name when the remote profile cannot be fetched`() = runTest {
        coEvery { remote.fetchParticipantsWithConversations("me") } returns
            listOf(participantRow(conversationDto("d1", isGroup = false, createdBy = "me")))
        coEvery { remote.fetchOtherParticipantId("d1", "me") } returns "ana"
        coEvery { remote.fetchUserProfile("ana") } throws IllegalStateException("offline")
        coEvery { userDao.getById("ana") } returns userDbo("ana", username = "ana_cached", displayName = "Ana")

        repo.syncConversations("me")

        assertEquals("ana_cached", capturedConversation().name)
    }

    @Test
    fun `sync stores the last message of each conversation for the list preview`() = runTest {
        val dto = conversationDto("g1", isGroup = true, name = "Familia")
        coEvery { remote.fetchParticipantsWithConversations("me") } returns listOf(participantRow(dto))
        val lastMessage = mockk<MessageDTO>(relaxed = true) { every { id } returns "m9" }
        coEvery { messageRemote.getLastMessage("g1", any()) } returns lastMessage

        repo.syncConversations("me")

        coVerify { messageDao.upsert(match { it.id == "m9" }) }
    }

    // ── getOrCreateDirectConversation ─────────────────────────────────────────

    @Test
    fun `getOrCreateDirectConversation prefers the cached username and skips the profile fetch`() = runTest {
        coEvery { remote.getOrCreateDirectConversation("me", "ana") } returns "d1"
        coEvery { userDao.getById("ana") } returns userDbo("ana", username = "ana_g", displayName = "Ana García")

        val conversation = repo.getOrCreateDirectConversation("me", "ana")

        assertEquals("d1", conversation.id)
        assertEquals("ana_g", conversation.name)
        coVerify(exactly = 0) { remote.fetchUserProfile(any()) }
        assertEquals("ana", capturedConversation().otherUserId)
    }

    @Test
    fun `getOrCreateDirectConversation fetches and caches the profile when it is not cached`() = runTest {
        coEvery { remote.getOrCreateDirectConversation("me", "ana") } returns "d1"
        coEvery { userDao.getById("ana") } returns null
        coEvery { remote.fetchUserProfile("ana") } returns profile("ana", username = "ana_g", displayName = "Ana")

        val conversation = repo.getOrCreateDirectConversation("me", "ana")

        assertEquals("ana_g", conversation.name)
        coVerify { userDao.upsert(match { it.id == "ana" }) }
    }

    @Test
    fun `getOrCreateDirectConversation is called Chat when no name can be found`() = runTest {
        coEvery { remote.getOrCreateDirectConversation("me", "ana") } returns "d1"
        coEvery { userDao.getById("ana") } returns null
        coEvery { remote.fetchUserProfile("ana") } throws IllegalStateException("offline")

        val conversation = repo.getOrCreateDirectConversation("me", "ana")

        assertEquals("Chat", conversation.name)
        assertNull(capturedConversation().name)
    }

    // ── current user ──────────────────────────────────────────────────────────

    @Test
    fun `getById is null when nobody is signed in`() = runTest {
        coEvery { conversationDao.getById("c1") } returns ConversationDBO("c1", "x", false, "u", 0L)
        every { remote.currentUserId() } returns null

        assertNull(repo.getById("c1"))
    }

    // ── realtime handlers ─────────────────────────────────────────────────────

    private fun dbConversation(unread: Int) = ConversationDBO(
        id = "c1", name = "Chat", isGroup = false, createdBy = "me", updatedAt = 0L, unreadCount = unread,
    )

    private fun messageDto(sender: String, conversationId: String = "c1") =
        MessageDTO(id = "m1", conversationId = conversationId, senderId = sender, content = "hola", isRead = false, createdAt = "2026-10-05T10:00:00Z")

    @Test
    fun `a new message from another user increments the unread counter`() = runTest {
        every { remote.observeParticipantInserts(any()) } returns emptyFlow()
        every { remote.observeConversationUpdates(any()) } returns emptyFlow()
        every { remote.observeProfileUpdates(any()) } returns emptyFlow()
        every { remote.observeNewMessageInserts("me") } returns flowOf(messageDto(sender = "ana"))
        every { conversationDao.observeActive() } returns flowOf(emptyList())
        coEvery { conversationDao.getById("c1") } returns dbConversation(unread = 2)

        val job = launch { repo.observeConversations("me").collect { } }
        runCurrent()
        job.cancel()

        coVerify { conversationDao.upsert(match { it.id == "c1" && it.unreadCount == 3 }) }
    }

    @Test
    fun `a message the user sent does not change the unread counter`() = runTest {
        every { remote.observeParticipantInserts(any()) } returns emptyFlow()
        every { remote.observeConversationUpdates(any()) } returns emptyFlow()
        every { remote.observeProfileUpdates(any()) } returns emptyFlow()
        every { remote.observeNewMessageInserts("me") } returns flowOf(messageDto(sender = "me"))
        every { conversationDao.observeActive() } returns flowOf(emptyList())
        coEvery { conversationDao.getById("c1") } returns dbConversation(unread = 2)

        val job = launch { repo.observeConversations("me").collect { } }
        runCurrent()
        job.cancel()

        coVerify { conversationDao.upsert(match { it.id == "c1" && it.unreadCount == 2 }) }
    }

    @Test
    fun `a profile update renames the direct chat with that user`() = runTest {
        every { remote.observeParticipantInserts(any()) } returns emptyFlow()
        every { remote.observeConversationUpdates(any()) } returns emptyFlow()
        every { remote.observeNewMessageInserts(any()) } returns emptyFlow()
        every { remote.observeProfileUpdates("me") } returns flowOf(
            buildJsonObject {
                put("id", JsonPrimitive("ana"))
                put("username", JsonPrimitive("ana_new"))
                put("display_name", JsonPrimitive("Ana N"))
            },
        )
        every { conversationDao.observeActive() } returns flowOf(emptyList())
        coEvery { userDao.getById("ana") } returns userDbo("ana", "ana_old", "Ana")
        coEvery { conversationDao.getByOtherUserId("ana") } returns
            ConversationDBO(id = "d1", name = "ana_old", isGroup = false, createdBy = "me", updatedAt = 0L, otherUserId = "ana")

        val job = launch { repo.observeConversations("me").collect { } }
        runCurrent()
        job.cancel()

        coVerify { userDao.upsert(match { it.id == "ana" && it.username == "ana_new" && it.displayName == "Ana N" }) }
        coVerify { conversationDao.upsert(match { it.id == "d1" && it.name == "ana_new" }) }
    }
}
