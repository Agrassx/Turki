package com.turki.bot.service

import com.turki.core.domain.User
import com.turki.core.repository.AnalyticsRepository
import com.turki.core.repository.HomeworkRepository
import com.turki.core.repository.LessonRepository
import com.turki.core.repository.ReminderPreferenceRepository
import com.turki.core.repository.ReminderRepository
import com.turki.core.repository.ReviewRepository
import com.turki.core.repository.UserDictionaryRepository
import com.turki.core.repository.UserCustomDictionaryRepository
import com.turki.core.repository.UserProgressRepository
import com.turki.core.repository.UserRepository
import com.turki.core.repository.UserStateRepository
import com.turki.core.repository.UserStatsRepository
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class UserDataExport(
    val exportDate: String,
    val profile: ProfileExport,
    val progress: List<ProgressExport>,
    val dictionary: List<DictionaryExport>,
    val customDictionary: List<CustomDictionaryExport>,
    val homeworkSubmissions: List<HomeworkSubmissionExport>,
    val reminderPreferences: ReminderPreferencesExport?,
    val stats: StatsExport?
)

@Serializable
data class ProfileExport(
    val telegramId: Long,
    val username: String?,
    val firstName: String,
    val lastName: String?,
    val language: String,
    val currentLessonId: Int,
    val createdAt: String
)

@Serializable
data class ProgressExport(
    val lessonId: Int,
    val lessonTitle: String,
    val status: String,
    val updatedAt: String
)

@Serializable
data class DictionaryExport(
    val word: String,
    val translation: String,
    val pronunciation: String?,
    val addedAt: String
)

@Serializable
data class CustomDictionaryExport(
    val word: String,
    val translation: String,
    val pronunciation: String?,
    val example: String?,
    val addedAt: String
)

@Serializable
data class HomeworkSubmissionExport(
    val lessonId: Int,
    val score: Int,
    val maxScore: Int,
    val submittedAt: String
)

@Serializable
data class ReminderPreferencesExport(
    val daysOfWeek: String,
    val timeLocal: String,
    val isEnabled: Boolean
)

@Serializable
data class StatsExport(
    val currentStreak: Int,
    val weeklyLessons: Int,
    val weeklyPractice: Int,
    val weeklyReview: Int,
    val weeklyHomework: Int
)

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
    private val userStatsRepository: UserStatsRepository,
    private val lessonRepository: LessonRepository,
    private val clock: Clock = Clock.System
) {
    private val json = Json { prettyPrint = true }

    suspend fun exportUserData(user: User): String {
        val progress = userProgressRepository.findByUser(user.id)
        val lessons = lessonRepository.findAll().associateBy { it.id }
        val dictionary = userDictionaryRepository.findByUser(user.id)
        val vocabularyIds = dictionary.map { it.vocabularyId }
        val vocabulary = if (vocabularyIds.isNotEmpty()) {
            lessonRepository.findVocabularyByIds(vocabularyIds)
        } else {
            emptyList()
        }
        val vocabularyMap = vocabulary.associateBy { it.id }
        val customDictionary = userCustomDictionaryRepository.listByUser(user.id)
        val submissions = homeworkRepository.findSubmissionsByUser(user.id)
        val reminderPref = reminderPreferenceRepository.findByUserId(user.id)
        val stats = userStatsRepository.findByUserId(user.id)

        val export = UserDataExport(
            exportDate = clock.now().toString(),
            profile = ProfileExport(
                telegramId = user.telegramId,
                username = user.username,
                firstName = user.firstName,
                lastName = user.lastName,
                language = user.language.name,
                currentLessonId = user.currentLessonId,
                createdAt = user.createdAt.toString()
            ),
            progress = progress.map { p ->
                ProgressExport(
                    lessonId = p.lessonId,
                    lessonTitle = lessons[p.lessonId]?.title ?: "Unknown",
                    status = p.status,
                    updatedAt = p.updatedAt.toString()
                )
            },
            dictionary = dictionary.mapNotNull { d ->
                vocabularyMap[d.vocabularyId]?.let { v ->
                    DictionaryExport(
                        word = v.word,
                        translation = v.translation,
                        pronunciation = v.pronunciation,
                        addedAt = d.addedAt.toString()
                    )
                }
            },
            customDictionary = customDictionary.map { c ->
                CustomDictionaryExport(
                    word = c.word,
                    translation = c.translation,
                    pronunciation = c.pronunciation,
                    example = c.example,
                    addedAt = c.addedAt.toString()
                )
            },
            homeworkSubmissions = submissions.map { s ->
                HomeworkSubmissionExport(
                    lessonId = s.homeworkId,
                    score = s.score,
                    maxScore = s.maxScore,
                    submittedAt = s.submittedAt.toString()
                )
            },
            reminderPreferences = reminderPref?.let {
                ReminderPreferencesExport(
                    daysOfWeek = it.daysOfWeek,
                    timeLocal = it.timeLocal,
                    isEnabled = it.isEnabled
                )
            },
            stats = stats?.let {
                StatsExport(
                    currentStreak = it.currentStreak,
                    weeklyLessons = it.weeklyLessons,
                    weeklyPractice = it.weeklyPractice,
                    weeklyReview = it.weeklyReview,
                    weeklyHomework = it.weeklyHomework
                )
            }
        )

        return json.encodeToString(export)
    }

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
