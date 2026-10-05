package com.ajrpachon.chatapp.ui.backup

import com.ajrpachon.chatapp.domain.model.BackupInfoBO
import com.ajrpachon.chatapp.domain.repository.BackupRepository
import com.ajrpachon.chatapp.ui.common.UiText
import com.ajrpachon.chatapp.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class BackupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<BackupRepository>(relaxed = true)
    private val info = BackupInfoBO(lastBackupDate = "05/10/2026", backupSizeMb = "12.5", fileId = "f1")

    @Before
    fun setUp() {
        coEvery { repository.getLatestBackupInfo() } returns null
    }

    private fun buildViewModel() = BackupViewModel(repository)

    @Test
    fun `the latest backup info is shown on start`() = runTest(mainDispatcherRule.scheduler) {
        coEvery { repository.getLatestBackupInfo() } returns info

        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals("05/10/2026", vm.state.value.lastBackupDate)
        assertEquals("12.5", vm.state.value.backupSizeMb)
    }

    @Test
    fun `no previous backup leaves the info empty`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()

        assertNull(vm.state.value.lastBackupDate)
        assertNull(vm.state.value.backupSizeMb)
    }

    @Test
    fun `a failure reading the latest info is ignored`() = runTest(mainDispatcherRule.scheduler) {
        coEvery { repository.getLatestBackupInfo() } throws IllegalStateException("no drive")

        val vm = buildViewModel()
        advanceUntilIdle()

        assertNull(vm.state.value.error)
    }

    @Test
    fun `a successful backup updates the info and shows a message`() = runTest(mainDispatcherRule.scheduler) {
        coEvery { repository.backup() } returns info
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(BackupIntent.StartBackup)
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isBackingUp)
        assertEquals("05/10/2026", state.lastBackupDate)
        assertNotNull(state.successMessage)
        assertNull(state.error)
    }

    @Test
    fun `a failing backup shows the error and stops the spinner`() = runTest(mainDispatcherRule.scheduler) {
        coEvery { repository.backup() } throws IllegalStateException("quota exceeded")
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(BackupIntent.StartBackup)
        advanceUntilIdle()

        assertEquals(UiText.Dynamic("quota exceeded"), vm.state.value.error)
        assertFalse(vm.state.value.isBackingUp)
        assertNull(vm.state.value.successMessage)
    }

    @Test
    fun `a successful restore shows a message`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(BackupIntent.StartRestore)
        advanceUntilIdle()

        coVerify { repository.restore() }
        assertFalse(vm.state.value.isRestoring)
        assertNotNull(vm.state.value.successMessage)
    }

    @Test
    fun `a failing restore shows the error and stops the spinner`() = runTest(mainDispatcherRule.scheduler) {
        coEvery { repository.restore() } throws IllegalStateException("no backup found")
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(BackupIntent.StartRestore)
        advanceUntilIdle()

        assertEquals(UiText.Dynamic("no backup found"), vm.state.value.error)
        assertFalse(vm.state.value.isRestoring)
    }

    @Test
    fun `a second backup request while one is running is ignored`() = runTest(mainDispatcherRule.scheduler) {
        val gate = CompletableDeferred<BackupInfoBO>()
        coEvery { repository.backup() } coAnswers { gate.await() }
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(BackupIntent.StartBackup)
        advanceUntilIdle()
        assertTrue(vm.state.value.isBackingUp)
        vm.onIntent(BackupIntent.StartBackup)
        vm.onIntent(BackupIntent.StartRestore)
        gate.complete(info)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.backup() }
        coVerify(exactly = 0) { repository.restore() }
    }

    @Test
    fun `dismissing clears the error and the success message`() = runTest(mainDispatcherRule.scheduler) {
        coEvery { repository.backup() } throws IllegalStateException("boom")
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(BackupIntent.StartBackup)
        advanceUntilIdle()

        vm.onIntent(BackupIntent.DismissError)
        assertNull(vm.state.value.error)

        coEvery { repository.backup() } returns info
        vm.onIntent(BackupIntent.StartBackup)
        advanceUntilIdle()
        assertNotNull(vm.state.value.successMessage)
        vm.onIntent(BackupIntent.DismissSuccess)
        assertNull(vm.state.value.successMessage)
    }
}
