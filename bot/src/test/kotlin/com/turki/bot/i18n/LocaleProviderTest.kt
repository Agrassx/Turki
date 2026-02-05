package com.turki.bot.i18n

import com.turki.core.domain.Language
import kotlin.test.Test
import kotlin.test.assertSame

class LocaleProviderTest {
    @Test
    fun `returns RussianStrings for all supported languages`() {
        assertSame(RussianStrings, LocaleProvider.get(Language.RUSSIAN))
        assertSame(RussianStrings, LocaleProvider.get(Language.TURKISH))
        assertSame(RussianStrings, LocaleProvider.get(Language.ENGLISH))
    }
}
