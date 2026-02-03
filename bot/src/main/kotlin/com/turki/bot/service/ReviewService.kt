package com.turki.bot.service

import com.turki.core.domain.ReviewCard
import com.turki.core.domain.VocabularyItem
import com.turki.core.repository.ReviewRepository
import com.turki.core.repository.UserDictionaryRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus

class ReviewService(
    private val lessonService: LessonService,
    private val reviewRepository: ReviewRepository,
    private val userDictionaryRepository: UserDictionaryRepository
) {
    companion object {
        private const val MAX_STAGE = 6
        private const val STAGE_2_DAYS = 3
        private const val STAGE_3_DAYS = 5
        private const val STAGE_4_DAYS = 7
        private const val STAGE_5_DAYS = 10
        private const val STAGE_FINAL_DAYS = 14
    }
    suspend fun buildQueue(userId: Long, limit: Int, currentLessonId: Int): List<VocabularyItem> {
        val now = Clock.System.now()
        val due = reviewRepository.getDueCards(userId, now, limit).mapNotNull { card ->
            lessonService.findVocabularyById(card.vocabularyId)
        }.toMutableList()

        if (due.size >= limit) {
            return due
        }

        val favorites = userDictionaryRepository.listFavorites(userId, limit)
            .mapNotNull { lessonService.findVocabularyById(it.vocabularyId) }
        due.addAll(favorites.filterNot { item -> due.any { it.id == item.id } })

        if (due.size >= limit) {
            return due.take(limit)
        }

        val lessons = lessonService.getLessonsByLanguage(com.turki.core.domain.Language.TURKISH)
        val allowedLessons = lessons.filter { it.id < currentLessonId }
        val extra = allowedLessons.flatMap { lessonService.getVocabulary(it.id) }
        due.addAll(extra.filterNot { item -> due.any { it.id == item.id } })

        return due.take(limit)
    }

    suspend fun updateCard(userId: Long, vocabularyId: Int, correct: Boolean) {
        val existing = reviewRepository.findByUserAndVocabulary(userId, vocabularyId)
        val stage = when {
            existing == null && correct -> 1
            existing == null -> 0
            correct -> (existing.stage + 1).coerceAtMost(MAX_STAGE)
            else -> (existing.stage - 1).coerceAtLeast(0)
        }
        val nextReviewAt = nextReviewTime(stage, Clock.System.now())
        reviewRepository.upsert(
            ReviewCard(
                userId = userId,
                vocabularyId = vocabularyId,
                stage = stage,
                nextReviewAt = nextReviewAt,
                lastResult = correct
            )
        )
    }

    private fun nextReviewTime(stage: Int, now: kotlinx.datetime.Instant): kotlinx.datetime.Instant {
        val days = when (stage) {
            0 -> 1
            1 -> 2
            2 -> STAGE_2_DAYS
            3 -> STAGE_3_DAYS
            4 -> STAGE_4_DAYS
            5 -> STAGE_5_DAYS
            else -> STAGE_FINAL_DAYS
        }
        return now.plus(DateTimePeriod(days = days), TimeZone.currentSystemDefault())
    }

    suspend fun clearUser(userId: Long) {
        reviewRepository.deleteByUser(userId)
    }
}
