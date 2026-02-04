package com.turki.core.repository

import com.turki.core.domain.UserProgress

interface UserProgressRepository {
    suspend fun findByUserAndLesson(userId: Long, lessonId: Int): UserProgress?
    suspend fun findByUser(userId: Long): List<UserProgress>
    suspend fun upsert(progress: UserProgress): UserProgress
    suspend fun countCompleted(userId: Long): Long
    suspend fun listCompletedLessonIds(userId: Long): List<Int>
    suspend fun deleteByUser(userId: Long): Boolean
}
