package com.ajrpachon.chatapp.data.remote.source

import com.ajrpachon.chatapp.data.remote.dto.CallDTO
import com.ajrpachon.chatapp.data.remote.dto.UserDTO
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.catchResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private const val TAG = "CallRemoteSource"
private val lenientJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class ParticipantIdDTO(@SerialName("user_id") val userId: String)

@Serializable
private data class CallSignalDTO(
    @SerialName("call_id") val callId: String,
    @SerialName("signal") val signal: String,
    @SerialName("sender_id") val senderId: String,
)

class CallRemoteSource(private val supabase: SupabaseClient) {

    fun getCurrentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    suspend fun insertCall(
        callId: String,
        conversationId: String,
        callerId: String,
        calleeId: String,
        typeStr: String,
        status: String,
        roomName: String,
    ) {
        supabase.postgrest["calls"].insert(buildJsonObject {
            put("id", callId)
            put("conversation_id", conversationId)
            put("caller_id", callerId)
            put("callee_id", calleeId)
            put("type", typeStr)
            put("status", status)
            put("room_name", roomName)
        })
    }

    suspend fun getConversationParticipantIds(
        conversationId: String,
        excludeUserId: String,
    ): List<String> =
        catchResult {
            supabase.postgrest["conversation_participants"]
                .select(Columns.list("user_id")) {
                    filter {
                        eq("conversation_id", conversationId)
                        neq("user_id", excludeUserId)
                    }
                }
                .decodeList<ParticipantIdDTO>()
                .map { it.userId }
        }.getOrDefault(emptyList())

    suspend fun updateCallStatus(callId: String, status: String) {
        supabase.postgrest["calls"].update(
            buildJsonObject { put("status", status) }
        ) { filter { eq("id", callId) } }
    }

    suspend fun getCallerName(callerId: String): String? =
        catchResult {
            supabase.postgrest["profiles"]
                .select { filter { eq("id", callerId) } }
                .decodeSingleOrNull<UserDTO>()
                ?.let { profile -> profile.username?.takeIf { it.isNotBlank() } ?: profile.displayName }
        }.getOrNull()

    fun observeIncomingCalls(userId: String): Flow<Pair<CallDTO, String>> = channelFlow {
        val channelName = "incoming-calls-$userId-${System.nanoTime()}"
        val incomingCallsChannel = supabase.channel(channelName)

        incomingCallsChannel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "calls"
            filter("callee_id", FilterOperator.EQ, userId)
        }.onEach { action ->
            catchResult {
                val callDto = lenientJson.decodeFromJsonElement<CallDTO>(action.record)
                if (callDto.status == "ringing") {
                    val callerName = getCallerName(callDto.callerId) ?: "Unknown"
                    send(callDto to callerName)
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

    fun observeCallStatus(callId: String): Flow<String> = channelFlow {
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

    fun observeHangupSignal(callId: String, currentUserId: String?): Flow<Unit> = channelFlow {
        val channelName = "call-hangup-$callId-${System.nanoTime()}"
        AppLogger.d(TAG, "observeHangupSignal: subscribing channel=$channelName currentUserId=$currentUserId")
        val channel = supabase.channel(channelName)
        channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "call_signals"
            filter("call_id", FilterOperator.EQ, callId)
        }.onEach { action ->
            AppLogger.d(TAG, "observeHangupSignal: INSERT received record=${action.record}")
            catchResult {
                val json = lenientJson.decodeFromJsonElement<CallSignalDTO>(action.record)
                AppLogger.d(TAG, "observeHangupSignal: signal=${json.signal} senderId=${json.senderId} myId=$currentUserId")
                if (json.senderId != currentUserId) {
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

    suspend fun insertHangupSignal(callId: String, senderId: String) {
        catchResult {
            supabase.postgrest["call_signals"].insert(buildJsonObject {
                put("call_id", callId)
                put("signal", "hangup")
                put("sender_id", senderId)
            })
        }.onSuccess { AppLogger.d(TAG, "insertHangupSignal: INSERT OK callId=$callId") }
         .onFailure { e -> AppLogger.e(TAG, "insertHangupSignal: INSERT FAILED callId=$callId", e) }
    }

    suspend fun fetchLivekitToken(roomName: String, identity: String): String {
        val responseText = supabase.functions.invoke(
            function = "livekit-token",
            body = buildJsonObject {
                put("room_name", roomName)
                put("identity", identity)
            },
        ).bodyAsText()
        val json = Json.parseToJsonElement(responseText)
        return json.jsonObject["token"]?.jsonPrimitive?.content
            ?: error("livekit-token Edge Function returned no token")
    }
}
