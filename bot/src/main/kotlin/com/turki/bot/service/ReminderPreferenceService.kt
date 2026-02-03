package com.turki.bot.service

import com.turki.core.domain.ReminderPreference
import com.turki.core.repository.ReminderPreferenceRepository
import kotlinx.datetime.Clock

class ReminderPreferenceService(
    private val reminderPreferenceRepository: ReminderPreferenceRepository
) {
    suspend fun getOrDefault(userId: Long): ReminderPreference {
        return reminderPreferenceRepository.findByUserId(userId) ?: ReminderPreference(
            userId = userId,
            daysOfWeek = "MON,TUE,WED,THU,FRI",
            timeLocal = "19:00",
            isEnabled = false,
            lastFiredAt = null
        )
    }

    suspend fun setEnabled(userId: Long, enabled: Boolean): ReminderPreference {
        val current = getOrDefault(userId)
        val next = current.copy(isEnabled = enabled)
        return reminderPreferenceRepository.upsert(next)
    }

    suspend fun setSchedule(userId: Long, daysOfWeek: String, timeLocal: String): ReminderPreference {
        val current = getOrDefault(userId)
        val next = current.copy(daysOfWeek = daysOfWeek, timeLocal = timeLocal, isEnabled = true)
        return reminderPreferenceRepository.upsert(next)
    }

    suspend fun markFired(userId: Long) {
        reminderPreferenceRepository.upsert(
            getOrDefault(userId).copy(lastFiredAt = Clock.System.now())
        )
    }
}
