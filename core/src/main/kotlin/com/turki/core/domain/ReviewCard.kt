package com.turki.core.domain

import kotlinx.datetime.Instant

data class ReviewCard(
    val userId: Long,
    val vocabularyId: Int,
    val stage: Int,
    val nextReviewAt: Instant,
    val lastResult: Boolean?
)
