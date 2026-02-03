package com.turki.core.domain

import kotlinx.datetime.Instant

data class UserDictionaryEntry(
    val userId: Long,
    val vocabularyId: Int,
    val isFavorite: Boolean,
    val tags: String,
    val addedAt: Instant
)
