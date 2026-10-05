package com.ajrpachon.chatapp.ui.chat

import app.cash.turbine.test
import com.ajrpachon.chatapp.domain.model.StickerBO
import com.ajrpachon.chatapp.domain.model.StickerPackBO
import com.ajrpachon.chatapp.domain.repository.StickerPackRepository
import com.ajrpachon.chatapp.util.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class StickerPackViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<StickerPackRepository>(relaxed = true)
    private val installed = MutableStateFlow<List<StickerPackBO>>(emptyList())
    private val available = MutableStateFlow<List<StickerPackBO>>(emptyList())

    private fun pack(id: String, installed: Boolean) = StickerPackBO(id, "Pack $id", "https://x/$id.png", installed)

    @Before
    fun setUp() {
        every { repository.getInstalledPacks() } returns installed
        every { repository.getAvailablePacks() } returns available
    }

    private fun buildViewModel() = StickerPackViewModel(repository)

    @Test
    fun `installed and available packs follow the repository flows`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        assertEquals(emptyList<StickerPackBO>(), vm.state.value.installedPacks)

        installed.value = listOf(pack("a", true))
        available.value = listOf(pack("b", false), pack("c", false))
        advanceUntilIdle()

        assertEquals(listOf("a"), vm.state.value.installedPacks.map { it.id })
        assertEquals(listOf("b", "c"), vm.state.value.availablePacks.map { it.id })
    }

    @Test
    fun `selecting a pack stores its id`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        assertNull(vm.state.value.selectedPackId)

        vm.onIntent(StickerPackIntent.SelectPack("a"))

        assertEquals("a", vm.state.value.selectedPackId)
    }

    @Test
    fun `installing a pack asks the repository`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(StickerPackIntent.InstallPack("b"))
        advanceUntilIdle()

        coVerify { repository.installPack("b") }
    }

    @Test
    fun `stickersForPack exposes the stickers of that pack`() = runTest(mainDispatcherRule.scheduler) {
        val stickers = MutableStateFlow(listOf(StickerBO("s1", "a", "https://x/1.png", "")))
        every { repository.getStickersForPack("a") } returns stickers
        val vm = buildViewModel()

        vm.stickersForPack("a").test {
            assertEquals(emptyList<StickerBO>(), awaitItem())
            assertEquals(listOf("s1"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }
}
