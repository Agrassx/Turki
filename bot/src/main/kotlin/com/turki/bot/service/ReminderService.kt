package com.turki.bot.service

import com.turki.core.domain.Reminder
import com.turki.core.domain.ReminderType
import com.turki.core.repository.ReminderRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.hours

/**
 * Service for managing user reminders.
 *
 * This service handles creation, retrieval, and management of reminders for:
 * - Lesson reminders - Notify users to continue learning
 * - Homework reminders - Remind users to complete homework
 * - Subscription reminders - Notify about subscription expiration
 *
 * Reminders are scheduled at specific times and processed by the ReminderScheduler.
 * All operations are coroutine-based and thread-safe.
 */
class ReminderService(private val reminderRepository: ReminderRepository) {

    /**
     * Creates a lesson reminder for a user at a specific time.
     *
     * @param userId The user's database ID
     * @param scheduledAt The timestamp when the reminder should be sent
     * @return The created [Reminder]
     */
    suspend fun createLessonReminder(userId: Long, scheduledAt: Instant): Reminder {
        val reminder = Reminder(
            id = 0,
            userId = userId,
            type = ReminderType.LESSON_REMINDER,
            scheduledAt = scheduledAt
        )
        return reminderRepository.create(reminder)
    }

    /**
     * Creates a homework reminder for a user, scheduled 24 hours from now.
     *
     * @param userId The user's database ID
     * @return The created [Reminder] scheduled for 24 hours later
     */
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

    /**
     * Retrieves all reminders that are due to be sent.
     *
     * @return List of [Reminder] objects that should be sent now
     */
    suspend fun getPendingReminders(): List<Reminder> {
        val now = Clock.System.now()
        return reminderRepository.findPendingReminders(now)
    }

    /**
     * Marks a reminder as sent.
     *
     * @param reminderId The reminder's database ID
     */
    suspend fun markReminderAsSent(reminderId: Long) {
        reminderRepository.markAsSent(reminderId, Clock.System.now())
    }

    /**
     * Deletes a reminder from the database.
     *
     * @param reminderId The reminder's database ID
     */
    suspend fun deleteReminder(reminderId: Long) {
        reminderRepository.delete(reminderId)
    }

    /**
     * Cancels all reminders of a specific type for a user.
     *
     * @param userId The user's database ID
     * @param type The type of reminders to cancel
     */
    suspend fun cancelUserReminders(userId: Long, type: ReminderType) {
        reminderRepository.deleteByUserAndType(userId, type)
    }
}
