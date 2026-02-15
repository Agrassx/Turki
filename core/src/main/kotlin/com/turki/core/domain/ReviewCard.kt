package com.turki.core.domain

import kotlinx.datetime.Instant

data class ReviewCard(
    val userId: Long,
    val vocabularyId: Int,
    val stage: Int,
    val nextReviewAt: Instant,
    val lastResult: Boolean?,
    val correctCount: Int = 0,
    val totalAttempts: Int = 0
) {
    /** Accuracy as a percentage (0..100), or null if no attempts yet. */
    val accuracyPercent: Int?
        get() = if (totalAttempts > 0) (correctCount * 100) / totalAttempts else null
}
