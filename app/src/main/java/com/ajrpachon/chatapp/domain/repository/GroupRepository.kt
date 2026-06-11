package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.model.GroupMemberBO
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    suspend fun createGroup(
        name: String,
        description: String?,
        createdBy: String,
        participantIds: List<String>,
    ): ConversationBO

    fun observeMembers(conversationId: String): Flow<List<GroupMemberBO>>
    suspend fun syncMembership(conversationId: String)
    suspend fun getMembers(conversationId: String): List<GroupMemberBO>
    suspend fun addMember(conversationId: String, userId: String, canSeeHistory: Boolean)
    suspend fun removeMember(conversationId: String, userId: String)
    suspend fun updateGroup(conversationId: String, name: String?, description: String?, avatarUrl: String?)
    suspend fun leaveGroup(conversationId: String, userId: String)
    suspend fun promoteMember(conversationId: String, userId: String)
    suspend fun demoteMember(conversationId: String, userId: String)
    suspend fun uploadGroupAvatar(conversationId: String, bytes: ByteArray): String
}
