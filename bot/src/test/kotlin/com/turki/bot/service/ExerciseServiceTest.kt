package com.turki.bot.service

import com.turki.core.domain.VocabularyItem
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExerciseServiceTest {
    @Test
    fun `returns empty list when no vocabulary`() = runTest {
        val lessonService = mockk<LessonService>()
        coEvery { lessonService.getVocabulary(1) } returns emptyList()

        val service = ExerciseService(lessonService)
        val result = service.buildLessonExercises(1)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `builds exercises with correct options`() = runTest {
        val lessonService = mockk<LessonService>()
        val vocab = listOf(
            VocabularyItem(1, 1, "Merhaba", "Привет"),
            VocabularyItem(2, 1, "Günaydın", "Доброе утро"),
            VocabularyItem(3, 1, "Teşekkürler", "Спасибо"),
            VocabularyItem(4, 1, "Lütfen", "Пожалуйста")
        )
        coEvery { lessonService.getVocabulary(1) } returns vocab

        val service = ExerciseService(lessonService)
        val result = service.buildLessonExercises(1, limit = 3)

        assertEquals(3, result.size)
        result.forEach { item ->
            assertTrue(item.options.contains(item.correctOption))
            assertTrue(item.options.size in 1..4)
        }
    }
}
