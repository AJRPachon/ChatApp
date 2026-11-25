package com.ajrpachon.chatapp.data.repository

import androidx.paging.PagingConfig
import androidx.paging.PagingSource.LoadResult
import androidx.paging.PagingState
import com.ajrpachon.chatapp.domain.model.MessageBO
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MessageRefreshKeyTest {

    private val config = PagingConfig(pageSize = 30, enablePlaceholders = false)

    private fun firstPageState(anchor: Int?): PagingState<Int, MessageBO> = PagingState(
        pages = listOf(
            LoadResult.Page(
                data = List(30) { mockk<MessageBO>(relaxed = true) },
                prevKey = null,
                nextKey = 30,
            ),
        ),
        anchorPosition = anchor,
        config = config,
        leadingPlaceholderCount = 0,
    )

    @Test
    fun `at the newest message the refresh starts at offset 0 so new messages stay visible`() {
        assertEquals(0, messageRefreshKey(firstPageState(anchor = 0)))
    }

    @Test
    fun `deep in the history the refresh is centred on the anchor`() {
        // initialLoadSize defaults to 3 * pageSize = 90 -> start 45 items before the anchor
        assertEquals(55, messageRefreshKey(firstPageState(anchor = 100)))
    }

    @Test
    fun `no anchor means no refresh key`() {
        assertNull(messageRefreshKey(firstPageState(anchor = null)))
    }
}
