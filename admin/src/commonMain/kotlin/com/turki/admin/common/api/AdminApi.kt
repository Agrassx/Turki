package com.turki.admin.common.api

import com.turki.admin.common.domain.Lesson
import com.turki.admin.common.domain.User

interface AdminApi {
    suspend fun getUsers(): List<User>
    suspend fun getLessons(): List<Lesson>
    suspend fun toggleSubscription(userId: Long, active: Boolean)
    suspend fun resetUserProgress(userId: Long)
    suspend fun resetAllProgress()
}
