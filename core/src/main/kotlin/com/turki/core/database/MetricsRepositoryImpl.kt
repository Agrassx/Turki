package com.turki.core.database

import com.turki.core.domain.ErrorLog
import com.turki.core.domain.MetricSnapshot
import com.turki.core.repository.MetricsRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll

class MetricsRepositoryImpl : MetricsRepository {

    // Metric snapshots
    override suspend fun saveSnapshot(snapshot: MetricSnapshot): MetricSnapshot = DatabaseFactory.dbQuery {
        val now = Clock.System.now()
        val id = MetricsSnapshotsTable.insertAndGetId {
            it[date] = snapshot.date
            it[metricName] = snapshot.metricName
            it[value] = snapshot.value
            it[metadata] = snapshot.metadata
            it[createdAt] = now
        }
        snapshot.copy(id = id.value, createdAt = now)
    }

    override suspend fun getSnapshotsByDate(date: String): List<MetricSnapshot> = DatabaseFactory.dbQuery {
        MetricsSnapshotsTable.selectAll()
            .where { MetricsSnapshotsTable.date eq date }
            .map { toMetricSnapshot(it) }
    }

    override suspend fun getSnapshotsByMetric(
        metricName: String,
        fromDate: String,
        toDate: String
    ): List<MetricSnapshot> = DatabaseFactory.dbQuery {
        MetricsSnapshotsTable.selectAll()
            .where {
                (MetricsSnapshotsTable.metricName eq metricName) and
                    (MetricsSnapshotsTable.date greaterEq fromDate) and
                    (MetricsSnapshotsTable.date lessEq toDate)
            }
            .orderBy(MetricsSnapshotsTable.date)
            .map { toMetricSnapshot(it) }
    }

    override suspend fun getLatestSnapshot(metricName: String): MetricSnapshot? = DatabaseFactory.dbQuery {
        MetricsSnapshotsTable.selectAll()
            .where { MetricsSnapshotsTable.metricName eq metricName }
            .orderBy(MetricsSnapshotsTable.date, SortOrder.DESC)
            .limit(1)
            .map { toMetricSnapshot(it) }
            .firstOrNull()
    }

    // Error logs
    override suspend fun logError(error: ErrorLog): ErrorLog = DatabaseFactory.dbQuery {
        val now = Clock.System.now()
        val id = ErrorLogsTable.insertAndGetId {
            it[errorType] = error.errorType
            it[message] = error.message
            it[stackTrace] = error.stackTrace
            it[userId] = error.userId
            it[context] = error.context
            it[createdAt] = now
        }
        error.copy(id = id.value, createdAt = now)
    }

    override suspend fun getErrorsSince(since: Instant): List<ErrorLog> = DatabaseFactory.dbQuery {
        ErrorLogsTable.selectAll()
            .where { ErrorLogsTable.createdAt greaterEq since }
            .orderBy(ErrorLogsTable.createdAt, SortOrder.DESC)
            .map { toErrorLog(it) }
    }

    override suspend fun countErrorsSince(since: Instant): Long = DatabaseFactory.dbQuery {
        ErrorLogsTable.selectAll()
            .where { ErrorLogsTable.createdAt greaterEq since }
            .count()
    }

    override suspend fun getErrorsByType(errorType: String, since: Instant): List<ErrorLog> =
        DatabaseFactory.dbQuery {
            ErrorLogsTable.selectAll()
                .where {
                    (ErrorLogsTable.errorType eq errorType) and
                        (ErrorLogsTable.createdAt greaterEq since)
                }
                .orderBy(ErrorLogsTable.createdAt, SortOrder.DESC)
                .map { toErrorLog(it) }
        }

    // Aggregations from analytics_events
    override suspend fun countActiveUsersSince(since: Instant): Long = DatabaseFactory.dbQuery {
        AnalyticsEventsTable.select(AnalyticsEventsTable.userId)
            .where { AnalyticsEventsTable.createdAt greaterEq since }
            .withDistinct()
            .count()
    }

    override suspend fun countEventsSince(eventName: String, since: Instant): Long = DatabaseFactory.dbQuery {
        AnalyticsEventsTable.selectAll()
            .where {
                (AnalyticsEventsTable.eventName eq eventName) and
                    (AnalyticsEventsTable.createdAt greaterEq since)
            }
            .count()
    }

    override suspend fun countDistinctUsersByEventSince(eventName: String, since: Instant): Long =
        DatabaseFactory.dbQuery {
            AnalyticsEventsTable.select(AnalyticsEventsTable.userId)
                .where {
                    (AnalyticsEventsTable.eventName eq eventName) and
                        (AnalyticsEventsTable.createdAt greaterEq since)
                }
                .withDistinct()
                .count()
        }

    override suspend fun getTopEventsSince(since: Instant, limit: Int): Map<String, Long> = DatabaseFactory.dbQuery {
        val countCol = AnalyticsEventsTable.id.count()
        AnalyticsEventsTable
            .select(AnalyticsEventsTable.eventName, countCol)
            .where { AnalyticsEventsTable.createdAt greaterEq since }
            .groupBy(AnalyticsEventsTable.eventName)
            .orderBy(countCol, SortOrder.DESC)
            .limit(limit)
            .associate { it[AnalyticsEventsTable.eventName] to it[countCol] }
    }

    private fun toMetricSnapshot(row: ResultRow) = MetricSnapshot(
        id = row[MetricsSnapshotsTable.id].value,
        date = row[MetricsSnapshotsTable.date],
        metricName = row[MetricsSnapshotsTable.metricName],
        value = row[MetricsSnapshotsTable.value],
        metadata = row[MetricsSnapshotsTable.metadata],
        createdAt = row[MetricsSnapshotsTable.createdAt]
    )

    private fun toErrorLog(row: ResultRow) = ErrorLog(
        id = row[ErrorLogsTable.id].value,
        errorType = row[ErrorLogsTable.errorType],
        message = row[ErrorLogsTable.message],
        stackTrace = row[ErrorLogsTable.stackTrace],
        userId = row[ErrorLogsTable.userId],
        context = row[ErrorLogsTable.context],
        createdAt = row[ErrorLogsTable.createdAt]
    )
}
