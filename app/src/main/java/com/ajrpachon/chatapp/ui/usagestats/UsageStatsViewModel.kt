package com.ajrpachon.chatapp.ui.usagestats

import com.ajrpachon.chatapp.data.local.dao.ConversationDao
import com.ajrpachon.chatapp.data.local.dao.MessageDao
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
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
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

                val sent = messageDao.countSent(userId)
                val received = messageDao.countReceived(userId)
                val calls = messageDao.countCalls()
                val callSeconds = messageDao.sumCallDurationSeconds()
                val images = messageDao.countImages()
                val audio = messageDao.countAudio()
                val videos = messageDao.countVideos()

                val mostActive = messageDao.getMostActiveConversation()
                val mostActiveName = mostActive?.let { result ->
                    conversationDao.getById(result.conversationId)?.name ?: ""
                } ?: ""

                val sevenDaysAgoMs = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
                val dayCounts = messageDao.countMessagesByDay(sevenDaysAgoMs)

                // Build a map of dayEpoch -> count from DB results
                val dbMap = dayCounts.associate { it.dayEpoch to it.count }

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
