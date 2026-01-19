package com.turki.core.repository

import com.turki.core.domain.Language
import com.turki.core.domain.User
import kotlin.time.Instant

interface UserRepository {
    suspend fun findById(id: Long): User?
    suspend fun findByTelegramId(telegramId: Long): User?
    suspend fun findAll(): List<User>
    suspend fun findActiveSubscribers(): List<User>
    suspend fun create(user: User): User
    suspend fun update(user: User): User
    suspend fun updateSubscription(userId: Long, active: Boolean, expiresAt: Instant?): Boolean
    suspend fun updateCurrentLesson(userId: Long, lessonId: Int): Boolean
    suspend fun updateLanguage(userId: Long, language: Language): Boolean
    suspend fun delete(id: Long): Boolean
    suspend fun count(): Long
}
