package com.ajrpachon.chatapp.ui.chat

import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.domain.model.GiphySearchResult
import com.ajrpachon.chatapp.domain.repository.GiphyRepository
import com.ajrpachon.chatapp.ui.common.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class GifPickerViewModel(
    private val giphyRepository: GiphyRepository,
) : BaseViewModel<GifPickerState, Nothing>(GifPickerState()) {

    // Bumped after a new API key is saved to force a re-search, mirroring the
    // previous `LaunchedEffect(query, refresh)` behavior in the Composable.
    private val refreshTrigger = MutableStateFlow(0)

    init {
        updateState { it.copy(savedApiKey = giphyRepository.getApiKey().orEmpty()) }
        viewModelScope.launch {
            combine(
                state.map { it.query }.distinctUntilChanged(),
                refreshTrigger,
            ) { query, _ -> query }
                .flatMapLatest { query ->
                    flow {
                        emit(SearchOutcome.Loading)
                        if (query.isNotBlank()) delay(SEARCH_DEBOUNCE_MS)
                        emit(SearchOutcome.Loaded(giphyRepository.search(query)))
                    }
                }
                .collect { outcome ->
                    when (outcome) {
                        is SearchOutcome.Loading -> updateState { it.copy(isLoading = true) }
                        is SearchOutcome.Loaded -> applySearchResult(outcome.result)
                    }
                }
        }
    }

    fun onIntent(intent: GifPickerIntent) {
        when (intent) {
            is GifPickerIntent.QueryChanged -> updateState { it.copy(query = intent.query) }
            is GifPickerIntent.SaveApiKey -> {
                giphyRepository.setApiKey(intent.key)
                updateState { it.copy(showKeyDialog = false, savedApiKey = intent.key.trim()) }
                refreshTrigger.update { count -> count + 1 }
            }
            is GifPickerIntent.ShowKeyDialog -> updateState { it.copy(showKeyDialog = true) }
            is GifPickerIntent.DismissKeyDialog -> updateState { it.copy(showKeyDialog = false) }
        }
    }

    private fun applySearchResult(result: GiphySearchResult) {
        when (result) {
            is GiphySearchResult.Success -> updateState {
                it.copy(gifs = result.gifs, isLoading = false, errorState = null)
            }

            GiphySearchResult.ApiKeyInvalid -> updateState {
                it.copy(gifs = emptyList(), isLoading = false, errorState = GifPickerError.API_KEY_INVALID)
            }

            GiphySearchResult.NetworkError -> updateState {
                it.copy(gifs = emptyList(), isLoading = false, errorState = GifPickerError.NETWORK_ERROR)
            }
        }
    }

    private sealed interface SearchOutcome {
        data object Loading : SearchOutcome
        data class Loaded(val result: GiphySearchResult) : SearchOutcome
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 400L
    }
}
