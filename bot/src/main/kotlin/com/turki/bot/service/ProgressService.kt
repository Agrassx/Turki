package com.turki.bot.service

import com.turki.core.domain.UserProgress
import com.turki.core.domain.UserStats
import com.turki.core.repository.UserProgressRepository
import com.turki.core.repository.UserStatsRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.minus

class ProgressService(
    private val userProgressRepository: UserProgressRepository,
    private val userStatsRepository: UserStatsRepository,
    private val lessonService: LessonService
) {
    suspend fun markLessonStarted(userId: Long, lessonId: Int, contentVersion: String = "v1") {
        userProgressRepository.upsert(
            UserProgress(
                userId = userId,
                lessonId = lessonId,
                status = "IN_PROGRESS",
                lastExerciseId = null,
                contentVersion = contentVersion,
                updatedAt = Clock.System.now()
            )
        )
        touchStats(userId) { it }
    }

    suspend fun markLessonCompleted(userId: Long, lessonId: Int, contentVersion: String = "v1") {
        userProgressRepository.upsert(
            UserProgress(
                userId = userId,
                lessonId = lessonId,
                status = "COMPLETED",
                lastExerciseId = null,
                contentVersion = contentVersion,
                updatedAt = Clock.System.now()
            )
        )
        touchStats(userId) { stats ->
            stats.copy(weeklyLessons = stats.weeklyLessons + 1)
        }
    }

    suspend fun recordPractice(userId: Long) {
        touchStats(userId) { stats ->
            stats.copy(weeklyPractice = stats.weeklyPractice + 1)
        }
    }

    suspend fun recordReview(userId: Long) {
        touchStats(userId) { stats ->
            stats.copy(weeklyReview = stats.weeklyReview + 1)
        }
    }

    suspend fun recordHomework(userId: Long) {
        touchStats(userId) { stats ->
            stats.copy(weeklyHomework = stats.weeklyHomework + 1)
        }
    }

    suspend fun getProgressSummary(userId: Long): ProgressSummary {
        val completedLessons = userProgressRepository.countCompleted(userId).toInt()
        val totalLessons = lessonService.getLessonsByLanguage(com.turki.core.domain.Language.TURKISH).size
        val stats = userStatsRepository.findByUserId(userId)
        return ProgressSummary(
            completedLessons = completedLessons,
            totalLessons = totalLessons,
            currentLevel = "A1",
            currentStreak = stats?.currentStreak ?: 0
        )
    }

    suspend fun getCompletedLessonIds(userId: Long): Set<Int> {
        return userProgressRepository.listCompletedLessonIds(userId).toSet()
    }

    suspend fun getWeeklyStats(userId: Long): UserStats {
        return userStatsRepository.findByUserId(userId) ?: UserStats(
            userId = userId,
            currentStreak = 0,
            lastActiveAt = null,
            weeklyLessons = 0,
            weeklyPractice = 0,
            weeklyReview = 0,
            weeklyHomework = 0,
            lastWeeklyReportAt = null
        )
    }

    suspend fun resetProgress(userId: Long) {
        userProgressRepository.deleteByUser(userId)
        touchStats(userId) { stats ->
            stats.copy(
                currentStreak = 0,
                weeklyLessons = 0,
                weeklyPractice = 0,
                weeklyReview = 0,
                weeklyHomework = 0
            )
        }
    }

    suspend fun resetWeekly(userId: Long) {
        userStatsRepository.resetWeekly(userId)
    }

    private suspend fun touchStats(userId: Long, update: (UserStats) -> UserStats) {
        val now = Clock.System.now()
        val existing = userStatsRepository.findByUserId(userId)
        val base = existing ?: UserStats(
            userId = userId,
            currentStreak = 0,
            lastActiveAt = null,
            weeklyLessons = 0,
            weeklyPractice = 0,
            weeklyReview = 0,
            weeklyHomework = 0,
            lastWeeklyReportAt = null
        )

        val next = update(base).let { updated ->
            val currentStreak = calculateStreak(updated.currentStreak, updated.lastActiveAt, now)
            updated.copy(
                currentStreak = currentStreak,
                lastActiveAt = now
            )
        }

        userStatsRepository.upsert(next)
    }

    private fun calculateStreak(current: Int, lastActiveAt: Instant?, now: Instant): Int {
        if (lastActiveAt == null) {
            return 1
        }
        val zone = TimeZone.currentSystemDefault()
        val lastDate = lastActiveAt.toLocalDateTime(zone).date
        val today = now.toLocalDateTime(zone).date
        return when {
            lastDate == today -> current
            lastDate == today.minus(1, DateTimeUnit.DAY) -> current + 1
            else -> 1
        }
    }
}

data class ProgressSummary(
    val completedLessons: Int,
    val totalLessons: Int,
    val currentLevel: String,
    val currentStreak: Int
)
