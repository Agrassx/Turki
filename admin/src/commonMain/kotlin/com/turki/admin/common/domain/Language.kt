package com.turki.admin.common.domain

import kotlinx.serialization.Serializable

@Serializable
enum class Language(val code: String, val displayName: String) {
    TURKISH("tr", "Türkçe"),
    ENGLISH("en", "English"),
    RUSSIAN("ru", "Русский");

    companion object {
        fun fromCode(code: String): Language? = entries.find { it.code == code }
    }
}
