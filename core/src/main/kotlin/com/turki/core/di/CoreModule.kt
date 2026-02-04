package com.turki.core.di

import com.turki.core.database.HomeworkRepositoryImpl
import com.turki.core.database.LessonRepositoryImpl
import com.turki.core.database.AnalyticsRepositoryImpl
import com.turki.core.database.ReminderPreferenceRepositoryImpl
import com.turki.core.database.ReminderRepositoryImpl
import com.turki.core.database.UserRepositoryImpl
import com.turki.core.database.UserDictionaryRepositoryImpl
import com.turki.core.database.UserCustomDictionaryRepositoryImpl
import com.turki.core.database.UserProgressRepositoryImpl
import com.turki.core.database.UserStateRepositoryImpl
import com.turki.core.database.UserStatsRepositoryImpl
import com.turki.core.database.ReviewRepositoryImpl
import com.turki.core.database.MetricsRepositoryImpl
import com.turki.core.database.SubscriptionRepositoryImpl
import com.turki.core.repository.AnalyticsRepository
import com.turki.core.repository.MetricsRepository
import com.turki.core.repository.SubscriptionRepository
import com.turki.core.repository.HomeworkRepository
import com.turki.core.repository.LessonRepository
import com.turki.core.repository.ReminderPreferenceRepository
import com.turki.core.repository.ReminderRepository
import com.turki.core.repository.UserRepository
import com.turki.core.repository.UserDictionaryRepository
import com.turki.core.repository.UserCustomDictionaryRepository
import com.turki.core.repository.UserProgressRepository
import com.turki.core.repository.UserStateRepository
import com.turki.core.repository.UserStatsRepository
import com.turki.core.repository.ReviewRepository
import com.turki.core.service.DataImportService
import org.koin.dsl.module

val coreModule = module {
    single<UserRepository> { UserRepositoryImpl() }
    single<LessonRepository> { LessonRepositoryImpl() }
    single<HomeworkRepository> { HomeworkRepositoryImpl() }
    single<ReminderRepository> { ReminderRepositoryImpl() }
    single<UserStateRepository> { UserStateRepositoryImpl() }
    single<UserProgressRepository> { UserProgressRepositoryImpl() }
    single<UserDictionaryRepository> { UserDictionaryRepositoryImpl() }
    single<UserCustomDictionaryRepository> { UserCustomDictionaryRepositoryImpl() }
    single<ReviewRepository> { ReviewRepositoryImpl() }
    single<ReminderPreferenceRepository> { ReminderPreferenceRepositoryImpl() }
    single<AnalyticsRepository> { AnalyticsRepositoryImpl() }
    single<UserStatsRepository> { UserStatsRepositoryImpl() }
    single<SubscriptionRepository> { SubscriptionRepositoryImpl() }
    single<MetricsRepository> { MetricsRepositoryImpl() }
    single { DataImportService() }
}
