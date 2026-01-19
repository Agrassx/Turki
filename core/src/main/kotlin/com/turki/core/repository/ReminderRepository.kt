package com.turki.core.repository

import com.turki.core.domain.Reminder
import com.turki.core.domain.ReminderType
import kotlin.time.Instant

interface ReminderRepository {
    suspend fun findById(id: Long): Reminder?
    suspend fun findPendingReminders(before: Instant): List<Reminder>
    suspend fun findByUserId(userId: Long): List<Reminder>
    suspend fun create(reminder: Reminder): Reminder
    suspend fun markAsSent(id: Long, sentAt: Instant): Boolean
    suspend fun delete(id: Long): Boolean
    suspend fun deleteByUserAndType(userId: Long, type: ReminderType): Boolean
}
