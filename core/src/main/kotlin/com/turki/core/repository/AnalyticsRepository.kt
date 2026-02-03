package com.turki.core.repository

import com.turki.core.domain.AnalyticsEvent
import kotlinx.datetime.Instant

interface AnalyticsRepository {
    suspend fun create(event: AnalyticsEvent): AnalyticsEvent
    suspend fun findByUserSince(userId: Long, since: Instant): List<AnalyticsEvent>
    suspend fun countByUserAndEvent(userId: Long, eventName: String, since: Instant): Long
    suspend fun deleteByUser(userId: Long): Boolean
}
