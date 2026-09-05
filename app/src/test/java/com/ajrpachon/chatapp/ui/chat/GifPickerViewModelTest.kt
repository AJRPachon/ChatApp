package com.ajrpachon.chatapp.ui.chat

import com.ajrpachon.chatapp.domain.model.GiphyGif
import com.ajrpachon.chatapp.domain.model.GiphySearchResult
import com.ajrpachon.chatapp.domain.repository.GiphyRepository
import com.ajrpachon.chatapp.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class GifPickerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val giphyRepository = mockk<GiphyRepository>()

    private val trendingGif = GiphyGif(previewUrl = "preview1", fullUrl = "full1")
    private val searchedGif = GiphyGif(previewUrl = "preview2", fullUrl = "full2")

    private fun buildViewModel(
        trendingResult: GiphySearchResult = GiphySearchResult.Success(listOf(trendingGif)),
    ): GifPickerViewModel {
        every { giphyRepository.getApiKey() } returns null
        coEvery { giphyRepository.search("") } returns trendingResult
        return GifPickerViewModel(giphyRepository)
    }

    @Test
    fun `loads trending gifs on init without a debounce delay`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals(listOf(trendingGif), vm.state.value.gifs)
        assertEquals(false, vm.state.value.isLoading)
        assertNull(vm.state.value.errorState)
    }

    @Test
    fun `QueryChanged debounces before searching`() = runTest(mainDispatcherRule.scheduler) {
        coEvery { giphyRepository.search("cat") } returns GiphySearchResult.Success(listOf(searchedGif))
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(GifPickerIntent.QueryChanged("cat"))
        advanceTimeBy(399)
        assertEquals(listOf(trendingGif), vm.state.value.gifs) // still the previous result, not yet searched

        advanceTimeBy(2)
        advanceUntilIdle()
        assertEquals(listOf(searchedGif), vm.state.value.gifs)
    }

    @Test
    fun `ApiKeyInvalid result surfaces API_KEY_INVALID error state`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel(trendingResult = GiphySearchResult.ApiKeyInvalid)
        advanceUntilIdle()

        assertEquals(GifPickerError.API_KEY_INVALID, vm.state.value.errorState)
        assertEquals(emptyList<GiphyGif>(), vm.state.value.gifs)
    }

    @Test
    fun `SaveApiKey persists the key and triggers a re-search`() = runTest(mainDispatcherRule.scheduler) {
        every { giphyRepository.setApiKey(any()) } returns Unit
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(GifPickerIntent.SaveApiKey("new-key"))
        advanceUntilIdle()

        verify { giphyRepository.setApiKey("new-key") }
        assertEquals(false, vm.state.value.showKeyDialog)
        assertEquals("new-key", vm.state.value.savedApiKey)
    }

    @Test
    fun `ShowKeyDialog and DismissKeyDialog toggle showKeyDialog`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(GifPickerIntent.ShowKeyDialog)
        assertEquals(true, vm.state.value.showKeyDialog)

        vm.onIntent(GifPickerIntent.DismissKeyDialog)
        assertEquals(false, vm.state.value.showKeyDialog)
    }
}
