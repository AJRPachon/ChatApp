package com.ajrpachon.chatapp.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * The coroutine dispatchers a class needs to move work off the main thread, wrapped in one type so
 * Koin can inject them (a bare `CoroutineDispatcher` is too generic a type to register) and tests can
 * substitute a `TestDispatcher` by constructing `AppDispatchers(testDispatcher)`.
 */
class AppDispatchers(
    val default: CoroutineDispatcher = Dispatchers.Default,
)
