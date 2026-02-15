package com.turki.core.repository

import com.turki.core.domain.Language
import com.turki.core.domain.User
import kotlinx.datetime.Instant

/**
 * Repository interface for user data access.
 *
 * This interface defines operations for managing user accounts, including:
 * - User CRUD operations
 * - Subscription management
 * - Progress tracking
 * - Language preferences
 *
 * All operations are coroutine-based and should be executed within database transactions.
 */
interface UserRepository {
    /**
     * Finds a user by their database ID.
     *
     * @param id The user's database ID
     * @return The [User] if found, null otherwise
     */
    suspend fun findById(id: Long): User?

    /**
     * Finds a user by their Telegram ID.
     *
     * @param telegramId The Telegram user ID
     * @return The [User] if found, null otherwise
     */
    suspend fun findByTelegramId(telegramId: Long): User?

    /**
     * Retrieves all users in the system.
     *
     * @return List of all [User] records
     */
    suspend fun findAll(): List<User>

    /**
     * Retrieves all users with active subscriptions.
     *
     * @return List of [User] records with active subscriptions
     */
    suspend fun findActiveSubscribers(): List<User>

    /**
     * Creates a new user in the database.
     *
     * @param user The user object to create
     * @return The created [User] with assigned database ID
     */
    suspend fun create(user: User): User

    /**
     * Updates an existing user in the database.
     *
     * @param user The user object with updated data
     * @return The updated [User]
     */
    suspend fun update(user: User): User

    /**
     * Updates a user's subscription status.
     *
     * @param userId The user's database ID
     * @param active Whether the subscription is active
     * @param expiresAt Optional expiration timestamp for the subscription
     * @return true if the update was successful, false otherwise
     */
    suspend fun updateSubscription(userId: Long, active: Boolean, expiresAt: Instant?): Boolean

    /**
     * Updates the current lesson ID for a user.
     *
     * @param userId The user's database ID
     * @param lessonId The new current lesson ID
     * @return true if the update was successful, false otherwise
     */
    suspend fun updateCurrentLesson(userId: Long, lessonId: Int): Boolean

    /**
     * Updates the user's preferred language.
     *
     * @param userId The user's database ID
     * @param language The new language preference
     * @return true if the update was successful, false otherwise
     */
    suspend fun updateLanguage(userId: Long, language: Language): Boolean

    /**
     * Updates the user's timezone.
     *
     * @param userId The user's database ID
     * @param timezone IANA timezone string (e.g. "Europe/Moscow")
     * @return true if the update was successful, false otherwise
     */
    suspend fun updateTimezone(userId: Long, timezone: String): Boolean

    /**
     * Deletes a user from the database.
     *
     * @param id The user's database ID
     * @return true if the user was deleted, false otherwise
     */
    suspend fun delete(id: Long): Boolean

    /**
     * Counts the total number of users in the system.
     *
     * @return The total count of users
     */
    suspend fun count(): Long

    /**
     * Resets a user's progress to the beginning.
     *
     * This operation sets current lesson ID to 1 and deletes all homework submissions.
     *
     * @param userId The user's database ID
     * @return true if the reset was successful, false otherwise
     */
    suspend fun resetProgress(userId: Long): Boolean

    /**
     * Resets progress for all users in the system.
     *
     * This operation:
     * - Sets current lesson ID to 1 for all users
     * - Deletes all homework submissions
     * - Updates all users' updatedAt timestamps
     *
     * @return true if the operation was successful, false otherwise
     */
    suspend fun resetAllProgress(): Boolean
}
