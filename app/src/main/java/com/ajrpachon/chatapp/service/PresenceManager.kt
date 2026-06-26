package com.ajrpachon.chatapp.service

import com.ajrpachon.chatapp.domain.repository.UserRepository
import com.ajrpachon.chatapp.utils.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "PresenceManager"
private const val HEARTBEAT_INTERVAL_MS = 60_000L

class PresenceManager(private val userRepository: UserRepository) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var heartbeatJob: Job? = null

    fun start() {
        if (heartbeatJob?.isActive == true) return
        heartbeatJob = scope.launch {
            val userId = userRepository.getCurrentUserId() ?: return@launch
            while (isActive) {
                runCatching { userRepository.updateLastSeen(userId) }
                    .onFailure { AppLogger.e(TAG, "updateLastSeen failed", it) }
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }
}
