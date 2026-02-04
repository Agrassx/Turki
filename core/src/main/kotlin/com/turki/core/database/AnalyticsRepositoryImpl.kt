package com.turki.core.database

import com.turki.core.domain.AnalyticsEvent
import com.turki.core.repository.AnalyticsRepository
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

class AnalyticsRepositoryImpl : AnalyticsRepository {
    override suspend fun create(event: AnalyticsEvent): AnalyticsEvent = DatabaseFactory.dbQuery {
        AnalyticsEventsTable.insert {
            it[eventName] = event.eventName
            it[userId] = event.userId
            it[sessionId] = event.sessionId
            it[props] = event.props
            it[createdAt] = event.createdAt
        }
        event
    }

    override suspend fun findByUserSince(userId: Long, since: kotlinx.datetime.Instant): List<AnalyticsEvent> =
        DatabaseFactory.dbQuery {
            AnalyticsEventsTable.selectAll()
                .where { (AnalyticsEventsTable.userId eq userId) and (AnalyticsEventsTable.createdAt greaterEq since) }
                .map(::toEvent)
        }

    override suspend fun countByUserAndEvent(
        userId: Long,
        eventName: String,
        since: kotlinx.datetime.Instant
    ): Long = DatabaseFactory.dbQuery {
        AnalyticsEventsTable.selectAll()
            .where {
                (AnalyticsEventsTable.userId eq userId) and
                    (AnalyticsEventsTable.eventName eq eventName) and
                    (AnalyticsEventsTable.createdAt greaterEq since)
            }
            .count()
    }

    override suspend fun deleteByUser(userId: Long): Boolean = DatabaseFactory.dbQuery {
        AnalyticsEventsTable.deleteWhere { AnalyticsEventsTable.userId eq userId } > 0
    }

    override suspend fun countEventsBetween(
        eventName: String,
        from: kotlinx.datetime.Instant,
        to: kotlinx.datetime.Instant
    ): Long = DatabaseFactory.dbQuery {
        AnalyticsEventsTable.selectAll()
            .where {
                (AnalyticsEventsTable.eventName eq eventName) and
                    (AnalyticsEventsTable.createdAt greaterEq from) and
                    (AnalyticsEventsTable.createdAt less to)
            }
            .count()
    }

    override suspend fun countDistinctUsersWithEventsBetween(
        from: kotlinx.datetime.Instant,
        to: kotlinx.datetime.Instant
    ): Long = DatabaseFactory.dbQuery {
        AnalyticsEventsTable.select(AnalyticsEventsTable.userId)
            .where {
                (AnalyticsEventsTable.createdAt greaterEq from) and
                    (AnalyticsEventsTable.createdAt less to)
            }
            .withDistinct()
            .count()
    }

    private fun toEvent(row: org.jetbrains.exposed.sql.ResultRow): AnalyticsEvent = AnalyticsEvent(
        eventName = row[AnalyticsEventsTable.eventName],
        userId = row[AnalyticsEventsTable.userId].value,
        sessionId = row[AnalyticsEventsTable.sessionId],
        props = row[AnalyticsEventsTable.props],
        createdAt = row[AnalyticsEventsTable.createdAt]
    )
}
