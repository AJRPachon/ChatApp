package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.domain.repository.CrashReporter
import com.google.firebase.crashlytics.FirebaseCrashlytics

class FirebaseCrashReporter(
    private val crashlytics: FirebaseCrashlytics,
) : CrashReporter {

    override fun recordException(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }

    override fun log(message: String) {
        crashlytics.log(message)
    }

    override fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }

    override fun setUserId(id: String?) {
        crashlytics.setUserId(id.orEmpty())
    }
}
