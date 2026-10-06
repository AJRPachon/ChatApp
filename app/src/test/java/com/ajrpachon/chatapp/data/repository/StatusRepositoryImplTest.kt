package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.local.dao.StatusDao
import com.ajrpachon.chatapp.data.local.dao.UserDao
import com.ajrpachon.chatapp.data.local.entity.StatusDBO
import com.ajrpachon.chatapp.data.local.entity.UserDBO
import com.ajrpachon.chatapp.data.remote.dto.StatusDTO
import com.ajrpachon.chatapp.data.remote.source.StatusRemoteSource
import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
import com.ajrpachon.chatapp.domain.repository.AnalyticsEvents
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusRepositoryImplTest {

    private val statusDao = mockk<StatusDao>(relaxed = true)
    private val userDao = mockk<UserDao>()
    private val remote = mockk<StatusRemoteSource>(relaxed = true)
    private val analytics = mockk<AnalyticsTracker>(relaxed = true)
    private val repo = StatusRepositoryImpl(statusDao, userDao, remote, analytics)

    private fun dto(id: String, userId: String = "me") = StatusDTO(
        id = id,
        userId = userId,
        createdAt = "2026-10-05T10:00:00Z",
        expiresAt = "2026-10-06T10:00:00Z",
    )

    private fun statusDbo(id: String, userId: String) = StatusDBO(
        id = id,
        userId = userId,
        text = "t",
        imageUrl = null,
        createdAt = 1L,
        expiresAt = 2L,
    )

    private fun user(id: String, name: String) = UserDBO(
        id = id,
        email = "",
        username = name,
        displayName = name,
        avatarUrl = null,
        createdAt = 0L,
    )

    @Test
    fun `syncStatuses does nothing when nobody is signed in`() = runTest {
        every { remote.getCurrentUserId() } returns null

        repo.syncStatuses(listOf("u2"))

        coVerify(exactly = 0) { remote.getActiveStatuses(any()) }
        coVerify(exactly = 0) { statusDao.upsertAll(any()) }
    }

    @Test
    fun `syncStatuses asks for the contacts plus the current user without duplicates`() = runTest {
        every { remote.getCurrentUserId() } returns "me"
        coEvery { remote.getActiveStatuses(any()) } returns emptyList()

        repo.syncStatuses(listOf("u2", "me", "u2"))

        coVerify { remote.getActiveStatuses(listOf("u2", "me")) }
    }

    @Test
    fun `syncStatuses drops expired rows then stores what the server returned`() = runTest {
        every { remote.getCurrentUserId() } returns "me"
        coEvery { remote.getActiveStatuses(any()) } returns listOf(dto("s1", "u2"), dto("s2", "me"))
        val stored = slot<List<StatusDBO>>()

        repo.syncStatuses(listOf("u2"))

        coVerify(ordering = Ordering.ORDERED) {
            statusDao.deleteExpired(any())
            statusDao.upsertAll(capture(stored))
        }
        assertEquals(listOf("s1", "s2"), stored.captured.map { it.id })
    }

    @Test
    fun `syncStatuses swallows a failing server call`() = runTest {
        every { remote.getCurrentUserId() } returns "me"
        coEvery { remote.getActiveStatuses(any()) } throws IllegalStateException("offline")

        repo.syncStatuses(emptyList())

        coVerify(exactly = 0) { statusDao.upsertAll(any()) }
    }

    @Test
    fun `postTextStatus posts and caches a status that lasts 24 hours`() = runTest {
        every { remote.getCurrentUserId() } returns "me"
        val posted = slot<StatusDTO>()
        coEvery { remote.postStatus(capture(posted)) } returns Unit
        val cached = slot<StatusDBO>()

        repo.postTextStatus("hola", backgroundColor = 0xFF112233)

        assertEquals("me", posted.captured.userId)
        assertEquals("hola", posted.captured.text)
        assertEquals(0xFF112233, posted.captured.backgroundColor)
        coVerify { statusDao.upsert(capture(cached)) }
        assertEquals(24 * 60 * 60 * 1000L, cached.captured.expiresAt - cached.captured.createdAt)
        coVerify {
            analytics.logEvent(
                AnalyticsEvents.STATUS_POSTED,
                mapOf(AnalyticsEvents.PARAM_STATUS_TYPE to AnalyticsEvents.TYPE_TEXT),
            )
        }
    }

    @Test
    fun `postTextStatus does nothing when nobody is signed in`() = runTest {
        every { remote.getCurrentUserId() } returns null

        repo.postTextStatus("hola", 0L)

        coVerify(exactly = 0) { remote.postStatus(any()) }
        coVerify(exactly = 0) { analytics.logEvent(any(), any()) }
    }

    @Test
    fun `postImageStatus uploads first and stores the returned url`() = runTest {
        every { remote.getCurrentUserId() } returns "me"
        coEvery { remote.uploadStatusImage("me", any()) } returns "https://cdn/img.jpg"
        val posted = slot<StatusDTO>()
        coEvery { remote.postStatus(capture(posted)) } returns Unit

        repo.postImageStatus(byteArrayOf(1, 2, 3), text = "pie")

        assertEquals("https://cdn/img.jpg", posted.captured.imageUrl)
        assertEquals("pie", posted.captured.text)
        assertNull(posted.captured.videoUrl)
        coVerify {
            analytics.logEvent(
                AnalyticsEvents.STATUS_POSTED,
                mapOf(AnalyticsEvents.PARAM_STATUS_TYPE to AnalyticsEvents.TYPE_IMAGE),
            )
        }
    }

    @Test
    fun `postVideoStatus uploads first and stores the returned url`() = runTest {
        every { remote.getCurrentUserId() } returns "me"
        coEvery { remote.uploadStatusVideo("me", any()) } returns "https://cdn/v.mp4"
        val posted = slot<StatusDTO>()
        coEvery { remote.postStatus(capture(posted)) } returns Unit

        repo.postVideoStatus(byteArrayOf(9), text = null)

        assertEquals("https://cdn/v.mp4", posted.captured.videoUrl)
        assertNull(posted.captured.imageUrl)
        coVerify {
            analytics.logEvent(
                AnalyticsEvents.STATUS_POSTED,
                mapOf(AnalyticsEvents.PARAM_STATUS_TYPE to AnalyticsEvents.TYPE_VIDEO),
            )
        }
    }

    @Test
    fun `deleteStatus removes it remotely`() = runTest {
        repo.deleteStatus("s1")

        coVerify { remote.deleteStatus("s1") }
    }

    @Test
    fun `observeActiveStatuses joins each status with its author and flags the current user`() = runTest {
        every { remote.getCurrentUserId() } returns "me"
        every { remote.observeStatusChanges("me") } returns emptyFlow()
        every { statusDao.observeActive(any()) } returns flowOf(listOf(statusDbo("s1", "me"), statusDbo("s2", "u2")))
        coEvery { userDao.getById("me") } returns user("me", "Yo")
        coEvery { userDao.getById("u2") } returns user("u2", "Ana")

        val statuses = repo.observeActiveStatuses(listOf("u2")).first()

        assertEquals(listOf("Yo", "Ana"), statuses.map { it.userName })
        assertTrue(statuses[0].isFromMe)
        assertFalse(statuses[1].isFromMe)
    }

    @Test
    fun `observeActiveStatuses skips statuses whose author is not in the local database`() = runTest {
        every { remote.getCurrentUserId() } returns "me"
        every { remote.observeStatusChanges("me") } returns emptyFlow()
        every { statusDao.observeActive(any()) } returns flowOf(listOf(statusDbo("s1", "ghost"), statusDbo("s2", "u2")))
        coEvery { userDao.getById("ghost") } returns null
        coEvery { userDao.getById("u2") } returns user("u2", "Ana")

        val statuses = repo.observeActiveStatuses(emptyList()).first()

        assertEquals(listOf("s2"), statuses.map { it.id })
    }
}
