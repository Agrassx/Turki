package com.turki.core.repository

import com.turki.core.domain.Reminder
import com.turki.core.domain.ReminderType
import kotlinx.datetime.Instant

/**
 * Repository interface for reminder data access.
 *
 * This interface defines operations for managing user reminders, including:
 * - Reminder CRUD operations
 * - Finding pending reminders for scheduled sending
 * - Marking reminders as sent
 * - Filtering reminders by user and type
 *
 * All operations are coroutine-based and should be executed within database transactions.
 */
interface ReminderRepository {
    /**
     * Finds a reminder by its database ID.
     *
     * @param id The reminder's database ID
     * @return The [Reminder] if found, null otherwise
     */
    suspend fun findById(id: Long): Reminder?

    /**
     * Finds all reminders that are scheduled to be sent before a specific time.
     *
     * This is used by the reminder scheduler to find reminders that need to be sent.
     *
     * @param before The timestamp to find reminders scheduled before this time
     * @return List of [Reminder] objects that should be sent
     */
    suspend fun findPendingReminders(before: Instant): List<Reminder>

    /**
     * Finds all reminders for a specific user.
     *
     * @param userId The user's database ID
     * @return List of [Reminder] objects for the user
     */
    suspend fun findByUserId(userId: Long): List<Reminder>

    /**
     * Creates a new reminder in the database.
     *
     * @param reminder The reminder object to create
     * @return The created [Reminder] with assigned database ID
     */
    suspend fun create(reminder: Reminder): Reminder

    /**
     * Marks a reminder as sent and records the timestamp.
     *
     * @param id The reminder's database ID
     * @param sentAt The timestamp when the reminder was sent
     * @return true if the update was successful, false otherwise
     */
    suspend fun markAsSent(id: Long, sentAt: Instant): Boolean

    /**
     * Deletes a reminder from the database.
     *
     * @param id The reminder's database ID
     * @return true if the reminder was deleted, false otherwise
     */
    suspend fun delete(id: Long): Boolean

    /**
     * Deletes all reminders of a specific type for a user.
     *
     * This is useful for canceling all reminders of a certain type (e.g., all homework reminders).
     *
     * @param userId The user's database ID
     * @param type The type of reminders to delete
     * @return true if any reminders were deleted, false otherwise
     */
    suspend fun deleteByUserAndType(userId: Long, type: ReminderType): Boolean

    /**
     * Deletes all reminders for a specific user.
     *
     * @param userId The user's database ID
     * @return true if any reminders were deleted, false otherwise
     */
    suspend fun deleteByUser(userId: Long): Boolean
}
