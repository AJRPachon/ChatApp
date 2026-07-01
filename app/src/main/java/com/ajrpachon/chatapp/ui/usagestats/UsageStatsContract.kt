package com.ajrpachon.chatapp.ui.usagestats

data class UsageStatsState(
    val isLoading: Boolean = true,
    val totalMessagesSent: Int = 0,
    val totalMessagesReceived: Int = 0,
    val totalCalls: Int = 0,
    val totalCallMinutes: Int = 0,
    val totalImages: Int = 0,
    val totalAudio: Int = 0,
    val totalVideos: Int = 0,
    val mostActiveConvName: String = "",
    val messagesPerDay: List<Pair<String, Int>> = emptyList(),
    val error: String? = null,
)

sealed interface UsageStatsIntent {
    data object Reload : UsageStatsIntent
}
