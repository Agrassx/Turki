package com.turki.bot.service

import com.turki.core.domain.Language
import com.turki.core.domain.VocabularyItem
import kotlin.random.Random

private const val OPTIONS_COUNT = 4
private const val QUESTIONS_PER_WORD = 2

/**
 * Service that builds Duolingo-style "learn new words" sessions.
 * Each word appears in multiple exercise types for reinforcement.
 */
class LearnWordsService(
    private val lessonService: LessonService,
    private val random: Random = Random.Default
) {
    /**
     * Build a learning session with words from the user's current and completed lessons.
     */
    suspend fun buildSession(
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

        // Pick N words to learn
        val targetWords = allVocabulary.shuffled(random).take(wordCount)

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
            questions = questions.shuffled(random),
            currentIndex = 0,
            correctCount = 0,
            difficulty = difficulty
        )
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
