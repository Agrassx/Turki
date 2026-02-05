package com.turki.bot.di

import com.turki.bot.service.HomeworkService
import com.turki.bot.service.LessonService
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.DictionaryService
import com.turki.bot.service.ExerciseService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.ReminderPreferenceService
import com.turki.bot.service.ReminderService
import com.turki.bot.service.MetricsService
import com.turki.bot.service.ReviewService
import com.turki.bot.service.SupportService
import com.turki.bot.service.UserDataService
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import kotlinx.datetime.TimeZone
import kotlin.random.Random
import org.koin.dsl.module

val botModule = module {
    single { TimeZone.currentSystemDefault() }
    single<Random> { Random.Default }
    single { UserService(get(), get()) }
    single { LessonService(get()) }
    single { HomeworkService(get(), get(), get()) }
    single { ReminderService(get(), get()) }
    single { UserStateService(get()) }
    single { ExerciseService(get(), get()) }
    single { ProgressService(get(), get(), get(), get(), get()) }
    single { DictionaryService(get(), get(), get(), get()) }
    single { ReviewService(get(), get(), get(), get(), get(), get(), get()) }
    single { ReminderPreferenceService(get(), get()) }
    single { AnalyticsService(get(), get()) }
    single { UserDataService(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { SupportService() }
    single { MetricsService(get(), get(), get(), get()) }
}
