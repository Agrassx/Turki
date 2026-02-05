package com.turki.core.database

import com.turki.core.domain.ReminderPreference
import com.turki.core.repository.ReminderPreferenceRepository
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class ReminderPreferenceRepositoryImpl(
    private val clock: Clock = Clock.System
) : ReminderPreferenceRepository {
    override suspend fun findByUserId(userId: Long): ReminderPreference? = DatabaseFactory.dbQuery {
        ReminderPreferencesTable.selectAll()
            .where { ReminderPreferencesTable.userId eq userId }
            .map(::toPreference)
            .singleOrNull()
    }

    override suspend fun upsert(preference: ReminderPreference): ReminderPreference = DatabaseFactory.dbQuery {
        val updated = ReminderPreferencesTable.update({ ReminderPreferencesTable.userId eq preference.userId }) {
            it[daysOfWeek] = preference.daysOfWeek
            it[timeLocal] = preference.timeLocal
            it[isEnabled] = preference.isEnabled
            it[lastFiredAt] = preference.lastFiredAt
        }

        if (updated == 0) {
            ReminderPreferencesTable.insert {
                it[userId] = preference.userId
                it[daysOfWeek] = preference.daysOfWeek
                it[timeLocal] = preference.timeLocal
                it[isEnabled] = preference.isEnabled
                it[lastFiredAt] = preference.lastFiredAt
            }
        }

        preference
    }

    override suspend fun updateLastFired(userId: Long): Boolean = DatabaseFactory.dbQuery {
        ReminderPreferencesTable.update({ ReminderPreferencesTable.userId eq userId }) {
            it[lastFiredAt] = clock.now()
        } > 0
    }

    override suspend fun deleteByUser(userId: Long): Boolean = DatabaseFactory.dbQuery {
        ReminderPreferencesTable.deleteWhere { ReminderPreferencesTable.userId eq userId } > 0
    }

    private fun toPreference(row: org.jetbrains.exposed.sql.ResultRow): ReminderPreference = ReminderPreference(
        userId = row[ReminderPreferencesTable.userId].value,
        daysOfWeek = row[ReminderPreferencesTable.daysOfWeek],
        timeLocal = row[ReminderPreferencesTable.timeLocal],
        isEnabled = row[ReminderPreferencesTable.isEnabled],
        lastFiredAt = row[ReminderPreferencesTable.lastFiredAt]
    )
}
