package com.turki.bot.service

import com.turki.core.repository.AnalyticsRepository
import com.turki.core.repository.HomeworkRepository
import com.turki.core.repository.ReminderPreferenceRepository
import com.turki.core.repository.ReminderRepository
import com.turki.core.repository.ReviewRepository
import com.turki.core.repository.UserDictionaryRepository
import com.turki.core.repository.UserCustomDictionaryRepository
import com.turki.core.repository.UserProgressRepository
import com.turki.core.repository.UserRepository
import com.turki.core.repository.UserStateRepository
import com.turki.core.repository.UserStatsRepository

class UserDataService(
    private val userRepository: UserRepository,
    private val userStateRepository: UserStateRepository,
    private val userProgressRepository: UserProgressRepository,
    private val userDictionaryRepository: UserDictionaryRepository,
    private val reviewRepository: ReviewRepository,
    private val userCustomDictionaryRepository: UserCustomDictionaryRepository,
    private val reminderPreferenceRepository: ReminderPreferenceRepository,
    private val reminderRepository: ReminderRepository,
    private val homeworkRepository: HomeworkRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val userStatsRepository: UserStatsRepository
) {
    suspend fun deleteUserData(userId: Long) {
        userStateRepository.clear(userId)
        userProgressRepository.deleteByUser(userId)
        userDictionaryRepository.deleteByUser(userId)
        userCustomDictionaryRepository.deleteByUser(userId)
        reviewRepository.deleteByUser(userId)
        reminderPreferenceRepository.deleteByUser(userId)
        reminderRepository.deleteByUser(userId)
        homeworkRepository.deleteSubmissionsByUser(userId)
        analyticsRepository.deleteByUser(userId)
        userStatsRepository.deleteByUser(userId)
        userRepository.delete(userId)
    }
}
