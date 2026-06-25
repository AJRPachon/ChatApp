package com.ajrpachon.chatapp.utils

import android.util.Log
import com.ajrpachon.chatapp.BuildConfig

object AppLogger {
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
    }
}
