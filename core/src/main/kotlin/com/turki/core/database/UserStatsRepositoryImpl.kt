package com.turki.core.database

import com.turki.core.domain.UserStats
import com.turki.core.repository.UserStatsRepository
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class UserStatsRepositoryImpl(
    private val clock: Clock = Clock.System
) : UserStatsRepository {
    override suspend fun findByUserId(userId: Long): UserStats? = DatabaseFactory.dbQuery {
        UserStatsTable.selectAll()
            .where { UserStatsTable.userId eq userId }
            .map(::toStats)
            .singleOrNull()
    }

    override suspend fun upsert(stats: UserStats): UserStats = DatabaseFactory.dbQuery {
        val updated = UserStatsTable.update({ UserStatsTable.userId eq stats.userId }) {
            it[currentStreak] = stats.currentStreak
            it[lastActiveAt] = stats.lastActiveAt
            it[weeklyLessons] = stats.weeklyLessons
            it[weeklyPractice] = stats.weeklyPractice
            it[weeklyReview] = stats.weeklyReview
            it[weeklyHomework] = stats.weeklyHomework
            it[lastWeeklyReportAt] = stats.lastWeeklyReportAt
        }

        if (updated == 0) {
            UserStatsTable.insert {
                it[userId] = stats.userId
                it[currentStreak] = stats.currentStreak
                it[lastActiveAt] = stats.lastActiveAt
                it[weeklyLessons] = stats.weeklyLessons
                it[weeklyPractice] = stats.weeklyPractice
                it[weeklyReview] = stats.weeklyReview
                it[weeklyHomework] = stats.weeklyHomework
                it[lastWeeklyReportAt] = stats.lastWeeklyReportAt
            }
        }

        stats
    }

    override suspend fun resetWeekly(userId: Long): Boolean = DatabaseFactory.dbQuery {
        val now = clock.now()
        val updated = UserStatsTable.update({ UserStatsTable.userId eq userId }) {
            it[weeklyLessons] = 0
            it[weeklyPractice] = 0
            it[weeklyReview] = 0
            it[weeklyHomework] = 0
            it[lastWeeklyReportAt] = now
        }
        if (updated == 0) {
            UserStatsTable.insert {
                it[UserStatsTable.userId] = userId
                it[currentStreak] = 0
                it[lastActiveAt] = null
                it[weeklyLessons] = 0
                it[weeklyPractice] = 0
                it[weeklyReview] = 0
                it[weeklyHomework] = 0
                it[lastWeeklyReportAt] = now
            }
        }
        true
    }

    override suspend fun deleteByUser(userId: Long): Boolean = DatabaseFactory.dbQuery {
        UserStatsTable.deleteWhere { UserStatsTable.userId eq userId } > 0
    }

    private fun toStats(row: org.jetbrains.exposed.sql.ResultRow): UserStats = UserStats(
        userId = row[UserStatsTable.userId].value,
        currentStreak = row[UserStatsTable.currentStreak],
        lastActiveAt = row[UserStatsTable.lastActiveAt],
        weeklyLessons = row[UserStatsTable.weeklyLessons],
        weeklyPractice = row[UserStatsTable.weeklyPractice],
        weeklyReview = row[UserStatsTable.weeklyReview],
        weeklyHomework = row[UserStatsTable.weeklyHomework],
        lastWeeklyReportAt = row[UserStatsTable.lastWeeklyReportAt]
    )
}
