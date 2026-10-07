package com.ajrpachon.chatapp.ui.common

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every string must exist in both languages, and a format placeholder in one must appear in the
 * other. A key missing from `values-en` silently shows Spanish on an English device.
 */
class StringResourcesParityTest {

    private val resDir = listOf(File("src/main/res"), File("app/src/main/res")).first { it.exists() }

    private val keyPattern = Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
    private val placeholderPattern = Regex("""%\d+\$[sd]|%[sd]""")

    private fun strings(dir: String): Map<String, String> =
        keyPattern.findAll(File(resDir, "$dir/strings.xml").readText())
            .associate { it.groupValues[1] to it.groupValues[2] }

    @Test
    fun `values and values-en define the same keys`() {
        val es = strings("values").keys
        val en = strings("values-en").keys

        assertEquals("only in values: ${es - en}", emptySet<String>(), es - en)
        assertEquals("only in values-en: ${en - es}", emptySet<String>(), en - es)
    }

    @Test
    fun `both languages use the same format placeholders`() {
        val es = strings("values")
        val en = strings("values-en")

        val mismatched = es.keys.intersect(en.keys).filter { key ->
            placeholderPattern.findAll(es.getValue(key)).map { it.value }.sorted().toList() !=
                placeholderPattern.findAll(en.getValue(key)).map { it.value }.sorted().toList()
        }

        assertTrue("placeholders differ for: $mismatched", mismatched.isEmpty())
    }
}
