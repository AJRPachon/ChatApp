package com.ajrpachon.chatapp.ui.usagestats

import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.ui.common.BaseViewModel
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.catchResult
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import java.util.concurrent.TimeUnit

class UsageStatsViewModel(
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
) : BaseViewModel<UsageStatsState, Nothing>(UsageStatsState()) {

    init {
        loadStats()
    }

    fun onIntent(intent: UsageStatsIntent) {
        when (intent) {
            is UsageStatsIntent.Reload -> loadStats()
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            catchResult {
                val userId = getCurrentUserUseCase().filterNotNull().first().id

                val sent = messageRepository.countSent(userId)
                val received = messageRepository.countReceived(userId)
                val calls = messageRepository.countCalls()
                val callSeconds = messageRepository.sumCallDurationSeconds()
                val images = messageRepository.countImages()
                val audio = messageRepository.countAudio()
                val videos = messageRepository.countVideos()

                val mostActiveConvId = messageRepository.getMostActiveConversationId()
                val mostActiveName = mostActiveConvId?.let { id ->
                    conversationRepository.getById(id)?.name ?: ""
                } ?: ""

                val sevenDaysAgoMs = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
                val dayCounts = messageRepository.countMessagesByDay(sevenDaysAgoMs)

                // Build a map of dayEpoch -> count from DB results
                val dbMap = dayCounts.toMap()

                // Generate last 7 days (today inclusive) as Pair<label, count>
                val zoneId = ZoneId.systemDefault()
                val todayEpochDay = Instant.now().atZone(zoneId).toLocalDate().toEpochDay()
                val messagesPerDay = (6 downTo 0).map { daysAgo ->
                    val epochDay = todayEpochDay - daysAgo
                    val date = java.time.LocalDate.ofEpochDay(epochDay)
                    val label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es", "ES"))
                        .replaceFirstChar { it.uppercaseChar() }
                    val count = dbMap[epochDay] ?: 0
                    label to count
                }

                updateState {
                    it.copy(
                        isLoading = false,
                        totalMessagesSent = sent,
                        totalMessagesReceived = received,
                        totalCalls = calls,
                        totalCallMinutes = callSeconds / 60,
                        totalImages = images,
                        totalAudio = audio,
                        totalVideos = videos,
                        mostActiveConvName = mostActiveName,
                        messagesPerDay = messagesPerDay,
                    )
                }
            }.onFailure { e ->
                AppLogger.e(TAG, "loadStats failed", e)
                updateState { it.copy(isLoading = false, error = e.message ?: "Error al cargar estadísticas") }
            }
        }
    }

    companion object {
        private const val TAG = "UsageStatsViewModel"
    }
}
