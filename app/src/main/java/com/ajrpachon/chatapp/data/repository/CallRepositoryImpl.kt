package com.ajrpachon.chatapp.data.repository
import com.ajrpachon.chatapp.utils.catchResult

import java.util.Base64
import com.ajrpachon.chatapp.data.remote.dto.CallDTO
import com.ajrpachon.chatapp.data.remote.dto.UserDTO
import com.ajrpachon.chatapp.data.remote.dto.toBO
import com.ajrpachon.chatapp.domain.model.CallBO
import com.ajrpachon.chatapp.domain.model.CallStatus
import com.ajrpachon.chatapp.domain.model.CallType
import com.ajrpachon.chatapp.domain.repository.CallRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import com.ajrpachon.chatapp.utils.AppLogger

private const val TAG = "CallRepositoryImpl"
private val lenientJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class ParticipantIdDTO(@SerialName("user_id") val userId: String)

@Serializable
private data class CallSignalDTO(
    @SerialName("call_id") val callId: String,
    @SerialName("signal") val signal: String,
    @SerialName("sender_id") val senderId: String,
)

class CallRepositoryImpl(
    private val supabase: SupabaseClient,
    private val livekitApiKey: String,
    private val livekitApiSecret: String,
) : CallRepository {

    override suspend fun createCall(
        conversationId: String,
        calleeId: String,
        type: CallType,
    ): CallBO {
        val callerId = supabase.auth.currentUserOrNull()?.id ?: error("Not authenticated")
        val callId = java.util.UUID.randomUUID().toString()
        val roomName = "room_${conversationId.take(8)}_${System.currentTimeMillis()}"
        val typeStr = if (type == CallType.VIDEO) "video" else "audio"

        supabase.postgrest["calls"].insert(buildJsonObject {
            put("id", callId)
            put("conversation_id", conversationId)
            put("caller_id", callerId)
            put("callee_id", calleeId)
            put("type", typeStr)
            put("status", "ringing")
            put("room_name", roomName)
        })

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
        val callerId = supabase.auth.currentUserOrNull()?.id ?: error("Not authenticated")
        val roomName = "group_${conversationId.take(8)}_${System.currentTimeMillis()}"
        val typeStr = if (type == CallType.VIDEO) "video" else "audio"

        val members = catchResult {
            supabase.postgrest["conversation_participants"]
                .select(Columns.list("user_id")) {
                    filter {
                        eq("conversation_id", conversationId)
                        neq("user_id", callerId)
                    }
                }
                .decodeList<ParticipantIdDTO>()
        }.getOrDefault(emptyList())

        val masterCallId = java.util.UUID.randomUUID().toString()
        supabase.postgrest["calls"].insert(buildJsonObject {
            put("id", masterCallId)
            put("conversation_id", conversationId)
            put("caller_id", callerId)
            put("callee_id", callerId)
            put("type", typeStr)
            put("status", "ringing")
            put("room_name", roomName)
        })

        for (m in members) {
            catchResult {
                supabase.postgrest["calls"].insert(buildJsonObject {
                    put("id", java.util.UUID.randomUUID().toString())
                    put("conversation_id", conversationId)
                    put("caller_id", callerId)
                    put("callee_id", m.userId)
                    put("type", typeStr)
                    put("status", "ringing")
                    put("room_name", roomName)
                })
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
        catchResult {
            supabase.postgrest["calls"].update(
                buildJsonObject { put("status", "active") }
            ) { filter { eq("id", callId) } }
        }
    }

    override suspend fun rejectCall(callId: String) {
        catchResult {
            supabase.postgrest["calls"].update(
                buildJsonObject { put("status", "rejected") }
            ) { filter { eq("id", callId) } }
        }
    }

    override suspend fun endCall(callId: String) {
        catchResult {
            supabase.postgrest["calls"].update(
                buildJsonObject { put("status", "ended") }
            ) { filter { eq("id", callId) } }
        }
    }

    override fun observeIncomingCalls(userId: String): Flow<CallBO> = channelFlow {
        val incomingCallsChannel = supabase.channel("incoming-calls-$userId-${System.nanoTime()}")

        // Register listener before subscribing so no events are missed
        incomingCallsChannel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "calls"
            filter("callee_id", FilterOperator.EQ, userId)
        }.onEach { action ->
            catchResult {
                val callDto = lenientJson.decodeFromJsonElement<CallDTO>(action.record)
                if (callDto.status == "ringing") {
                    val callerName = catchResult {
                        supabase.postgrest["profiles"]
                            .select { filter { eq("id", callDto.callerId) } }
                            .decodeSingleOrNull<UserDTO>()
                            ?.let { profile -> profile.username?.takeIf { it.isNotBlank() } ?: profile.displayName }
                    }.getOrNull() ?: "Unknown"
                    send(callDto.toBO(callerName))
                }
            }
        }.launchIn(this)

        incomingCallsChannel.subscribe()

        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                catchResult { incomingCallsChannel.unsubscribe() }
                catchResult { supabase.realtime.removeChannel(incomingCallsChannel) }
            }
        }
    }

    override fun observeCallStatus(callId: String): Flow<String> = channelFlow {
        val callStatusChannel = supabase.channel("call-status-$callId-${System.nanoTime()}")
        callStatusChannel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "calls"
            filter("id", FilterOperator.EQ, callId)
        }.onEach { action ->
            catchResult {
                val callDto = lenientJson.decodeFromJsonElement<CallDTO>(action.record)
                send(callDto.status)
            }
        }.launchIn(this)
        callStatusChannel.subscribe()
        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                catchResult { callStatusChannel.unsubscribe() }
                catchResult { supabase.realtime.removeChannel(callStatusChannel) }
            }
        }
    }

    override fun observeHangupSignal(callId: String): Flow<Unit> = channelFlow {
        val senderId = supabase.auth.currentUserOrNull()?.id
        val channelName = "call-hangup-$callId-${System.nanoTime()}"
        AppLogger.d(TAG, "observeHangupSignal: subscribing channel=$channelName senderId=$senderId")
        val channel = supabase.channel(channelName)
        channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "call_signals"
            filter("call_id", FilterOperator.EQ, callId)
        }.onEach { action ->
            AppLogger.d(TAG, "observeHangupSignal: INSERT received record=${action.record}")
            catchResult {
                val json = lenientJson.decodeFromJsonElement<CallSignalDTO>(action.record)
                AppLogger.d(TAG, "observeHangupSignal: signal=${json.signal} senderId=${json.senderId} mySenderId=$senderId")
                if (json.senderId != senderId) {
                    AppLogger.d(TAG, "observeHangupSignal: emitting Unit (different sender)")
                    send(Unit)
                } else {
                    AppLogger.d(TAG, "observeHangupSignal: ignoring own signal")
                }
            }.onFailure { e -> AppLogger.e(TAG, "observeHangupSignal: decode failed", e) }
        }.launchIn(this)
        channel.subscribe()
        AppLogger.d(TAG, "observeHangupSignal: subscribed to $channelName")
        try {
            awaitCancellation()
        } finally {
            AppLogger.d(TAG, "observeHangupSignal: unsubscribing $channelName")
            withContext(NonCancellable) {
                catchResult { channel.unsubscribe() }
                catchResult { supabase.realtime.removeChannel(channel) }
            }
        }
    }

    override suspend fun sendHangupSignal(callId: String) {
        val senderId = supabase.auth.currentUserOrNull()?.id
        AppLogger.d(TAG, "sendHangupSignal: callId=$callId senderId=$senderId")
        if (senderId == null) {
            AppLogger.e(TAG, "sendHangupSignal: NOT AUTHENTICATED, aborting")
            return
        }
        catchResult {
            supabase.postgrest["call_signals"].insert(buildJsonObject {
                put("call_id", callId)
                put("signal", "hangup")
                put("sender_id", senderId)
            })
        }.onSuccess { AppLogger.d(TAG, "sendHangupSignal: INSERT OK callId=$callId") }
         .onFailure { e -> AppLogger.e(TAG, "sendHangupSignal: INSERT FAILED callId=$callId", e) }
    }

    override fun fetchLivekitToken(roomName: String, identity: String): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString(
            """{"alg":"HS256","typ":"JWT"}""".toByteArray(Charsets.UTF_8)
        )
        val now = System.currentTimeMillis() / 1000
        val grants = """{"roomJoin":true,"room":"$roomName","canPublish":true,"canSubscribe":true}"""
        val payload = encoder.encodeToString(
            """{"iss":"$livekitApiKey","sub":"$identity","iat":$now,"exp":${now + 21600},"nbf":${now - 5},"video":$grants}"""
                .toByteArray(Charsets.UTF_8)
        )
        val data = "$header.$payload"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(livekitApiSecret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val sig = encoder.encodeToString(mac.doFinal(data.toByteArray(Charsets.UTF_8)))
        return "$data.$sig"
    }
}
