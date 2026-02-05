package com.turki.bot.service

import com.turki.core.domain.Language
import com.turki.core.domain.Lesson
import com.turki.core.domain.UserStats
import com.turki.core.repository.UserProgressRepository
import com.turki.core.repository.UserStatsRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProgressServiceTest {
    private val userProgressRepository = mockk<UserProgressRepository>()
    private val userStatsRepository = mockk<UserStatsRepository>()
    private val lessonService = mockk<LessonService>()
    private val service = ProgressService(userProgressRepository, userStatsRepository, lessonService)

    @Test
    fun `getProgressSummary uses completed count and total lessons`() = runTest {
        coEvery { userProgressRepository.countCompleted(1) } returns 2L
        coEvery { lessonService.getLessonsByLanguage(Language.TURKISH) } returns listOf(
            Lesson(1, 1, Language.TURKISH, title = "L1", description = "", content = ""),
            Lesson(2, 2, Language.TURKISH, title = "L2", description = "", content = ""),
            Lesson(3, 3, Language.TURKISH, title = "L3", description = "", content = "")
        )
        coEvery { userStatsRepository.findByUserId(1) } returns UserStats(
            userId = 1,
            currentStreak = 4,
            lastActiveAt = null,
            weeklyLessons = 0,
            weeklyPractice = 0,
            weeklyReview = 0,
            weeklyHomework = 0,
            lastWeeklyReportAt = null
        )

        val summary = service.getProgressSummary(1)

        assertEquals(2, summary.completedLessons)
        assertEquals(3, summary.totalLessons)
        assertEquals(4, summary.currentStreak)
        assertEquals("A1", summary.currentLevel)
    }

    @Test
    fun `getWeeklyStats returns default when missing`() = runTest {
        coEvery { userStatsRepository.findByUserId(5) } returns null

        val stats = service.getWeeklyStats(5)

        assertEquals(5, stats.userId)
        assertEquals(0, stats.currentStreak)
        assertTrue(stats.weeklyLessons == 0 && stats.weeklyPractice == 0)
    }

    @Test
    fun `getCompletedLessonIds returns set`() = runTest {
        coEvery { userProgressRepository.listCompletedLessonIds(7) } returns listOf(1, 2, 2, 3)

        val result = service.getCompletedLessonIds(7)

        assertEquals(setOf(1, 2, 3), result)
    }
}
