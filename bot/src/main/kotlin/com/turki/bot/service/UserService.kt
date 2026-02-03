package com.turki.bot.service

import com.turki.core.domain.Language
import com.turki.core.domain.User
import com.turki.core.repository.UserRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Service for managing user accounts and user-related operations.
 *
 * This service provides high-level operations for user management including:
 * - User creation and retrieval
 * - Subscription management
 * - Progress tracking
 * - Language preferences
 *
 * All operations are coroutine-based and thread-safe.
 */
class UserService(private val userRepository: UserRepository) {

    /**
     * Finds an existing user by Telegram ID or creates a new one.
     *
     * This is the primary method for user registration. If a user with the given
     * Telegram ID exists, it returns that user. Otherwise, creates a new user
     * with the provided information.
     *
     * @param telegramId The unique Telegram user ID
     * @param username Optional Telegram username (without @)
     * @param firstName User's first name
     * @param lastName Optional user's last name
     * @return The existing or newly created [User]
     *
     * @sample
     * ```
     * val user = userService.findOrCreateUser(
     *     telegramId = 123456789L,
     *     username = "johndoe",
     *     firstName = "John",
     *     lastName = "Doe"
     * )
     * ```
     */
    suspend fun findOrCreateUser(
        telegramId: Long,
        username: String?,
        firstName: String,
        lastName: String?
    ): User {
        return userRepository.findByTelegramId(telegramId)
            ?: createUser(telegramId, username, firstName, lastName)
    }

    private suspend fun createUser(
        telegramId: Long,
        username: String?,
        firstName: String,
        lastName: String?
    ): User {
        val now = Clock.System.now()
        val user = User(
            id = 0,
            telegramId = telegramId,
            username = username,
            firstName = firstName,
            lastName = lastName,
            createdAt = now,
            updatedAt = now
        )
        return userRepository.create(user)
    }

    /**
     * Finds a user by their Telegram ID.
     *
     * @param telegramId The Telegram user ID to search for
     * @return The [User] if found, null otherwise
     */
    suspend fun findByTelegramId(telegramId: Long): User? =
        userRepository.findByTelegramId(telegramId)

    /**
     * Updates a user's subscription status.
     *
     * @param userId The user's database ID
     * @param active Whether the subscription is active
     * @param expiresAt Optional expiration timestamp for the subscription
     */
    suspend fun updateSubscription(userId: Long, active: Boolean, expiresAt: Instant?) =
        userRepository.updateSubscription(userId, active, expiresAt)

    /**
     * Updates the current lesson ID for a user.
     *
     * This is used to track user progress through lessons.
     *
     * @param userId The user's database ID
     * @param lessonId The new current lesson ID
     */
    suspend fun updateCurrentLesson(userId: Long, lessonId: Int) =
        userRepository.updateCurrentLesson(userId, lessonId)

    /**
     * Updates the user's preferred language.
     *
     * @param userId The user's database ID
     * @param language The new language preference
     */
    suspend fun updateLanguage(userId: Long, language: Language) =
        userRepository.updateLanguage(userId, language)

    /**
     * Retrieves all users in the system.
     *
     * @return List of all [User] records
     */
    suspend fun getAllUsers(): List<User> = userRepository.findAll()

    /**
     * Retrieves all users with active subscriptions.
     *
     * @return List of [User] records with active subscriptions
     */
    suspend fun getActiveSubscribers(): List<User> = userRepository.findActiveSubscribers()

    /**
     * Resets a user's progress to the beginning.
     *
     * This operation:
     * - Sets current lesson ID to 1
     * - Deletes all homework submissions
     * - Updates the user's updatedAt timestamp
     *
     * @param userId The user's database ID
     */
    suspend fun resetProgress(userId: Long) = userRepository.resetProgress(userId)

    /**
     * Deletes a user account and basic profile data.
     *
     * @param userId The user's database ID
     */
    suspend fun deleteUser(userId: Long) = userRepository.delete(userId)
}
