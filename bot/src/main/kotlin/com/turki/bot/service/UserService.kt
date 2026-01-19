package com.turki.bot.service

import com.turki.core.domain.Language
import com.turki.core.domain.User
import com.turki.core.repository.UserRepository
import kotlin.time.Clock
import kotlin.time.Instant

class UserService(private val userRepository: UserRepository) {

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

    suspend fun findByTelegramId(telegramId: Long): User? =
        userRepository.findByTelegramId(telegramId)

    suspend fun updateSubscription(userId: Long, active: Boolean, expiresAt: Instant?) =
        userRepository.updateSubscription(userId, active, expiresAt)

    suspend fun updateCurrentLesson(userId: Long, lessonId: Int) =
        userRepository.updateCurrentLesson(userId, lessonId)

    suspend fun updateLanguage(userId: Long, language: Language) =
        userRepository.updateLanguage(userId, language)

    suspend fun getAllUsers(): List<User> = userRepository.findAll()

    suspend fun getActiveSubscribers(): List<User> = userRepository.findActiveSubscribers()
}
