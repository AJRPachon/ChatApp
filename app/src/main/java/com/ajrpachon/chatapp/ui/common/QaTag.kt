package com.ajrpachon.chatapp.ui.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

/**
 * testTag that is also exposed as the Android resource-id for UI automation (Maestro).
 *
 * MainActivity enables `testTagsAsResourceId` on the root of the main window, but dialogs,
 * dropdown menus and bottom sheets render in their own window and don't inherit it — a plain
 * testTag inside them is invisible to Maestro's `id:` selector. Use this for anything that
 * lives inside a popup; it is harmless (and equivalent to testTag) everywhere else.
 */
fun Modifier.qaTag(tag: String): Modifier =
    this
        .semantics { testTagsAsResourceId = true }
        .testTag(tag)
