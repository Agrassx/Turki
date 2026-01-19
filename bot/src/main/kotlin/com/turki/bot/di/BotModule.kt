package com.turki.bot.di

import com.turki.bot.service.HomeworkService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ReminderService
import com.turki.bot.service.UserService
import org.koin.dsl.module

val botModule = module {
    single { UserService(get()) }
    single { LessonService(get()) }
    single { HomeworkService(get(), get()) }
    single { ReminderService(get()) }
}
