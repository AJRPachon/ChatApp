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
 * One [TestCoroutineScheduler] per rule instance (i.e. per test method — JUnit4
 * builds a fresh test class instance per @Test) so that `viewModelScope` delays
 * advance together with the test's own `runTest(rule.scheduler) { ... }` scope.
 *
 * This used to be a top-level `val sharedScheduler`, reused by every test class
 * in the whole JVM test run. An uncaught exception left on that scheduler by one
 * test's leaked/still-running coroutine (e.g. a ViewModel's viewModelScope job
 * that outlives the test) would then surface as a spurious
 * `UncaughtExceptionsBeforeTest` failure on a *later, unrelated* test class that
 * happened to share the same JVM worker — confirmed causing intermittent CI
 * failures across unrelated ViewModel tests. Scoping the scheduler per rule
 * instance isolates each test's coroutines from every other test's.
 */
class MainDispatcherRule(
    val scheduler: TestCoroutineScheduler = TestCoroutineScheduler(),
    val testDispatcher: TestDispatcher = StandardTestDispatcher(scheduler),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(testDispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
