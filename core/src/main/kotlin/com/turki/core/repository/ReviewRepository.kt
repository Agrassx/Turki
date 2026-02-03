package com.turki.core.repository

import com.turki.core.domain.ReviewCard
import kotlinx.datetime.Instant

interface ReviewRepository {
    suspend fun getDueCards(userId: Long, now: Instant, limit: Int): List<ReviewCard>
    suspend fun findByUserAndVocabulary(userId: Long, vocabularyId: Int): ReviewCard?
    suspend fun upsert(card: ReviewCard): ReviewCard
    suspend fun deleteByUser(userId: Long): Boolean
}
