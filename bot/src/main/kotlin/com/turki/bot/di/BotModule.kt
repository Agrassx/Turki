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
import org.koin.dsl.module

val botModule = module {
    single { UserService(get()) }
    single { LessonService(get()) }
    single { HomeworkService(get(), get()) }
    single { ReminderService(get()) }
    single { UserStateService(get()) }
    single { ExerciseService(get()) }
    single { ProgressService(get(), get(), get()) }
    single { DictionaryService(get(), get(), get()) }
    single { ReviewService(get(), get(), get(), get()) }
    single { ReminderPreferenceService(get()) }
    single { AnalyticsService(get()) }
    single { UserDataService(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { SupportService() }
    single { MetricsService(get(), get(), get()) }
}
