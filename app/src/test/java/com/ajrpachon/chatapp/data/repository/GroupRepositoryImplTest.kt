package com.ajrpachon.chatapp.data.repository

import app.cash.turbine.test
import com.ajrpachon.chatapp.data.local.dao.ConversationDao
import com.ajrpachon.chatapp.data.local.dao.GroupMemberDao
import com.ajrpachon.chatapp.data.local.dao.MessageDao
import com.ajrpachon.chatapp.data.local.entity.GroupMemberDBO
import com.ajrpachon.chatapp.data.remote.dto.GroupMemberDTO
import com.ajrpachon.chatapp.data.remote.source.GroupRemoteSource
import com.ajrpachon.chatapp.util.MainDispatcherRule
import io.github.jan.supabase.SupabaseClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Ignore
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class GroupRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val groupMemberDao = mockk<GroupMemberDao>(relaxed = true)
    private val conversationDao = mockk<ConversationDao>(relaxed = true)
    private val messageDao = mockk<MessageDao>(relaxed = true)
    private val remoteSource = mockk<GroupRemoteSource>(relaxed = true)
    private val supabase = mockk<SupabaseClient>(relaxed = true)

    private val repo = GroupRepositoryImpl(groupMemberDao, conversationDao, messageDao, remoteSource, supabase)

    // ── observeMembers — just maps Room ──────────────────────────────────────

    @Test
    fun `observeMembers emits mapped BOs from Room`() = runTest {
        every { groupMemberDao.observeByConversation(any()) } returns flowOf(
            listOf(dbo("user1"), dbo("user2"))
        )

        repo.observeMembers("conv1").test {
            val members = awaitItem()
            assert(members.size == 2)
            assert(members.any { it.userId == "user1" })
            assert(members.any { it.userId == "user2" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── syncMembership ────────────────────────────────────────────────────────

    @Test
    fun `syncMembership clears Room when user is not a member`() = runTest {
        coEvery { remoteSource.isCurrentUserMember("conv1") } returns false

        repo.syncMembership("conv1")

        coVerify { groupMemberDao.deleteAllForConversation("conv1") }
        coVerify { messageDao.deleteByConversation("conv1") }
    }

    @Test
    fun `syncMembership does nothing when isCurrentUserMember returns null (network error)`() = runTest {
        coEvery { remoteSource.isCurrentUserMember("conv1") } returns null

        repo.syncMembership("conv1")

        coVerify(exactly = 0) { groupMemberDao.deleteAllForConversation(any()) }
        coVerify(exactly = 0) { groupMemberDao.upsertAll(any()) }
    }

    @Ignore("Supabase 3.x plugin system throws when accessing .auth on a mock — requires integration test")
    @Test
    fun `syncMembership delegates to full member sync when user is a member`() = runTest {
        coEvery { remoteSource.isCurrentUserMember("conv1") } returns true
        coEvery { remoteSource.getMembers("conv1") } returns listOf(dto("user1"), dto("user2"))
        coEvery { groupMemberDao.getAllForConversation("conv1") } returns listOf(dbo("user1"))

        repo.syncMembership("conv1")

        // Verify the remote fetch was triggered — Supabase auth internals are not unit-testable here
        coVerify { remoteSource.getMembers("conv1") }
    }

    // ── addMember ─────────────────────────────────────────────────────────────

    @Test
    fun `addMember calls remote and upserts refreshed list to Room`() = runTest {
        coEvery { remoteSource.getMembers(any()) } returns listOf(dto("user1"), dto("user2"))

        repo.addMember("conv1", "user2", canSeeHistory = false)

        coVerify { remoteSource.addMember("conv1", "user2", false) }
        coVerify { groupMemberDao.upsertAll(any()) }
    }

    @Test
    fun `addMember skips Room upsert when remote refresh returns null`() = runTest {
        coEvery { remoteSource.getMembers(any()) } returns null

        repo.addMember("conv1", "user2", canSeeHistory = true)

        coVerify { remoteSource.addMember("conv1", "user2", true) }
        coVerify(exactly = 0) { groupMemberDao.upsertAll(any()) }
    }

    // ── removeMember ──────────────────────────────────────────────────────────

    @Test
    fun `removeMember calls remote and deletes from Room`() = runTest {
        coEvery { remoteSource.getMembers(any()) } returns emptyList()

        repo.removeMember("conv1", "user1")

        coVerify { remoteSource.removeMember("conv1", "user1") }
        coVerify { groupMemberDao.delete("conv1", "user1") }
    }

    // ── leaveGroup ────────────────────────────────────────────────────────────

    @Test
    fun `leaveGroup promotes next member when current user is sole admin`() = runTest {
        val adminDBO = dbo("admin-user", role = "admin")
        val memberDBO = dbo("member-user", role = "member")
        coEvery { groupMemberDao.getAllForConversation("conv1") } returns listOf(adminDBO, memberDBO)

        repo.leaveGroup("conv1", "admin-user")

        coVerify { remoteSource.updateMemberRole("conv1", "member-user", "admin") }
        coVerify { groupMemberDao.upsert(memberDBO.copy(role = "admin")) }
    }

    @Test
    fun `leaveGroup does not promote when there are multiple admins`() = runTest {
        val admin1 = dbo("admin1", role = "admin")
        val admin2 = dbo("admin2", role = "admin")
        coEvery { groupMemberDao.getAllForConversation("conv1") } returns listOf(admin1, admin2)

        repo.leaveGroup("conv1", "admin1")

        coVerify(exactly = 0) { remoteSource.updateMemberRole(any(), any(), any()) }
    }

    @Test
    fun `leaveGroup removes user from remote and local`() = runTest {
        val memberDBO = dbo("user1", role = "member")
        coEvery { groupMemberDao.getAllForConversation("conv1") } returns listOf(memberDBO)

        repo.leaveGroup("conv1", "user1")

        coVerify { remoteSource.removeMember("conv1", "user1") }
        coVerify { groupMemberDao.delete("conv1", "user1") }
    }

    @Test
    fun `leaveGroup does not promote when sole member is also the one leaving`() = runTest {
        val adminDBO = dbo("admin-user", role = "admin")
        coEvery { groupMemberDao.getAllForConversation("conv1") } returns listOf(adminDBO)

        repo.leaveGroup("conv1", "admin-user")

        // No other member to promote
        coVerify(exactly = 0) { remoteSource.updateMemberRole(any(), any(), any()) }
        coVerify { remoteSource.removeMember("conv1", "admin-user") }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun dto(userId: String, role: String = "member") = GroupMemberDTO(
        conversationId = "conv1",
        userId = userId,
        role = role,
        joinedAt = "1970-01-01T00:00:00Z",
    )

    private fun dbo(userId: String, role: String = "member") = GroupMemberDBO(
        conversationId = "conv1",
        userId = userId,
        displayName = userId,
        username = userId,
        avatarUrl = null,
        role = role,
        joinedAt = 0L,
    )
}
