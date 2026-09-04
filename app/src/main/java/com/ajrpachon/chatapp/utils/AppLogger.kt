package com.ajrpachon.chatapp.utils

import android.util.Log
import com.ajrpachon.chatapp.BuildConfig
import com.ajrpachon.chatapp.domain.repository.CrashReporter

object AppLogger {
    // Set once from Application.onCreate() (see init()). Every other caller — 80+ existing
    // AppLogger.e(...) sites across the app — gets Crashlytics reporting for free, without
    // each call site needing to know about CrashReporter.
    @Volatile
    private var crashReporter: CrashReporter? = null

    /** Wires Crashlytics reporting into [e] going forward. Call once, at app startup. */
    fun init(crashReporter: CrashReporter) {
        this.crashReporter = crashReporter
    }

    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(tag, message)
    }
    fun i(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.i(tag, message)
    }
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) Log.w(tag, message, throwable)
    }
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        // Errors always logged but without sensitive details in release
        if (BuildConfig.DEBUG) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
        crashReporter?.log("$tag: $message")
        if (throwable != null) {
            crashReporter?.recordException(throwable)
        }
    }
}
