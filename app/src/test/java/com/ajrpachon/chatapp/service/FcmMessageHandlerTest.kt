package com.ajrpachon.chatapp.service

import android.app.NotificationManager
import android.content.Context
import com.ajrpachon.chatapp.service.FcmMessageHandler.Payload
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FcmMessageHandlerTest {

    private lateinit var context: Context
    private lateinit var handler: FcmMessageHandler
    private lateinit var nm: NotificationManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        handler = FcmMessageHandler(context)
        nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    // ── extractPayload — pure logic ───────────────────────────────────────────

    @Test
    fun `extractPayload returns payload when no active conversation (app dead or background)`() {
        val data = mapOf("title" to "Alice", "body" to "Hola", "conversation_id" to "conv1")
        val payload = handler.extractPayload(data, activeConversationId = null)
        assertNotNull(payload)
        assertEquals(Payload("Alice", "Hola", "conv1"), payload)
    }

    @Test
    fun `extractPayload returns payload when active conversation is different (user in another chat)`() {
        val data = mapOf("title" to "Bob", "body" to "Hey", "conversation_id" to "conv2")
        val payload = handler.extractPayload(data, activeConversationId = "conv1")
        assertNotNull(payload)
        assertEquals("conv2", payload!!.conversationId)
    }

    @Test
    fun `extractPayload suppresses when user is viewing that exact conversation (foreground active chat)`() {
        val data = mapOf("title" to "Bob", "body" to "Hey", "conversation_id" to "conv1")
        val payload = handler.extractPayload(data, activeConversationId = "conv1")
        assertNull(payload)
    }

    @Test
    fun `extractPayload returns payload when message has no conversation_id`() {
        val data = mapOf("title" to "System", "body" to "Update available")
        val payload = handler.extractPayload(data, activeConversationId = "conv1")
        assertNotNull(payload)
        assertNull(payload!!.conversationId)
    }

    @Test
    fun `extractPayload suppresses when title is missing`() {
        val data = mapOf("body" to "text", "conversation_id" to "conv1")
        assertNull(handler.extractPayload(data, null))
    }

    @Test
    fun `extractPayload suppresses when body is missing`() {
        val data = mapOf("title" to "Alice", "conversation_id" to "conv1")
        assertNull(handler.extractPayload(data, null))
    }

    @Test
    fun `extractPayload suppresses when data map is empty`() {
        assertNull(handler.extractPayload(emptyMap(), null))
    }

    // ── showNotification — verifies NotificationManager receives the notification ──

    @Test
    fun `showNotification posts a notification with the correct title and body`() {
        val payload = Payload("Alice", "Hola!", conversationId = "conv-abc")
        handler.showNotification(payload)

        val posted = nm.activeNotifications
        assertEquals(1, posted.size)
        assertEquals("Alice", posted[0].notification.extras.getString("android.title"))
        assertEquals("Hola!", posted[0].notification.extras.getString("android.text"))
    }

    @Test
    fun `showNotification creates the chat_messages channel on first call`() {
        handler.showNotification(Payload("A", "B", "conv1"))
        assertNotNull(nm.getNotificationChannel("chat_messages"))
    }

    @Test
    fun `showNotification reuses same notification ID for the same conversation (updates, no cooldown)`() {
        handler.showNotification(Payload("Alice", "msg 1", "conv1"))
        handler.showNotification(Payload("Alice", "msg 2", "conv1"))

        // Two posts to the same ID → only one active notification
        assertEquals(1, nm.activeNotifications.size)
        assertEquals("msg 2", nm.activeNotifications[0].notification.extras.getString("android.text"))
    }

    @Test
    fun `showNotification uses different IDs for different conversations`() {
        handler.showNotification(Payload("Alice", "hi", "conv1"))
        handler.showNotification(Payload("Bob", "hey", "conv2"))

        assertEquals(2, nm.activeNotifications.size)
    }

    // ── handle — end-to-end through extractPayload + showNotification ────────

    @Test
    fun `handle shows notification when app was dead (no active conversation)`() {
        val data = mapOf("title" to "Alice", "body" to "Hola", "conversation_id" to "conv1")
        handler.handle(data, activeConversationId = null)
        assertEquals(1, nm.activeNotifications.size)
    }

    @Test
    fun `handle shows notification when app is in background (different active conversation)`() {
        val data = mapOf("title" to "Bob", "body" to "Hey", "conversation_id" to "conv2")
        handler.handle(data, activeConversationId = "conv1")
        assertEquals(1, nm.activeNotifications.size)
    }

    @Test
    fun `handle suppresses notification when user is in that chat (foreground active)`() {
        val data = mapOf("title" to "Bob", "body" to "Hey", "conversation_id" to "conv1")
        handler.handle(data, activeConversationId = "conv1")
        assertEquals(0, nm.activeNotifications.size)
    }

    @Test
    fun `handle shows notification when app is in background with no open chat`() {
        val data = mapOf("title" to "Carol", "body" to "Sup", "conversation_id" to "conv3")
        handler.handle(data, activeConversationId = null)
        assertEquals(1, nm.activeNotifications.size)
    }

    @Test
    fun `handle does nothing when required fields are missing`() {
        handler.handle(mapOf("conversation_id" to "conv1"), activeConversationId = null)
        assertEquals(0, nm.activeNotifications.size)
    }

    // ── notifIdForConversation companion logic ────────────────────────────────

    @Test
    fun `notifIdForConversation returns same ID for same conversation across calls`() {
        val id1 = ChatFirebaseMessagingService.notifIdForConversation("conv-x")
        val id2 = ChatFirebaseMessagingService.notifIdForConversation("conv-x")
        assertEquals(id1, id2)
    }

    @Test
    fun `notifIdForConversation returns different IDs for different conversations`() {
        val idA = ChatFirebaseMessagingService.notifIdForConversation("conv-unique-a")
        val idB = ChatFirebaseMessagingService.notifIdForConversation("conv-unique-b")
        assertNotEquals(idA, idB)
    }

    @Test
    fun `notifIdForConversation returns unique IDs for null conversationId`() {
        val id1 = ChatFirebaseMessagingService.notifIdForConversation(null)
        val id2 = ChatFirebaseMessagingService.notifIdForConversation(null)
        assertNotEquals(id1, id2)
    }
}
