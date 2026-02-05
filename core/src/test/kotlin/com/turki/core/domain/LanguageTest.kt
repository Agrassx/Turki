package com.turki.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LanguageTest {
    @Test
    fun `fromCode returns matching language`() {
        assertEquals(Language.TURKISH, Language.fromCode("tr"))
        assertEquals(Language.RUSSIAN, Language.fromCode("ru"))
        assertEquals(Language.ENGLISH, Language.fromCode("en"))
    }

    @Test
    fun `fromCode returns null for unknown`() {
        assertNull(Language.fromCode("xx"))
    }
}
