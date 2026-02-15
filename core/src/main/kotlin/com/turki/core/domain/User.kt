package com.turki.core.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Long,
    val telegramId: Long,
    val username: String?,
    val firstName: String,
    val lastName: String?,
    val language: Language = Language.RUSSIAN,
    val timezone: String = "Europe/Moscow",
    val subscriptionActive: Boolean = false,
    val subscriptionExpiresAt: Instant? = null,
    val currentLessonId: Int = 1,
    val createdAt: Instant,
    val updatedAt: Instant
)
