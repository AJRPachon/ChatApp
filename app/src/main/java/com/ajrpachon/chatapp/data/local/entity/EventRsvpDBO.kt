package com.ajrpachon.chatapp.data.local.entity

import androidx.room.Entity

@Entity(tableName = "event_rsvps", primaryKeys = ["eventId", "userId"])
data class EventRsvpDBO(
    val eventId: String,
    val userId: String,
    val status: String, // "going" | "not_going"
)
