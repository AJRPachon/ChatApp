package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.local.dao.SessionDao
import com.ajrpachon.chatapp.data.local.entity.SessionDBO
import com.ajrpachon.chatapp.domain.model.SessionBO
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionRepositoryImplTest {

    private val dao = mockk<SessionDao>(relaxed = true)
    private val repo = SessionRepositoryImpl(dao)

    @Test
    fun `observeAll maps every row to a session`() = runTest {
        every { dao.observeAll() } returns flowOf(
            listOf(
                SessionDBO("s1", "Pixel 9", createdAt = 1L, lastActiveAt = 2L, isCurrent = true),
                SessionDBO("s2", "Tablet", createdAt = 3L, lastActiveAt = 4L, isCurrent = false),
            ),
        )

        val sessions = repo.observeAll().first()

        assertEquals(
            listOf(SessionBO("s1", "Pixel 9", 1L, 2L, true), SessionBO("s2", "Tablet", 3L, 4L, false)),
            sessions,
        )
    }

    @Test
    fun `upsert stores the session as an entity`() = runTest {
        repo.upsert(SessionBO("s1", "Pixel 9", 1L, 2L, true))

        coVerify { dao.upsert(SessionDBO("s1", "Pixel 9", 1L, 2L, true)) }
    }

    @Test
    fun `the remaining operations are forwarded to the dao`() = runTest {
        repo.updateCurrentLastActive(99L)
        repo.delete("s1")
        repo.deleteAll()
        repo.deleteAllOthers()

        coVerify { dao.updateCurrentLastActive(99L) }
        coVerify { dao.delete("s1") }
        coVerify { dao.deleteAll() }
        coVerify { dao.deleteAllOthers() }
    }
}
