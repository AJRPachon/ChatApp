package com.ajrpachon.chatapp.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.catchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base ViewModel that provides the standard MVI scaffolding:
 * - [state]: observable UI state backed by [MutableStateFlow]
 * - [effect]: one-shot side-effects backed by a buffered [Channel]
 *
 * Subclasses define [State] and [Effect] types. Use [updateState] and
 * [sendEffect] helpers instead of accessing the backing properties directly.
 *
 * Also provides [launchCatching] — a coroutine builder that wraps the body
 * in [catchResult] and logs failures with the given [tag].
 */
abstract class BaseViewModel<State, Effect>(initialState: State) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state = _state.asStateFlow()

    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    protected fun updateState(block: (State) -> State) = _state.update(block)

    protected fun sendEffect(effect: Effect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    /**
     * Launches a coroutine in [viewModelScope] whose body is wrapped in [catchResult].
     * Failures are logged at error level with [tag] + [context] and passed to [onError].
     *
     * @param tag     Log tag, typically the ViewModel class name.
     * @param context Short description of the operation for the log message.
     * @param onError Optional callback invoked with the caught [Throwable].
     * @param block   The suspending work to execute.
     */
    protected fun launchCatching(
        tag: String,
        context: String = "",
        onError: ((Throwable) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit,
    ) {
        viewModelScope.launch {
            catchResult { block() }
                .onFailure { e ->
                    if (context.isNotBlank()) {
                        AppLogger.e(tag, "$context failed", e)
                    }
                    onError?.invoke(e)
                }
        }
    }
}
