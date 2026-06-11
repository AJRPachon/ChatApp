package com.ajrpachon.chatapp.utils

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatchResultTest {

    @Test
    fun `catchResult returns success for successful block`() {
        val result = catchResult { 42 }
        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `catchResult returns failure for non-cancellation exception`() {
        val result = catchResult { throw RuntimeException("boom") }
        assertTrue(result.isFailure)
        assertEquals("boom", result.exceptionOrNull()?.message)
    }

    @Test(expected = CancellationException::class)
    fun `catchResult re-throws CancellationException`() {
        catchResult { throw CancellationException("cancelled") }
    }

    @Test(expected = CancellationException::class)
    fun `catchResult re-throws CancellationException subclasses`() {
        class CustomCancellation : CancellationException("custom")
        catchResult { throw CustomCancellation() }
    }
}
