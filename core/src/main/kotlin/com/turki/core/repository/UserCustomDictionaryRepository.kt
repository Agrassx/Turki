package com.turki.core.repository

import com.turki.core.domain.UserCustomDictionaryEntry

interface UserCustomDictionaryRepository {
    suspend fun listByUser(userId: Long): List<UserCustomDictionaryEntry>
    suspend fun create(entry: UserCustomDictionaryEntry): UserCustomDictionaryEntry
    suspend fun deleteByUser(userId: Long): Boolean
}
