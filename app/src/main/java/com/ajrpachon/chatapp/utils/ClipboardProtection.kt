package com.ajrpachon.chatapp.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object ClipboardProtection {
    private const val CLEAR_DELAY_MS = 60_000L // 1 minute

    fun copyWithTimeout(context: Context, label: String, text: String, scope: CoroutineScope) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        scope.launch(Dispatchers.Main) {
            delay(CLEAR_DELAY_MS)
            // Only clear if our content is still there
            if (clipboard.primaryClip?.getItemAt(0)?.text?.toString() == text) {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }
    }
}
