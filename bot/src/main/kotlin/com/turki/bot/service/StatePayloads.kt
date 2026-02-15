package com.turki.bot.service

import kotlinx.serialization.Serializable

enum class UserFlowState {
    EXERCISE,
    REVIEW,
    LEARN_WORDS,
    DICT_SEARCH,
    DICT_ADD_CUSTOM,
    HOMEWORK_TEXT,
    SUPPORT_MESSAGE
}

@Serializable
data class ExerciseFlowPayload(
    val lessonId: Int,
    val exerciseIndex: Int,
    val vocabularyIds: List<Int>,
    val optionsByVocabId: Map<Int, List<String>>,
    val correctByVocabId: Map<Int, String>,
    val explanationsByVocabId: Map<Int, String>
)

@Serializable
data class ReviewFlowPayload(
    val vocabularyIds: List<Int>,
    val index: Int
)

/**
 * Question for review session with translation direction support.
 */
@Serializable
data class ReviewQuestion(
    val id: String,                    // Unique ID for this question
    val sourceType: ReviewSourceType,  // Where the question came from
    val sourceId: Int,                 // ID of vocabulary/homework question
    val questionText: String,          // Text to show as question
    val correctAnswer: String,         // Expected answer
    val options: List<String>?,        // Options for multiple choice (null for text input)
    val direction: TranslationDirection
)

@Serializable
enum class ReviewSourceType {
    VOCABULARY,      // From lesson vocabulary
    HOMEWORK,        // From homework questions
    USER_DICTIONARY  // From user's personal dictionary
}

@Serializable
enum class TranslationDirection {
    RU_TO_TR,  // Russian to Turkish
    TR_TO_RU   // Turkish to Russian
}

@Serializable
data class ReviewSessionPayload(
    val questions: List<ReviewQuestion>,
    val currentIndex: Int,
    val correctCount: Int,
    val difficulty: ReviewDifficulty
)

@Serializable
enum class ReviewDifficulty(val questionCount: Int) {
    WARMUP(10),
    TRAINING(20),
    MARATHON(30)
}

@Serializable
data class DictionaryFlowPayload(
    val placeholder: String = "search"
)

// --- Learn words ---

@Serializable
enum class LearnDifficulty(val wordCount: Int) {
    EASY(5),
    MEDIUM(10),
    HARD(15)
}

@Serializable
enum class LearnQuestionType {
    MCQ_RU_TO_TR,  // Show Russian, pick Turkish
    MCQ_TR_TO_RU,  // Show Turkish, pick Russian
    MCQ_CHOOSE_TR, // "Выберите турецкое слово для 'X'"
    MCQ_CHOOSE_RU  // "Выберите русский перевод для 'X'"
}

@Serializable
data class LearnQuestion(
    val vocabularyId: Int,
    val type: LearnQuestionType,
    val questionText: String,
    val correctAnswer: String,
    val options: List<String>
)

@Serializable
data class LearnSessionPayload(
    val questions: List<LearnQuestion>,
    val currentIndex: Int,
    val correctCount: Int,
    val difficulty: LearnDifficulty
)
