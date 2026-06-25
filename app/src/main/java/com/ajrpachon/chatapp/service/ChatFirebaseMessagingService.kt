package com.ajrpachon.chatapp.service
import com.ajrpachon.chatapp.utils.catchResult

import com.ajrpachon.chatapp.data.remote.source.FcmTokenRemoteSource
import com.ajrpachon.chatapp.utils.AppLogger
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class ChatFirebaseMessagingService : FirebaseMessagingService() {

    private val fcmTokenRemoteSource: FcmTokenRemoteSource by inject()
    private val fcmTokenManager: FcmTokenManager by inject()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.IO + CoroutineName("FCMService"))

    override fun onNewToken(token: String) {
        // Save locally first in case the user isn't authenticated yet.
        // FcmTokenManager.syncToken() will push it on next login.
        fcmTokenManager.savePendingToken(token)
        scope.launch { catchResult { fcmTokenRemoteSource.upsertToken(token) } }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val conversationId = message.data["conversation_id"]
        AppLogger.d(TAG, "onMessageReceived: convId=$conversationId activeId=${ActiveChatTracker.activeConversationId}")

        // Merge notification fields into data map so FcmMessageHandler works with a single source.
        val data = message.data.toMutableMap()
        message.notification?.title?.let { data.putIfAbsent("title", it) }
        message.notification?.body?.let { data.putIfAbsent("body", it) }

        FcmMessageHandler(this).handle(data, ActiveChatTracker.activeConversationId)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        private const val TAG = "ChatFCMService"
        private val counter = AtomicInteger(1)
        // Reuse the same notification ID per conversation so updates replace instead of stack.
        private val convNotifIds = ConcurrentHashMap<String, Int>()

        fun notifIdForConversation(conversationId: String?): Int {
            if (conversationId == null) return counter.getAndIncrement()
            return convNotifIds.getOrPut(conversationId) { counter.getAndIncrement() }
        }
    }
}
