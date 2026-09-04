package com.ajrpachon.chatapp.ui.chat

/**
 * Bundles a per-concern delegate's only hooks into the owning ChatViewModel: read the current
 * [ChatState], write a new one via the same `.copy()` pattern every intent handler uses, and
 * emit a one-shot [ChatEffect]. Introduced when a fourth delegate constructor (individually
 * taking `getState`/`updateState`/`sendEffect` alongside its own repository deps) tripped
 * detekt's LongParameterList threshold — bundling these three keeps each delegate's param count
 * down to its actual dependencies. See docs/chat-viewmodel-decomposition.md.
 */
class ChatDelegateContext(
    val getState: () -> ChatState,
    val updateState: ((ChatState) -> ChatState) -> Unit,
    val sendEffect: (ChatEffect) -> Unit,
)
