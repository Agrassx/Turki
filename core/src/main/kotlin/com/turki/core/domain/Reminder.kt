package com.turki.core.domain

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Reminder(
    val id: Long,
    val userId: Long,
    val type: ReminderType,
    val scheduledAt: Instant,
    val sent: Boolean = false,
    val sentAt: Instant? = null
)

enum class ReminderType {
    LESSON_REMINDER,
    HOMEWORK_REMINDER,
    SUBSCRIPTION_EXPIRING
}
