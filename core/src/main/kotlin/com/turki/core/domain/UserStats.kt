package com.turki.core.domain

import kotlinx.datetime.Instant

data class UserStats(
    val userId: Long,
    val currentStreak: Int,
    val lastActiveAt: Instant?,
    val weeklyLessons: Int,
    val weeklyPractice: Int,
    val weeklyReview: Int,
    val weeklyHomework: Int,
    val lastWeeklyReportAt: Instant?
)
