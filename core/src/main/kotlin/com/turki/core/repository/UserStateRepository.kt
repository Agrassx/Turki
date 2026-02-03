package com.turki.core.repository

import com.turki.core.domain.UserState

interface UserStateRepository {
    suspend fun findByUserId(userId: Long): UserState?
    suspend fun upsert(userId: Long, state: String, payload: String): UserState
    suspend fun clear(userId: Long): Boolean
}
