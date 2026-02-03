package com.turki.admin.di

import com.turki.admin.common.api.AdminApi
import com.turki.admin.common.domain.Lesson
import com.turki.admin.common.domain.User
import com.turki.admin.common.viewmodel.LessonsViewModel
import com.turki.admin.common.viewmodel.UsersViewModel
import com.turki.admin.viewmodel.LessonsViewModel as DesktopLessonsViewModel
import com.turki.admin.viewmodel.UsersViewModel as DesktopUsersViewModel
import com.turki.core.domain.Language
import com.turki.core.domain.Lesson as CoreLesson
import com.turki.core.domain.User as CoreUser
import com.turki.core.domain.VocabularyItem as CoreVocabularyItem
import com.turki.core.repository.LessonRepository
import com.turki.core.repository.UserRepository
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days
import org.koin.dsl.module

class DirectAdminApi(
    private val userRepository: UserRepository,
    private val lessonRepository: LessonRepository
) : AdminApi {
    override suspend fun getUsers(): List<User> {
        return userRepository.findAll().map { coreUser ->
            User(
                id = coreUser.id,
                telegramId = coreUser.telegramId,
                username = coreUser.username,
                firstName = coreUser.firstName,
                lastName = coreUser.lastName,
                language = com.turki.admin.common.domain.Language.valueOf(coreUser.language.name),
                subscriptionActive = coreUser.subscriptionActive,
                subscriptionExpiresAt = coreUser.subscriptionExpiresAt,
                currentLessonId = coreUser.currentLessonId,
                createdAt = coreUser.createdAt,
                updatedAt = coreUser.updatedAt
            )
        }
    }

    override suspend fun getLessons(): List<Lesson> {
        return lessonRepository.findAll().map { coreLesson ->
            val vocabulary = lessonRepository.getVocabularyItems(coreLesson.id)
            Lesson(
                id = coreLesson.id,
                orderIndex = coreLesson.orderIndex,
                targetLanguage = com.turki.admin.common.domain.Language.valueOf(coreLesson.targetLanguage.name),
                title = coreLesson.title,
                description = coreLesson.description,
                content = coreLesson.content,
                vocabularyItems = vocabulary.map { vocab ->
                    com.turki.admin.common.domain.VocabularyItem(
                        id = vocab.id,
                        lessonId = vocab.lessonId,
                        word = vocab.word,
                        translation = vocab.translation,
                        pronunciation = vocab.pronunciation,
                        example = vocab.example
                    )
                }
            )
        }
    }

    override suspend fun toggleSubscription(userId: Long, active: Boolean) {
        val expiresAt = if (active) Clock.System.now() + 30.days else null
        userRepository.updateSubscription(userId, active, expiresAt)
    }

    override suspend fun resetUserProgress(userId: Long) {
        userRepository.resetProgress(userId)
    }

    override suspend fun resetAllProgress() {
        userRepository.resetAllProgress()
    }
}

val desktopAdminModule = module {
    single<AdminApi> {
        DirectAdminApi(get(), get())
    }
    factory<com.turki.admin.viewmodel.UsersViewModel> { DesktopUsersViewModel(get()) }
    factory<com.turki.admin.viewmodel.LessonsViewModel> { DesktopLessonsViewModel(get()) }
}
