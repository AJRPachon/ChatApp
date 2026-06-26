package com.ajrpachon.chatapp.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.ajrpachon.chatapp.domain.repository.UserRepository
import com.ajrpachon.chatapp.domain.usecase.SendMessageUseCase
import com.ajrpachon.chatapp.utils.AppLogger
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NotificationReplyReceiver : BroadcastReceiver(), KoinComponent {

    private val sendMessageUseCase: SendMessageUseCase by inject()
    private val userRepository: UserRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val replyBundle = RemoteInput.getResultsFromIntent(intent) ?: return
        val replyText = replyBundle.getCharSequence(KEY_REPLY_TEXT)?.toString()?.trim() ?: return
        val conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID) ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)

        val senderId = userRepository.getCurrentUserId() ?: return

        // Dismiss the notification immediately so the user gets feedback
        if (notifId != -1) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(notifId)
        }

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("NotifReply"))
        scope.launch {
            sendMessageUseCase(conversationId, senderId, replyText)
                .onFailure { e -> AppLogger.e(TAG, "Inline reply failed: ${e.message}") }
        }
    }

    companion object {
        const val ACTION_REPLY = "com.ajrpachon.chatapp.ACTION_NOTIFICATION_REPLY"
        const val KEY_REPLY_TEXT = "reply_text"
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_NOTIF_ID = "notif_id"
        private const val TAG = "NotificationReplyReceiver"
    }
}
