package com.ajrpachon.chatapp.domain.repository

/**
 * Abstraction over the analytics backend (Firebase Analytics in [data]).
 * Domain/UI code depends on this interface, never on Firebase directly —
 * keeps analytics testable (fake/mock) and swappable.
 */
interface AnalyticsTracker {
    fun logEvent(name: String, params: Map<String, Any> = emptyMap())
    fun setUserProperty(key: String, value: String)
    fun setUserId(id: String?)
}
