package com.turki.core.repository

import com.turki.core.domain.UserDictionaryEntry

interface UserDictionaryRepository {
    suspend fun findByUserAndVocabulary(userId: Long, vocabularyId: Int): UserDictionaryEntry?
    suspend fun findByUser(userId: Long): List<UserDictionaryEntry>
    suspend fun upsert(entry: UserDictionaryEntry): UserDictionaryEntry
    suspend fun listFavorites(userId: Long, limit: Int = 100): List<UserDictionaryEntry>
    suspend fun deleteByUser(userId: Long): Boolean
}
