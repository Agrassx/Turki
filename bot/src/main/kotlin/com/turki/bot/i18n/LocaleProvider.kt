package com.turki.bot.i18n

import com.turki.core.domain.Language

/**
 * Provider for localized strings based on user language.
 *
 * This object manages the mapping between [Language] and [Strings] implementations.
 * Currently, all languages use [RussianStrings] as the default implementation.
 * Future implementations can add TurkishStrings, EnglishStrings, etc.
 *
 * The provider always returns a valid [Strings] implementation, defaulting to
 * [RussianStrings] if the requested language is not available.
 */
object LocaleProvider {

    private val locales = mapOf(
        Language.RUSSIAN to RussianStrings,
        Language.TURKISH to RussianStrings,
        Language.ENGLISH to RussianStrings
    )

    /**
     * Gets the [Strings] implementation for the specified language.
     *
     * @param language The target language (default: [Language.RUSSIAN])
     * @return The [Strings] implementation for the language, or [RussianStrings] as fallback
     */
    fun get(language: Language = Language.RUSSIAN): Strings {
        return locales[language] ?: RussianStrings
    }

    /**
     * Default localization (Russian).
     */
    val default: Strings = RussianStrings
}

/**
 * Convenience property for accessing the default localization.
 *
 * This provides a shorthand way to access localized strings:
 * ```
 * S.welcome("John")
 * S.btnStartLesson
 * ```
 */
val S: Strings get() = LocaleProvider.default
