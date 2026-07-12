package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.remote.dto.toBO
import com.ajrpachon.chatapp.data.remote.source.CallRemoteSource
import com.ajrpachon.chatapp.domain.model.CallBO
import com.ajrpachon.chatapp.domain.model.CallStatus
import com.ajrpachon.chatapp.domain.model.CallType
import com.ajrpachon.chatapp.domain.repository.CallRepository
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.catchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val TAG = "CallRepositoryImpl"

class CallRepositoryImpl(
    private val callRemoteSource: CallRemoteSource,
) : CallRepository {

    override suspend fun createCall(
        conversationId: String,
        calleeId: String,
        type: CallType,
    ): CallBO {
        val callerId = callRemoteSource.getCurrentUserId() ?: error("Not authenticated")
        val callId = java.util.UUID.randomUUID().toString()
        val roomName = "room_${conversationId.take(8)}_${System.currentTimeMillis()}"
        val typeStr = if (type == CallType.VIDEO) "video" else "audio"

        callRemoteSource.insertCall(
            callId = callId,
            conversationId = conversationId,
            callerId = callerId,
            calleeId = calleeId,
            typeStr = typeStr,
            status = "ringing",
            roomName = roomName,
        )

        return CallBO(
            id = callId,
            conversationId = conversationId,
            callerId = callerId,
            callerName = "",
            calleeId = calleeId,
            type = type,
            status = CallStatus.RINGING,
            roomName = roomName,
        )
    }

    override suspend fun createGroupCall(conversationId: String, type: CallType): CallBO {
        val callerId = callRemoteSource.getCurrentUserId() ?: error("Not authenticated")
        val roomName = "group_${conversationId.take(8)}_${System.currentTimeMillis()}"
        val typeStr = if (type == CallType.VIDEO) "video" else "audio"

        val memberIds = callRemoteSource.getConversationParticipantIds(conversationId, callerId)

        val masterCallId = java.util.UUID.randomUUID().toString()
        callRemoteSource.insertCall(
            callId = masterCallId,
            conversationId = conversationId,
            callerId = callerId,
            calleeId = callerId,
            typeStr = typeStr,
            status = "ringing",
            roomName = roomName,
        )

        for (memberId in memberIds) {
            catchResult {
                callRemoteSource.insertCall(
                    callId = java.util.UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    callerId = callerId,
                    calleeId = memberId,
                    typeStr = typeStr,
                    status = "ringing",
                    roomName = roomName,
                )
            }
        }

        return CallBO(
            id = masterCallId,
            conversationId = conversationId,
            callerId = callerId,
            callerName = "",
            calleeId = callerId,
            type = type,
            status = CallStatus.RINGING,
            roomName = roomName,
        )
    }

    override suspend fun acceptCall(callId: String) {
        catchResult { callRemoteSource.updateCallStatus(callId, "active") }
    }

    override suspend fun rejectCall(callId: String) {
        catchResult { callRemoteSource.updateCallStatus(callId, "rejected") }
    }

    override suspend fun endCall(callId: String) {
        catchResult { callRemoteSource.updateCallStatus(callId, "ended") }
    }

    override fun observeIncomingCalls(userId: String): Flow<CallBO> =
        callRemoteSource.observeIncomingCalls(userId).map { (callDto, callerName) ->
            callDto.toBO(callerName)
        }

    override fun observeCallStatus(callId: String): Flow<String> =
        callRemoteSource.observeCallStatus(callId)

    override fun observeHangupSignal(callId: String): Flow<Unit> {
        val currentUserId = callRemoteSource.getCurrentUserId()
        AppLogger.d(TAG, "observeHangupSignal: delegating callId=$callId currentUserId=$currentUserId")
        return callRemoteSource.observeHangupSignal(callId, currentUserId)
    }

    override suspend fun sendHangupSignal(callId: String) {
        val senderId = callRemoteSource.getCurrentUserId()
        AppLogger.d(TAG, "sendHangupSignal: callId=$callId senderId=$senderId")
        if (senderId == null) {
            AppLogger.e(TAG, "sendHangupSignal: NOT AUTHENTICATED, aborting")
            return
        }
        callRemoteSource.insertHangupSignal(callId, senderId)
    }

    override suspend fun fetchLivekitToken(roomName: String, identity: String): String =
        callRemoteSource.fetchLivekitToken(roomName, identity)
}
