package com.turki.core.domain

import kotlinx.serialization.Serializable

@Serializable
data class Lesson(
    val id: Int,
    val orderIndex: Int,
    val targetLanguage: Language,
    val title: String,
    val description: String,
    val content: String,
    val vocabularyItems: List<VocabularyItem> = emptyList()
)

@Serializable
data class VocabularyItem(
    val id: Int,
    val lessonId: Int,
    val word: String,
    val translation: String,
    val pronunciation: String? = null,
    val example: String? = null
)
