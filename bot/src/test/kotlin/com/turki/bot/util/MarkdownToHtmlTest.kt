package com.turki.bot.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownToHtmlTest {
    @Test
    fun `converts headers bold italic and lists`() {
        val input = """
            # Title
            **Bold text** and *italic text*
            - Item one
            - Item two
        """.trimIndent()

        val html = input.markdownToHtml()

        assertTrue(html.contains("<b>Title</b>"))
        assertTrue(html.contains("<b>Bold text</b> and <i>italic text</i>"))
        assertTrue(html.contains("• Item one"))
        assertTrue(html.contains("• Item two"))
    }

    @Test
    fun `converts markdown tables to readable lines`() {
        val input = """
            | Турецкий | Произношение | Русский |
            |---|---|---|
            | Merhaba | mer-ha-ba | Привет |
        """.trimIndent()

        val html = input.markdownToHtml()
        assertEquals("<b>Merhaba</b> [mer-ha-ba] — Привет", html.trim())
    }
}
