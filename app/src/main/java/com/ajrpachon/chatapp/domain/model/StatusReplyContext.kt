package com.ajrpachon.chatapp.domain.model

/**
 * Snapshot of the status being replied to, captured at reply time and carried on the resulting
 * [MessageBO] as `replyToStatus*` fields — not a live reference. This is deliberate: once
 * [statusExpiresAt] passes, the quote in chat must read as "no longer available" (mirrors
 * WhatsApp), so the message never needs to re-fetch or join against the (by then likely deleted)
 * `user_status` row to know whether to show it.
 */
data class StatusReplyContext(
    val statusId: String,
    val statusOwnerId: String,
    val statusText: String?,
    val statusImageUrl: String?,
    val statusVideoUrl: String?,
    val statusBackgroundColor: Long?,
    val statusExpiresAt: Long,
)
