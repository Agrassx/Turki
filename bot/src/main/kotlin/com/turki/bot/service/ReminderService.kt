package com.turki.bot.service

import com.turki.core.domain.Reminder
import com.turki.core.domain.ReminderType
import com.turki.core.repository.ReminderRepository
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.Duration.Companion.hours

class ReminderService(private val reminderRepository: ReminderRepository) {

    suspend fun createLessonReminder(userId: Long, scheduledAt: Instant): Reminder {
        val reminder = Reminder(
            id = 0,
            userId = userId,
            type = ReminderType.LESSON_REMINDER,
            scheduledAt = scheduledAt
        )
        return reminderRepository.create(reminder)
    }

    suspend fun createHomeworkReminder(userId: Long): Reminder {
        val scheduledAt = Clock.System.now() + 24.hours
        val reminder = Reminder(
            id = 0,
            userId = userId,
            type = ReminderType.HOMEWORK_REMINDER,
            scheduledAt = scheduledAt
        )
        return reminderRepository.create(reminder)
    }

    suspend fun getPendingReminders(): List<Reminder> {
        val now = Clock.System.now()
        return reminderRepository.findPendingReminders(now)
    }

    suspend fun markReminderAsSent(reminderId: Long) {
        reminderRepository.markAsSent(reminderId, Clock.System.now())
    }

    suspend fun deleteReminder(reminderId: Long) {
        reminderRepository.delete(reminderId)
    }

    suspend fun cancelUserReminders(userId: Long, type: ReminderType) {
        reminderRepository.deleteByUserAndType(userId, type)
    }
}
