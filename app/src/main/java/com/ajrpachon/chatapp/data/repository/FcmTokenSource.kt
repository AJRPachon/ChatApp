package com.ajrpachon.chatapp.data.repository

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/** The device's FCM registration token, behind an interface so [FcmTokenRepositoryImpl] can be tested. */
interface FcmTokenSource {
    suspend fun currentToken(): String

    /** Invalidates the current token; the next [currentToken] returns a new one. */
    suspend fun invalidate()
}

class FirebaseFcmTokenSource : FcmTokenSource {
    override suspend fun currentToken(): String = FirebaseMessaging.getInstance().token.await()

    override suspend fun invalidate() {
        FirebaseMessaging.getInstance().deleteToken().await()
    }
}
