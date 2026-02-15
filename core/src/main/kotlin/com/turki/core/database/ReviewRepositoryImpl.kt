package com.turki.core.database

import com.turki.core.domain.ReviewCard
import com.turki.core.repository.ReviewRepository
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class ReviewRepositoryImpl : ReviewRepository {
    override suspend fun getDueCards(userId: Long, now: Instant, limit: Int): List<ReviewCard> =
        DatabaseFactory.dbQuery {
            ReviewCardsTable.selectAll()
                .where { (ReviewCardsTable.userId eq userId) and (ReviewCardsTable.nextReviewAt lessEq now) }
                .orderBy(ReviewCardsTable.nextReviewAt)
                .limit(limit)
                .map(::toCard)
        }

    override suspend fun findByUserAndVocabulary(userId: Long, vocabularyId: Int): ReviewCard? =
        DatabaseFactory.dbQuery {
            ReviewCardsTable.selectAll()
                .where { (ReviewCardsTable.userId eq userId) and (ReviewCardsTable.vocabularyId eq vocabularyId) }
                .map(::toCard)
                .singleOrNull()
        }

    override suspend fun getAllByUser(userId: Long): List<ReviewCard> = DatabaseFactory.dbQuery {
        ReviewCardsTable.selectAll()
            .where { ReviewCardsTable.userId eq userId }
            .map(::toCard)
    }

    override suspend fun upsert(card: ReviewCard): ReviewCard = DatabaseFactory.dbQuery {
        val updated = ReviewCardsTable.update({
            (ReviewCardsTable.userId eq card.userId) and
                (ReviewCardsTable.vocabularyId eq card.vocabularyId)
        }) {
            it[stage] = card.stage
            it[nextReviewAt] = card.nextReviewAt
            it[lastResult] = card.lastResult
            it[correctCount] = card.correctCount
            it[totalAttempts] = card.totalAttempts
        }

        if (updated == 0) {
            ReviewCardsTable.insert {
                it[userId] = card.userId
                it[vocabularyId] = card.vocabularyId
                it[stage] = card.stage
                it[nextReviewAt] = card.nextReviewAt
                it[lastResult] = card.lastResult
                it[correctCount] = card.correctCount
                it[totalAttempts] = card.totalAttempts
            }
        }

        card
    }

    override suspend fun deleteByUser(userId: Long): Boolean = DatabaseFactory.dbQuery {
        ReviewCardsTable.deleteWhere { ReviewCardsTable.userId eq userId } > 0
    }

    private fun toCard(row: org.jetbrains.exposed.sql.ResultRow): ReviewCard = ReviewCard(
        userId = row[ReviewCardsTable.userId].value,
        vocabularyId = row[ReviewCardsTable.vocabularyId].value,
        stage = row[ReviewCardsTable.stage],
        nextReviewAt = row[ReviewCardsTable.nextReviewAt],
        lastResult = row[ReviewCardsTable.lastResult],
        correctCount = row[ReviewCardsTable.correctCount],
        totalAttempts = row[ReviewCardsTable.totalAttempts]
    )
}
