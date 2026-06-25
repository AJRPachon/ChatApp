package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.CallBO
import com.ajrpachon.chatapp.domain.model.CallType
import kotlinx.coroutines.flow.Flow

interface CallRepository {
    suspend fun createCall(conversationId: String, calleeId: String, type: CallType): CallBO
    suspend fun createGroupCall(conversationId: String, type: CallType): CallBO
    suspend fun acceptCall(callId: String)
    suspend fun rejectCall(callId: String)
    suspend fun endCall(callId: String)
    fun observeIncomingCalls(userId: String): Flow<CallBO>
    fun observeCallStatus(callId: String): Flow<String>
    fun observeHangupSignal(callId: String): Flow<Unit>
    suspend fun sendHangupSignal(callId: String)
    suspend fun fetchLivekitToken(roomName: String, identity: String): String
}
