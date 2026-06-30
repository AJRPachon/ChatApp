package com.ajrpachon.chatapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.ajrpachon.chatapp.MainActivity
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.data.local.NotificationSoundRepository
import com.ajrpachon.chatapp.domain.model.NotificationSound
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.runBlocking

class FcmMessageHandler(
    private val context: Context,
    private val notificationSoundRepository: NotificationSoundRepository,
) {

    data class Payload(val title: String, val body: String, val conversationId: String?)

    // Per-conversation message history for MessagingStyle (cleared when notification is dismissed)
    private data class StyleMessage(val text: String, val senderName: String, val timestampMs: Long)
    private val messageHistory = ConcurrentHashMap<String, ArrayDeque<StyleMessage>>()

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

        val convId = payload.conversationId ?: ""
        val notifId = ChatFirebaseMessagingService.notifIdForConversation(payload.conversationId)

        // Accumulate message history for MessagingStyle (max 5 per conversation)
        val history = messageHistory.getOrPut(convId) { ArrayDeque() }
        history.addLast(StyleMessage(payload.body, payload.title, System.currentTimeMillis()))
        while (history.size > 5) history.removeFirst()

        // Tap intent — deep link to conversation
        val tapIntent = if (payload.conversationId != null) {
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("chatapp://chat/${payload.conversationId}?name=${Uri.encode(payload.title)}"),
                context,
                MainActivity::class.java,
            )
        } else {
            Intent(context, MainActivity::class.java)
        }.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPi = PendingIntent.getActivity(
            context, notifId, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Reply action
        val remoteInput = RemoteInput.Builder(NotificationReplyReceiver.KEY_REPLY_TEXT)
            .setLabel("Responder…")
            .build()
        val replyIntent = Intent(context, NotificationReplyReceiver::class.java).apply {
            action = NotificationReplyReceiver.ACTION_REPLY
            putExtra(NotificationReplyReceiver.EXTRA_CONVERSATION_ID, payload.conversationId)
            putExtra(NotificationReplyReceiver.EXTRA_NOTIF_ID, notifId)
        }
        val replyPi = PendingIntent.getBroadcast(
            context, notifId + 10_000, replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        val replyAction = NotificationCompat.Action.Builder(
            R.mipmap.ic_launcher, "Responder", replyPi
        ).addRemoteInput(remoteInput).build()

        // Build MessagingStyle
        val me = Person.Builder().setName("Tú").build()
        val style = NotificationCompat.MessagingStyle(me)
        history.forEach { msg ->
            val sender = Person.Builder().setName(msg.senderName).build()
            style.addMessage(msg.text, msg.timestampMs, sender)
        }

        // Resolve custom notification sound for this conversation
        val soundUri: Uri? = if (payload.conversationId != null) {
            val sound = runBlocking { notificationSoundRepository.get(payload.conversationId) }
            resolveUri(sound)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setStyle(style)
            .setAutoCancel(true)
            .setContentIntent(tapPi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(replyAction)
            .setGroup(convId)

        if (soundUri != null) {
            builder.setSound(soundUri)
        }

        nm.notify(notifId, builder.build())
    }

    private fun resolveUri(sound: NotificationSound): Uri? = when (sound) {
        NotificationSound.DEFAULT ->
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        NotificationSound.SILENT -> null
        else -> sound.resId?.let { resId ->
            Uri.parse("android.resource://${context.packageName}/$resId")
        } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
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
        val body = if (callType == "video") "📹 Videollamada entrante" else "📞 Llamada de voz entrante"

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
            .setContentTitle(callerName)
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

    fun clearConversationHistory(conversationId: String) {
        messageHistory.remove(conversationId)
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
