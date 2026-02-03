package com.turki.core.domain

import kotlinx.datetime.Instant

data class UserProgress(
    val userId: Long,
    val lessonId: Int,
    val status: String,
    val lastExerciseId: Int?,
    val contentVersion: String,
    val updatedAt: Instant
)
