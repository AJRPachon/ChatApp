package com.ajrpachon.chatapp.ui.status

import android.net.Uri
import com.ajrpachon.chatapp.domain.model.StatusBO

data class StatusState(
    val statuses: List<StatusBO> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showComposeDialog: Boolean = false,
    val composeText: String = "",
    val selectedColor: Long = 0xFF1976D2,
    /** Statuses pre-filtered for a single user, set by [StatusIntent.FilterUserStatuses]. */
    val userStatuses: List<StatusBO> = emptyList(),
)

sealed interface StatusEffect

sealed interface StatusIntent {
    data object Refresh : StatusIntent
    data object OpenCompose : StatusIntent
    data object CloseCompose : StatusIntent
    data class TextChanged(val text: String) : StatusIntent
    data class ColorChanged(val color: Long) : StatusIntent
    data object PostTextStatus : StatusIntent
    data class PostImageStatus(val uri: Uri) : StatusIntent
    data class DeleteStatus(val statusId: String) : StatusIntent
    /** Filters [allStatuses] by [userId] and stores the result in [StatusState.userStatuses]. */
    data class FilterUserStatuses(val allStatuses: List<StatusBO>, val userId: String) : StatusIntent
}
