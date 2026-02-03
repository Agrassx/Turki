package com.turki.core.repository

import com.turki.core.domain.UserStats

interface UserStatsRepository {
    suspend fun findByUserId(userId: Long): UserStats?
    suspend fun upsert(stats: UserStats): UserStats
    suspend fun resetWeekly(userId: Long): Boolean
    suspend fun deleteByUser(userId: Long): Boolean
}
