package com.turki.bot.service

import com.turki.core.domain.Language
import com.turki.core.domain.QuestionType
import com.turki.core.domain.ReviewCard
import com.turki.core.domain.VocabularyItem
import com.turki.core.repository.HomeworkRepository
import com.turki.core.repository.ReviewRepository
import com.turki.core.repository.UserDictionaryRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlin.random.Random

class ReviewService(
    private val lessonService: LessonService,
    private val reviewRepository: ReviewRepository,
    private val userDictionaryRepository: UserDictionaryRepository,
    private val homeworkRepository: HomeworkRepository
) {
    companion object {
        private const val MAX_STAGE = 6
        private const val STAGE_2_DAYS = 3
        private const val STAGE_3_DAYS = 5
        private const val STAGE_4_DAYS = 7
        private const val STAGE_5_DAYS = 10
        private const val STAGE_FINAL_DAYS = 14
        private const val OPTIONS_COUNT = 4
    }

    /**
     * Builds a review session with questions from multiple sources.
     * Questions include vocabulary, homework questions, and user's dictionary.
     * Supports both RU→TR and TR→RU translation directions.
     */
    suspend fun buildReviewSession(
        userId: Long,
        currentLessonId: Int,
        difficulty: ReviewDifficulty
    ): ReviewSessionPayload {
        val questionCount = difficulty.questionCount
        val questions = mutableListOf<ReviewQuestion>()

        // 1. Get vocabulary from user's dictionary (favorites)
        val userDictionary = userDictionaryRepository.listFavorites(userId, questionCount)
            .mapNotNull { entry -> lessonService.findVocabularyById(entry.vocabularyId) }

        // 2. Get vocabulary from completed lessons
        val lessons = lessonService.getLessonsByLanguage(Language.TURKISH)
            .filter { it.id <= currentLessonId }
        val lessonVocabulary = lessons.flatMap { lessonService.getVocabulary(it.id) }

        // 3. Get homework questions from completed lessons
        val homeworkQuestions = lessons.mapNotNull { lesson ->
            homeworkRepository.findByLessonId(lesson.id)
        }.flatMap { it.questions }
            .filter { it.questionType == QuestionType.MULTIPLE_CHOICE }

        // Build vocabulary pool for generating options
        val allVocabulary = (userDictionary + lessonVocabulary).distinctBy { it.id }

        // Generate questions from user dictionary (priority)
        userDictionary.take(questionCount / 3).forEach { vocab ->
            val direction = if (Random.nextBoolean()) TranslationDirection.RU_TO_TR else TranslationDirection.TR_TO_RU
            questions.add(createVocabularyQuestion(vocab, direction, allVocabulary, ReviewSourceType.USER_DICTIONARY))
        }

        // Generate questions from lesson vocabulary
        val vocabNeeded = (questionCount * 2 / 3) - questions.size
        lessonVocabulary.shuffled().take(vocabNeeded).forEach { vocab ->
            val direction = if (Random.nextBoolean()) TranslationDirection.RU_TO_TR else TranslationDirection.TR_TO_RU
            questions.add(createVocabularyQuestion(vocab, direction, allVocabulary, ReviewSourceType.VOCABULARY))
        }

        // Add homework questions if we need more
        val homeworkNeeded = questionCount - questions.size
        homeworkQuestions.shuffled().take(homeworkNeeded).forEach { hw ->
            questions.add(
                ReviewQuestion(
                    id = "hw_${hw.id}",
                    sourceType = ReviewSourceType.HOMEWORK,
                    sourceId = hw.id,
                    questionText = hw.questionText,
                    correctAnswer = hw.correctAnswer,
                    options = hw.options.takeIf { it.isNotEmpty() },
                    direction = TranslationDirection.RU_TO_TR // Homework is typically RU→TR
                )
            )
        }

        // Fill remaining with more vocabulary if needed
        if (questions.size < questionCount) {
            val remaining = questionCount - questions.size
            val usedIds = questions.mapNotNull {
                if (it.sourceType == ReviewSourceType.VOCABULARY || it.sourceType == ReviewSourceType.USER_DICTIONARY) {
                    it.sourceId
                } else null
            }.toSet()

            allVocabulary.filterNot { it.id in usedIds }
                .shuffled()
                .take(remaining)
                .forEach { vocab ->
                    val direction = if (Random.nextBoolean()) TranslationDirection.RU_TO_TR else TranslationDirection.TR_TO_RU
                    questions.add(createVocabularyQuestion(vocab, direction, allVocabulary, ReviewSourceType.VOCABULARY))
                }
        }

        return ReviewSessionPayload(
            questions = questions.shuffled().take(questionCount),
            currentIndex = 0,
            correctCount = 0,
            difficulty = difficulty
        )
    }

    private fun createVocabularyQuestion(
        vocab: VocabularyItem,
        direction: TranslationDirection,
        allVocabulary: List<VocabularyItem>,
        sourceType: ReviewSourceType
    ): ReviewQuestion {
        val (questionText, correctAnswer) = when (direction) {
            TranslationDirection.RU_TO_TR -> vocab.translation to vocab.word
            TranslationDirection.TR_TO_RU -> vocab.word to vocab.translation
        }

        // Generate wrong options from other vocabulary
        val wrongOptions = allVocabulary
            .filter { it.id != vocab.id }
            .shuffled()
            .take(OPTIONS_COUNT - 1)
            .map { if (direction == TranslationDirection.RU_TO_TR) it.word else it.translation }

        val options = (wrongOptions + correctAnswer).shuffled()

        return ReviewQuestion(
            id = "vocab_${vocab.id}_${direction.name}",
            sourceType = sourceType,
            sourceId = vocab.id,
            questionText = questionText,
            correctAnswer = correctAnswer,
            options = options,
            direction = direction
        )
    }

    // Legacy method for old review flow (kept for compatibility)
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

        val lessons = lessonService.getLessonsByLanguage(Language.TURKISH)
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
