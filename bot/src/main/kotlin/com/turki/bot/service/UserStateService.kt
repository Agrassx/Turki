package com.turki.bot.service

import com.turki.core.domain.UserState
import com.turki.core.repository.UserStateRepository

class UserStateService(
    private val userStateRepository: UserStateRepository
) {
    suspend fun get(userId: Long): UserState? = userStateRepository.findByUserId(userId)

    suspend fun set(userId: Long, state: String, payload: String): UserState =
        userStateRepository.upsert(userId, state, payload)

    suspend fun clear(userId: Long): Boolean = userStateRepository.clear(userId)
}
