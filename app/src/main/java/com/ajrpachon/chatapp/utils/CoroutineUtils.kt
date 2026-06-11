package com.ajrpachon.chatapp.utils

import kotlinx.coroutines.CancellationException

/**
 * Like [runCatching] but re-throws [CancellationException] so coroutine cancellation
 * propagates correctly through structured concurrency.
 * Use this instead of [runCatching] inside suspend functions or coroutine blocks.
 */
inline fun <T> catchResult(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
