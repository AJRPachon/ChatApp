package com.ajrpachon.chatapp.ui.components

import com.ajrpachon.chatapp.domain.model.EmojiCategory
import com.ajrpachon.chatapp.domain.repository.EmojiRepository
import com.ajrpachon.chatapp.util.MainDispatcherRule
import com.ajrpachon.chatapp.util.sharedScheduler
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EmojiPickerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val emojiRepository = mockk<EmojiRepository>()

    private val recentCategory = EmojiCategory(category = "Recientes", icon = "🕒", emojis = emptyList())
    private val smileysCategory = EmojiCategory(category = "Caritas", icon = "😀", emojis = listOf("😀", "😁"))

    private fun buildViewModel(): EmojiPickerViewModel {
        coEvery { emojiRepository.getCategories() } returns listOf(recentCategory, smileysCategory)
        return EmojiPickerViewModel(emojiRepository)
    }

    @Test
    fun `recent category is filled with recent emojis when present`() = runTest(sharedScheduler) {
        every { emojiRepository.getRecent() } returns listOf("😂", "❤️")
        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals(2, vm.state.value.categories.size)
        assertEquals(listOf("😂", "❤️"), vm.state.value.categories[0].emojis)
    }

    @Test
    fun `recent category slot is removed when there are no recent emojis`() = runTest(sharedScheduler) {
        every { emojiRepository.getRecent() } returns emptyList()
        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals(listOf(smileysCategory), vm.state.value.categories)
    }

    @Test
    fun `SelectTab updates selectedTab in state`() = runTest(sharedScheduler) {
        every { emojiRepository.getRecent() } returns emptyList()
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(EmojiPickerIntent.SelectTab(1))

        assertEquals(1, vm.state.value.selectedTab)
    }

    @Test
    fun `EmojiClicked records usage and emits EmojiChosen effect`() = runTest(sharedScheduler) {
        every { emojiRepository.getRecent() } returns emptyList()
        every { emojiRepository.recordUsed(any()) } returns Unit
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(EmojiPickerIntent.EmojiClicked("😀"))
        advanceUntilIdle()

        verify { emojiRepository.recordUsed("😀") }
        val effect = vm.effect.first()
        assertTrue(effect is EmojiPickerEffect.EmojiChosen)
        assertEquals("😀", (effect as EmojiPickerEffect.EmojiChosen).emoji)
    }
}
