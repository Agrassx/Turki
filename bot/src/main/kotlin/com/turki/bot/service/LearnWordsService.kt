package com.turki.bot.service

import com.turki.core.domain.Language
import com.turki.core.domain.ReviewCard
import com.turki.core.domain.VocabularyItem
import com.turki.core.repository.ReviewRepository
import kotlin.random.Random

private const val OPTIONS_COUNT = 4
private const val QUESTIONS_PER_WORD = 2
private const val WEAK_THRESHOLD = 50
private const val MEDIUM_THRESHOLD = 80
private const val WEAK_WEIGHT = 3
private const val MEDIUM_WEIGHT = 2

/**
 * Service that builds Duolingo-style "learn new words" sessions.
 * Each word appears in multiple exercise types for reinforcement.
 * Prioritises words the user knows poorly, but mixes in others for variety.
 */
class LearnWordsService(
    private val lessonService: LessonService,
    private val reviewRepository: ReviewRepository,
    private val random: Random = Random.Default
) {
    /**
     * Build a learning session with words from the user's current and completed lessons.
     * Words the user answers incorrectly more often get higher selection weight.
     */
    suspend fun buildSession(
        userId: Long,
        currentLessonId: Int,
        difficulty: LearnDifficulty
    ): LearnSessionPayload {
        val wordCount = difficulty.wordCount

        // Gather vocabulary from all lessons up to current
        val allLessons = lessonService.getLessonsByLanguage(Language.TURKISH)
        val currentLesson = allLessons.find { it.id == currentLessonId }
        val maxOrder = currentLesson?.orderIndex ?: Int.MAX_VALUE
        val lessons = allLessons.filter { it.orderIndex <= maxOrder }
        val allVocabulary = lessons.flatMap { lessonService.getVocabulary(it.id) }.distinctBy { it.id }

        if (allVocabulary.isEmpty()) {
            return LearnSessionPayload(emptyList(), 0, 0, difficulty)
        }

        // Build accuracy map from review cards
        val cards = reviewRepository.getAllByUser(userId)
        val accuracyMap = cards.associate { it.vocabularyId to it }

        // Weighted selection: poorly-known words appear more often
        val targetWords = weightedSelect(allVocabulary, accuracyMap, wordCount)

        // Generate multiple questions per word
        val questions = mutableListOf<LearnQuestion>()
        val questionTypes = LearnQuestionType.entries

        targetWords.forEach { word ->
            val typesForWord = questionTypes.toList().shuffled(random).take(QUESTIONS_PER_WORD)
            typesForWord.forEach { type ->
                questions.add(createQuestion(word, type, allVocabulary))
            }
        }

        return LearnSessionPayload(
            questions = deduplicateConsecutive(questions.shuffled(random)) { it.vocabularyId },
            currentIndex = 0,
            correctCount = 0,
            difficulty = difficulty
        )
    }

    /**
     * Select words with weighted probability based on accuracy.
     * Words with <50% accuracy get 3x weight, 50-80% get 2x, >80% get 1x.
     */
    private fun weightedSelect(
        vocabulary: List<VocabularyItem>,
        accuracyMap: Map<Int, ReviewCard>,
        count: Int
    ): List<VocabularyItem> {
        val weighted = vocabulary.flatMap { vocab ->
            val card = accuracyMap[vocab.id]
            val accuracy = card?.accuracyPercent
            val weight = when {
                accuracy == null -> MEDIUM_WEIGHT // never seen = medium priority
                accuracy < WEAK_THRESHOLD -> WEAK_WEIGHT
                accuracy < MEDIUM_THRESHOLD -> MEDIUM_WEIGHT
                else -> 1
            }
            List(weight) { vocab }
        }
        // Shuffle and deduplicate to pick distinct words
        return weighted.shuffled(random).distinctBy { it.id }.take(count)
    }

    private fun createQuestion(
        vocab: VocabularyItem,
        type: LearnQuestionType,
        allVocabulary: List<VocabularyItem>
    ): LearnQuestion {
        val (questionText, correctAnswer, optionPool) = when (type) {
            LearnQuestionType.MCQ_RU_TO_TR -> Triple(
                vocab.translation,
                vocab.word,
                allVocabulary.filter { it.id != vocab.id }.map { it.word }
            )
            LearnQuestionType.MCQ_TR_TO_RU -> Triple(
                vocab.word,
                vocab.translation,
                allVocabulary.filter { it.id != vocab.id }.map { it.translation }
            )
            LearnQuestionType.MCQ_CHOOSE_TR -> Triple(
                vocab.translation,
                vocab.word,
                allVocabulary.filter { it.id != vocab.id }.map { it.word }
            )
            LearnQuestionType.MCQ_CHOOSE_RU -> Triple(
                vocab.word,
                vocab.translation,
                allVocabulary.filter { it.id != vocab.id }.map { it.translation }
            )
        }

        val wrongOptions = optionPool.distinct().shuffled(random).take(OPTIONS_COUNT - 1)
        val options = (wrongOptions + correctAnswer).shuffled(random)

        return LearnQuestion(
            vocabularyId = vocab.id,
            type = type,
            questionText = questionText,
            correctAnswer = correctAnswer,
            options = options
        )
    }
}
