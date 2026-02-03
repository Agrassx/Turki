package com.turki.core.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class UserCustomDictionaryEntry(
    val id: Long,
    val userId: Long,
    val word: String,
    val translation: String,
    val pronunciation: String? = null,
    val example: String? = null,
    val addedAt: Instant
)
