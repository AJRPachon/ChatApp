package com.ajrpachon.chatapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.ajrpachon.chatapp.MainActivity
import com.ajrpachon.chatapp.R

class FcmMessageHandler(private val context: Context) {

    data class Payload(val title: String, val body: String, val conversationId: String?)

    fun extractPayload(data: Map<String, String>, activeConversationId: String?): Payload? {
        val conversationId = data["conversation_id"]
        if (conversationId != null && conversationId == activeConversationId) return null
        val title = data["title"] ?: return null
        val body = data["body"] ?: return null
        return Payload(title, body, conversationId)
    }

    fun showNotification(payload: Payload) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "chat_messages"
        if (nm.getNotificationChannel(channelId) == null) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Mensajes", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            payload.conversationId?.let { putExtra("conversation_id", it) }
            putExtra("other_user_name", payload.title)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(payload.title)
            .setContentText(payload.body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        nm.notify(ChatFirebaseMessagingService.notifIdForConversation(payload.conversationId), notification)
    }

    fun showIncomingCallNotification(data: Map<String, String>) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "incoming_calls"
        if (nm.getNotificationChannel(channelId) == null) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Llamadas entrantes", NotificationManager.IMPORTANCE_HIGH).apply {
                    setBypassDnd(true)
                    enableVibration(true)
                }
            )
        }

        val callerName = data["caller_name"] ?: "Alguien"
        val callType = data["call_type"] ?: "audio"
        val conversationId = data["conversation_id"]
        val title = callerName
        val body = if (callType == "video") "📹 Videollamada entrante" else "📞 Llamada de voz entrante"

        // Tap → open app (IncomingCallViewModel will show the call UI via Realtime)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            conversationId?.let { putExtra("conversation_id", it) }
            putExtra("other_user_name", callerName)
        }
        val openPi = PendingIntent.getActivity(
            context, 100, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(openPi)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(openPi, true)
            .setOngoing(true)
            .setTimeoutAfter(30_000)
            .build()

        nm.notify(CALL_NOTIF_ID, notification)
    }

    fun cancelIncomingCallNotification() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(CALL_NOTIF_ID)
    }

    fun handle(data: Map<String, String>, activeConversationId: String?) {
        if (data["type"] == "incoming_call") {
            showIncomingCallNotification(data)
            return
        }
        val payload = extractPayload(data, activeConversationId) ?: return
        showNotification(payload)
    }

    companion object {
        const val CALL_NOTIF_ID = 9999
    }
}
