package com.turki.core.domain

import kotlinx.datetime.Instant

data class ReminderPreference(
    val userId: Long,
    val daysOfWeek: String,
    val timeLocal: String,
    val isEnabled: Boolean,
    val lastFiredAt: Instant?
)
