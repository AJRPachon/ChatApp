package com.ajrpachon.chatapp.ui.call

import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
import com.ajrpachon.chatapp.utils.AnalyticsEvents

/**
 * Tracks `call_started`/`call_ended` analytics for one [CallViewModel] instance, exactly once
 * per call regardless of which of its several code paths triggers the ACTIVE/ENDED transition.
 * Fires symmetrically for both call directions (outgoing/incoming) and both kinds (1:1/group) —
 * unlike the call-summary chat message, which is only sent by the outgoing side.
 */
class CallAnalyticsLogger(
    private val analyticsTracker: AnalyticsTracker,
    private val callType: String,
    private val isGroup: Boolean,
) {
    private var startedLogged = false
    private var endedLogged = false

    fun logStarted() {
        if (startedLogged) return
        startedLogged = true
        analyticsTracker.logEvent(
            AnalyticsEvents.CALL_STARTED,
            mapOf(
                AnalyticsEvents.PARAM_CALL_TYPE to callType,
                AnalyticsEvents.PARAM_IS_GROUP to isGroup,
            ),
        )
    }

    fun logEnded(status: String, durationSeconds: Int) {
        if (endedLogged) return
        endedLogged = true
        val duration = if (status == "ended") durationSeconds else 0
        analyticsTracker.logEvent(
            AnalyticsEvents.CALL_ENDED,
            mapOf(
                AnalyticsEvents.PARAM_CALL_TYPE to callType,
                AnalyticsEvents.PARAM_CALL_STATUS to status,
                AnalyticsEvents.PARAM_CALL_DURATION_SECONDS to duration,
                AnalyticsEvents.PARAM_IS_GROUP to isGroup,
            ),
        )
    }
}
