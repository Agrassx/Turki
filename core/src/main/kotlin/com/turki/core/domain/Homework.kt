package com.turki.core.domain

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Homework(
    val id: Int,
    val lessonId: Int,
    val questions: List<HomeworkQuestion>
)

@Serializable
data class HomeworkQuestion(
    val id: Int,
    val homeworkId: Int,
    val questionType: QuestionType,
    val questionText: String,
    val options: List<String> = emptyList(),
    val correctAnswer: String
)

enum class QuestionType {
    MULTIPLE_CHOICE,
    TEXT_INPUT,
    TRANSLATION
}

@Serializable
data class HomeworkSubmission(
    val id: Long,
    val userId: Long,
    val homeworkId: Int,
    val answers: Map<Int, String>,
    val score: Int,
    val maxScore: Int,
    val submittedAt: Instant
)
