package com.turki.core.database

import com.turki.core.domain.Reminder
import com.turki.core.domain.ReminderType
import com.turki.core.repository.ReminderRepository
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class ReminderRepositoryImpl : ReminderRepository {

    override suspend fun findById(id: Long): Reminder? = DatabaseFactory.dbQuery {
        RemindersTable.selectAll().where { RemindersTable.id eq id }
            .map(::toReminder)
            .singleOrNull()
    }

    override suspend fun findPendingReminders(before: Instant): List<Reminder> = DatabaseFactory.dbQuery {
        RemindersTable.selectAll().where {
            (RemindersTable.sent eq false) and (RemindersTable.scheduledAt lessEq before)
        }.map(::toReminder)
    }

    override suspend fun findByUserId(userId: Long): List<Reminder> = DatabaseFactory.dbQuery {
        RemindersTable.selectAll().where { RemindersTable.userId eq userId }
            .map(::toReminder)
    }

    override suspend fun create(reminder: Reminder): Reminder = DatabaseFactory.dbQuery {
        val id = RemindersTable.insert {
            it[userId] = reminder.userId
            it[type] = reminder.type.name
            it[scheduledAt] = reminder.scheduledAt
            it[sent] = reminder.sent
            it[sentAt] = reminder.sentAt
        }[RemindersTable.id].value

        reminder.copy(id = id)
    }

    override suspend fun markAsSent(id: Long, sentAt: Instant): Boolean = DatabaseFactory.dbQuery {
        RemindersTable.update({ RemindersTable.id eq id }) {
            it[sent] = true
            it[RemindersTable.sentAt] = sentAt
        } > 0
    }

    override suspend fun delete(id: Long): Boolean = DatabaseFactory.dbQuery {
        RemindersTable.deleteWhere { RemindersTable.id eq id } > 0
    }

    override suspend fun deleteByUserAndType(userId: Long, type: ReminderType): Boolean = DatabaseFactory.dbQuery {
        RemindersTable.deleteWhere {
            (RemindersTable.userId eq userId) and (RemindersTable.type eq type.name)
        } > 0
    }

    private fun toReminder(row: ResultRow): Reminder = Reminder(
        id = row[RemindersTable.id].value,
        userId = row[RemindersTable.userId].value,
        type = ReminderType.valueOf(row[RemindersTable.type]),
        scheduledAt = row[RemindersTable.scheduledAt],
        sent = row[RemindersTable.sent],
        sentAt = row[RemindersTable.sentAt]
    )
}
