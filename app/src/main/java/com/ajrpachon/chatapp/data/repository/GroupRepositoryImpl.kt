package com.ajrpachon.chatapp.data.repository
import com.ajrpachon.chatapp.utils.catchResult
import com.ajrpachon.chatapp.utils.AppLogger

private const val TAG = "GroupRepo"

import com.ajrpachon.chatapp.data.local.dao.ConversationDao
import com.ajrpachon.chatapp.data.local.dao.GroupMemberDao
import com.ajrpachon.chatapp.data.local.dao.MessageDao
import com.ajrpachon.chatapp.data.local.entity.ConversationDBO
import com.ajrpachon.chatapp.data.local.entity.GroupMemberDBO
import com.ajrpachon.chatapp.data.remote.dto.GroupMemberDTO
import com.ajrpachon.chatapp.data.remote.source.GroupRemoteSource
import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.model.GroupMemberBO
import com.ajrpachon.chatapp.domain.model.GroupRole
import com.ajrpachon.chatapp.domain.repository.GroupRepository
import com.ajrpachon.chatapp.utils.UploadLimits.checkAvatarSize
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

private const val GROUP_AVATAR_BUCKET = "group-avatars"

class GroupRepositoryImpl(
    private val groupMemberDao: GroupMemberDao,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val remoteSource: GroupRemoteSource,
    private val supabase: SupabaseClient,
) : GroupRepository {

    override suspend fun createGroup(
        name: String,
        description: String?,
        createdBy: String,
        participantIds: List<String>,
    ): ConversationBO {
        val convId = java.util.UUID.randomUUID().toString()
        remoteSource.createGroup(convId, name, description, createdBy, participantIds)
        val now = System.currentTimeMillis()
        conversationDao.upsert(
            ConversationDBO(
                id = convId,
                name = name,
                isGroup = true,
                createdBy = createdBy,
                updatedAt = now,
                description = description,
            )
        )
        return ConversationBO(
            id = convId,
            name = name,
            isGroup = true,
            participants = emptyList(),
            lastMessage = null,
            unreadCount = 0,
            updatedAt = Instant.fromEpochMilliseconds(now),
            description = description,
        )
    }

    override fun observeMembers(conversationId: String): Flow<List<GroupMemberBO>> =
        groupMemberDao.observeByConversation(conversationId)
            .map { list -> list.map { it.toBO() } }

    // Called by ViewModels in their own viewModelScope so it cancels reliably on onCleared.
    override suspend fun syncMembership(conversationId: String) {
        val isMember = remoteSource.isCurrentUserMember(conversationId)
        AppLogger.d(TAG, "syncMembership conv=$conversationId isMember=$isMember")
        when (isMember) {
            null -> {} // network error — leave Room untouched
            false -> {
                AppLogger.d(TAG, "syncMembership: expelled — clearing Room")
                groupMemberDao.deleteAllForConversation(conversationId)
                messageDao.deleteByConversation(conversationId)
            }
            true -> syncFullMemberList(conversationId)
        }
    }

    // Fetches full member list from remote, updates historyVisibleFrom for current user if needed,
    // applies diff to Room, and ensures at least one admin exists.
    private suspend fun syncFullMemberList(conversationId: String) {
        val remote = remoteSource.getMembers(conversationId) ?: return
        if (remote.isEmpty()) return
        val currentUserId = supabase.auth.currentUserOrNull()?.id ?: return
        val selfMember = remote.firstOrNull { it.userId == currentUserId } ?: return

        val localMembers = groupMemberDao.getAllForConversation(conversationId)
        val wasExpelled = localMembers.none { it.userId == currentUserId }
        AppLogger.d(TAG, "syncFull conv=$conversationId currentUser=$currentUserId wasExpelled=$wasExpelled selfJoinedAt=${selfMember.joinedAt} localIds=${localMembers.map{it.userId}}")

        if (wasExpelled) {
            val newHistoryFrom = catchResult {
                Instant.parse(selfMember.joinedAt).toEpochMilliseconds()
            }.getOrDefault(0L)
            val existing = conversationDao.getById(conversationId)
            AppLogger.d(TAG, "syncFull EXPELLED — newHistoryFrom=$newHistoryFrom existing.historyVisibleFrom=${existing?.historyVisibleFrom}")
            if (existing != null && newHistoryFrom != existing.historyVisibleFrom) {
                conversationDao.upsert(existing.copy(historyVisibleFrom = newHistoryFrom))
                if (newHistoryFrom > 0L) messageDao.deleteMessagesBefore(conversationId, newHistoryFrom)
                AppLogger.d(TAG, "syncFull EXPELLED — wrote historyVisibleFrom=$newHistoryFrom, deleted messages before $newHistoryFrom")
            }
        }

        applyMemberDiff(conversationId, remote)
    }

    // Diffs remote list against Room: removes departed members, upserts all remote.
    // Skips whenever the current user is absent from the remote list — covers both a timing race
    // (Supabase propagation lag after re-add) AND a null auth state (token refresh window).
    // In either case the polling loop will re-run and apply the correct state within 3s.
    private suspend fun applyMemberDiff(conversationId: String, dtos: List<GroupMemberDTO>) {
        if (dtos.isEmpty()) return
        val currentUserId = supabase.auth.currentUserOrNull()?.id
        if (currentUserId == null || dtos.none { it.userId == currentUserId }) {
            AppLogger.w(TAG, "applyMemberDiff SKIPPED conv=$conversationId currentUserId=$currentUserId dtos=${dtos.map { it.userId }}")
            return
        }
        val remoteIds = dtos.map { it.userId }.toSet()
        val localIds = groupMemberDao.getAllForConversation(conversationId).map { it.userId }.toSet()
        val toRemove = localIds - remoteIds
        AppLogger.d(TAG, "applyMemberDiff conv=$conversationId local=$localIds remote=$remoteIds removing=$toRemove")
        for (removed in toRemove) groupMemberDao.delete(conversationId, removed)
        groupMemberDao.upsertAll(dtos.map { it.toDBO() })
    }

    override suspend fun getMembers(conversationId: String): List<GroupMemberBO> {
        val remote = remoteSource.getMembers(conversationId) ?: return emptyList()
        if (remote.isNotEmpty()) groupMemberDao.upsertAll(remote.map { it.toDBO() })
        return remote.map { dto ->
            GroupMemberBO(
                userId = dto.userId,
                conversationId = dto.conversationId,
                displayName = dto.profile?.displayName ?: dto.userId,
                username = dto.profile?.username ?: "",
                avatarUrl = dto.profile?.avatarUrl,
                role = if (dto.role == "admin") GroupRole.ADMIN else GroupRole.MEMBER,
                joinedAt = catchResult { Instant.parse(dto.joinedAt) }
                    .getOrDefault(Instant.fromEpochMilliseconds(0)),
            )
        }
    }

    override suspend fun addMember(conversationId: String, userId: String, canSeeHistory: Boolean) {
        remoteSource.addMember(conversationId, userId, canSeeHistory)
        val updated = remoteSource.getMembers(conversationId) ?: return
        if (updated.isNotEmpty()) groupMemberDao.upsertAll(updated.map { it.toDBO() })
    }

    override suspend fun removeMember(conversationId: String, userId: String) {
        remoteSource.removeMember(conversationId, userId)
        groupMemberDao.delete(conversationId, userId)
        ensureAdminExists(conversationId)
    }

    override suspend fun updateGroup(
        conversationId: String,
        name: String?,
        description: String?,
        avatarUrl: String?,
    ) {
        remoteSource.updateGroup(conversationId, name, description, avatarUrl)
        val existing = conversationDao.getById(conversationId) ?: return
        conversationDao.upsert(
            existing.copy(
                name = name ?: existing.name,
                description = description ?: existing.description,
                groupAvatarUrl = avatarUrl ?: existing.groupAvatarUrl,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun leaveGroup(conversationId: String, userId: String) {
        val members = groupMemberDao.getAllForConversation(conversationId)
        val admins = members.filter { it.role == "admin" }
        val isLastAdmin = admins.size == 1 && admins[0].userId == userId
        if (isLastAdmin) {
            val nextAdmin = members.firstOrNull { it.userId != userId }
            if (nextAdmin != null) {
                remoteSource.updateMemberRole(conversationId, nextAdmin.userId, "admin")
                groupMemberDao.upsert(nextAdmin.copy(role = "admin"))
            }
        }
        remoteSource.removeMember(conversationId, userId)
        groupMemberDao.delete(conversationId, userId)
    }

    private suspend fun ensureAdminExists(conversationId: String) {
        val remaining = remoteSource.getMembers(conversationId) ?: return
        if (remaining.isNotEmpty() && remaining.none { it.role == "admin" }) {
            catchResult { remoteSource.updateMemberRole(conversationId, remaining.first().userId, "admin") }
            val updated = remoteSource.getMembers(conversationId) ?: return
            if (updated.isNotEmpty()) groupMemberDao.upsertAll(updated.map { it.toDBO() })
        }
    }

    override suspend fun promoteMember(conversationId: String, userId: String) {
        remoteSource.updateMemberRole(conversationId, userId, "admin")
        val existing = groupMemberDao.getByUser(conversationId, userId)
        if (existing != null) groupMemberDao.upsert(existing.copy(role = "admin"))
    }

    override suspend fun demoteMember(conversationId: String, userId: String) {
        remoteSource.updateMemberRole(conversationId, userId, "member")
        val existing = groupMemberDao.getByUser(conversationId, userId)
        if (existing != null) groupMemberDao.upsert(existing.copy(role = "member"))
    }

    override suspend fun uploadGroupAvatar(conversationId: String, bytes: ByteArray): String {
        bytes.checkAvatarSize()
        val path = "$conversationId/${java.util.UUID.randomUUID()}.jpg"
        supabase.storage[GROUP_AVATAR_BUCKET].upload(path, bytes) { upsert = true }
        return supabase.storage[GROUP_AVATAR_BUCKET].publicUrl(path)
    }
}

// ── Mappers ───────────────────────────────────────────────────────────────────

private fun GroupMemberDTO.toDBO() = GroupMemberDBO(
    conversationId = conversationId,
    userId = userId,
    displayName = profile?.displayName ?: userId,
    username = profile?.username ?: "",
    avatarUrl = profile?.avatarUrl,
    role = role,
    joinedAt = catchResult { Instant.parse(joinedAt).toEpochMilliseconds() }
        .getOrDefault(System.currentTimeMillis()),
)

private fun GroupMemberDBO.toBO() = GroupMemberBO(
    userId = userId,
    conversationId = conversationId,
    displayName = displayName,
    username = username,
    avatarUrl = avatarUrl,
    role = if (role == "admin") GroupRole.ADMIN else GroupRole.MEMBER,
    joinedAt = Instant.fromEpochMilliseconds(joinedAt),
)
