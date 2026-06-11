package com.ajrpachon.chatapp.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Shares the same [TestCoroutineScheduler] with [runTest] so that
 * viewModelScope delays advance together with the test scope.
 */
val sharedScheduler = TestCoroutineScheduler()

class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher(sharedScheduler),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(testDispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
