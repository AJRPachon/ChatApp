package com.ajrpachon.chatapp.ui.common

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ajrpachon.chatapp.R

/**
 * A piece of user-visible text a ViewModel can hold without touching Android resources.
 *
 * ViewModels put [StringResource] (a string from `res/values`) or [Dynamic] (text that came from
 * somewhere else, such as an exception message) in their State and Effects; the Screen resolves it
 * with [asString] when it draws. That keeps hard-coded Spanish out of the ViewModels and lets the
 * text follow the device language.
 */
sealed interface UiText {

    data class Dynamic(val value: String) : UiText

    data class StringResource(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    /** Resolves the text inside a composition. */
    @Composable
    fun asString(): String = when (this) {
        is Dynamic -> value
        is StringResource -> stringResource(id, *args.toTypedArray())
    }

    /** Resolves the text outside a composition, for example from a `LaunchedEffect` collecting effects. */
    fun asString(context: Context): String = when (this) {
        is Dynamic -> value
        is StringResource -> context.getString(id, *args.toTypedArray())
    }

    companion object {
        fun of(@StringRes id: Int, vararg args: Any): UiText = StringResource(id, args.toList())
    }
}

/**
 * The text to show for a failure: the exception message when it has one, otherwise the string
 * resource [fallback].
 */
fun Throwable.toUiText(@StringRes fallback: Int = R.string.error_generic): UiText =
    message?.takeIf { it.isNotBlank() }?.let(UiText::Dynamic) ?: UiText.StringResource(fallback)
