package com.ajrpachon.chatapp.ui.applock

import com.ajrpachon.chatapp.util.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class AppLockViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `starts without an error`() {
        assertNull(AppLockViewModel().state.value.errorMessage)
    }

    @Test
    fun `an authentication error keeps its message`() {
        val vm = AppLockViewModel()

        vm.onIntent(AppLockIntent.AuthError("Demasiados intentos"))

        assertEquals("Demasiados intentos", vm.state.value.errorMessage)
    }

    @Test
    fun `a failed attempt shows an error`() {
        val vm = AppLockViewModel()

        vm.onIntent(AppLockIntent.AuthFailed)

        assertNotNull(vm.state.value.errorMessage)
    }

    @Test
    fun `clearing removes the error`() {
        val vm = AppLockViewModel()
        vm.onIntent(AppLockIntent.AuthFailed)

        vm.onIntent(AppLockIntent.ClearError)

        assertNull(vm.state.value.errorMessage)
    }

    @Test
    fun `a successful authentication clears the error and emits Authenticated`() =
        runTest(mainDispatcherRule.scheduler) {
            val vm = AppLockViewModel()
            vm.onIntent(AppLockIntent.AuthFailed)

            vm.onIntent(AppLockIntent.AuthSucceeded)
            advanceUntilIdle()

            assertNull(vm.state.value.errorMessage)
            assertEquals(AppLockEffect.Authenticated, vm.effect.first())
        }

    @Test
    fun `requestBiometric emits LaunchBiometric`() = runTest(mainDispatcherRule.scheduler) {
        val vm = AppLockViewModel()

        vm.requestBiometric()
        advanceUntilIdle()

        assertEquals(AppLockEffect.LaunchBiometric, vm.effect.first())
    }
}
