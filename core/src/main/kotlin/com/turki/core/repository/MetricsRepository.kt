package com.turki.core.repository

import com.turki.core.domain.ErrorLog
import com.turki.core.domain.MetricSnapshot
import kotlinx.datetime.Instant

/**
 * Repository for metrics and error logs.
 */
interface MetricsRepository {
    // Metric snapshots
    suspend fun saveSnapshot(snapshot: MetricSnapshot): MetricSnapshot
    suspend fun getSnapshotsByDate(date: String): List<MetricSnapshot>
    suspend fun getSnapshotsByMetric(metricName: String, fromDate: String, toDate: String): List<MetricSnapshot>
    suspend fun getLatestSnapshot(metricName: String): MetricSnapshot?

    // Error logs
    suspend fun logError(error: ErrorLog): ErrorLog
    suspend fun getErrorsSince(since: Instant): List<ErrorLog>
    suspend fun countErrorsSince(since: Instant): Long
    suspend fun getErrorsByType(errorType: String, since: Instant): List<ErrorLog>

    // Aggregations from analytics_events
    suspend fun countActiveUsersSince(since: Instant): Long
    suspend fun countEventsSince(eventName: String, since: Instant): Long
    suspend fun countDistinctUsersByEventSince(eventName: String, since: Instant): Long
    suspend fun getTopEventsSince(since: Instant, limit: Int): Map<String, Long>
}
