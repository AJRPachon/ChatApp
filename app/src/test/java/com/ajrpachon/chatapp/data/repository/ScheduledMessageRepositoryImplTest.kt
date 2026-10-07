package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.local.dao.ScheduledMessageDao
import com.ajrpachon.chatapp.data.local.entity.ScheduledMessageDBO
import com.ajrpachon.chatapp.domain.model.ScheduledMessageBO
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduledMessageRepositoryImplTest {

    private val dao = mockk<ScheduledMessageDao>(relaxed = true)
    private val repo = ScheduledMessageRepositoryImpl(dao)

    private val dbo = ScheduledMessageDBO("id1", "c1", "me", "hola", scheduledAtMs = 100L, createdAt = 50L)
    private val bo = ScheduledMessageBO("id1", "c1", "me", "hola", scheduledAtMs = 100L, createdAt = 50L)

    @Test
    fun `schedule inserts a row with the given fields`() = runTest {
        repo.schedule("id1", "c1", "me", "hola", scheduledAtMs = 100L, createdAt = 50L)

        coVerify { dao.insert(dbo) }
    }

    @Test
    fun `observeAll maps rows to domain models`() = runTest {
        every { dao.observeAll() } returns flowOf(listOf(dbo))

        assertEquals(listOf(bo), repo.observeAll().first())
    }

    @Test
    fun `getPending asks the dao with the given time and maps the rows`() = runTest {
        coEvery { dao.getPending(1_000L) } returns listOf(dbo)

        assertEquals(listOf(bo), repo.getPending(1_000L))
    }

    @Test
    fun `deleteById is forwarded`() = runTest {
        repo.deleteById("id1")

        coVerify { dao.deleteById("id1") }
    }
}
