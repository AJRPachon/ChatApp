package com.ajrpachon.chatapp.domain.repository

/**
 * Abstraction over the crash-reporting backend (Firebase Crashlytics in [data]).
 * Domain/UI code depends on this interface, never on Firebase directly —
 * keeps crash reporting testable (fake/mock) and swappable.
 */
interface CrashReporter {
    /** Reports a caught (non-fatal) exception. */
    fun recordException(throwable: Throwable)

    /** Adds a breadcrumb log line attached to the next crash/exception report. */
    fun log(message: String)

    /** Attaches a custom key/value to future crash reports. */
    fun setCustomKey(key: String, value: String)

    /** Associates crash reports with a (non-PII) user id, or clears it when `null`. */
    fun setUserId(id: String?)
}
