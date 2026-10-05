package com.ajrpachon.chatapp.ui.common

import androidx.annotation.CheckResult
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * `dropUnlessResumed` for callbacks that carry arguments.
 *
 * `androidx.lifecycle.compose.dropUnlessResumed` only wraps `() -> Unit`. These overloads do the same
 * for one to four arguments, so a click handler like `onOpenPdf(url, filename)` can be guarded
 * against firing while its screen is mid-transition (the double-tap that pushes the same route twice).
 *
 * Use them only for callbacks triggered directly by a user tap. A callback fired from a ViewModel
 * `Effect` (after a network call, say) must not be guarded: if the screen is paused when the effect
 * arrives, the navigation would be silently dropped and never retried.
 */
@CheckResult
@Composable
fun <A> dropUnlessResumed(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    block: (A) -> Unit,
): (A) -> Unit = { a -> if (lifecycleOwner.isResumed()) block(a) }

@CheckResult
@Composable
fun <A, B> dropUnlessResumed(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    block: (A, B) -> Unit,
): (A, B) -> Unit = { a, b -> if (lifecycleOwner.isResumed()) block(a, b) }

@CheckResult
@Composable
fun <A, B, C> dropUnlessResumed(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    block: (A, B, C) -> Unit,
): (A, B, C) -> Unit = { a, b, c -> if (lifecycleOwner.isResumed()) block(a, b, c) }

@CheckResult
@Composable
fun <A, B, C, D> dropUnlessResumed(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    block: (A, B, C, D) -> Unit,
): (A, B, C, D) -> Unit = { a, b, c, d -> if (lifecycleOwner.isResumed()) block(a, b, c, d) }

private fun LifecycleOwner.isResumed() = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
