package com.ajrpachon.chatapp.ui.common

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.ajrpachon.chatapp.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class UiTextTest {

    private val context: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun `dynamic text resolves to itself`() {
        assertEquals("hola", UiText.Dynamic("hola").asString(context))
    }

    @Test
    fun `string resource resolves through the context`() {
        assertEquals(context.getString(R.string.error_generic), UiText.StringResource(R.string.error_generic).asString(context))
    }

    @Test
    fun `string resource arguments are formatted in order`() {
        val text = UiText.of(R.string.broadcast_sent_summary_toast, 2, 3)

        assertEquals(context.getString(R.string.broadcast_sent_summary_toast, 2, 3), text.asString(context))
    }

    @Test
    fun `a throwable with a message becomes dynamic text`() {
        assertEquals(UiText.Dynamic("boom"), IllegalStateException("boom").toUiText())
    }

    @Test
    fun `a throwable without a usable message falls back to the given resource`() {
        assertEquals(UiText.StringResource(R.string.error_generic), IllegalStateException().toUiText())
        assertEquals(UiText.StringResource(R.string.error_generic), IllegalStateException("  ").toUiText())
        assertEquals(
            UiText.StringResource(R.string.usagestats_error_load),
            IllegalStateException().toUiText(R.string.usagestats_error_load),
        )
    }
}
