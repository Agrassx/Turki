package com.turki.core.repository

import com.turki.core.domain.ReminderPreference

interface ReminderPreferenceRepository {
    suspend fun findByUserId(userId: Long): ReminderPreference?
    suspend fun upsert(preference: ReminderPreference): ReminderPreference
    suspend fun updateLastFired(userId: Long): Boolean
    suspend fun deleteByUser(userId: Long): Boolean
}
