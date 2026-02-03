package com.turki.bot.service

import kotlinx.serialization.Serializable

enum class UserFlowState {
    EXERCISE,
    REVIEW,
    DICT_SEARCH,
    DICT_ADD_CUSTOM,
    HOMEWORK_TEXT
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

@Serializable
data class DictionaryFlowPayload(
    val placeholder: String = "search"
)
