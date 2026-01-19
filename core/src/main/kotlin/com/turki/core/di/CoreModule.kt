package com.turki.core.di

import com.turki.core.database.HomeworkRepositoryImpl
import com.turki.core.database.LessonRepositoryImpl
import com.turki.core.database.ReminderRepositoryImpl
import com.turki.core.database.UserRepositoryImpl
import com.turki.core.repository.HomeworkRepository
import com.turki.core.repository.LessonRepository
import com.turki.core.repository.ReminderRepository
import com.turki.core.repository.UserRepository
import org.koin.dsl.module

val coreModule = module {
    single<UserRepository> { UserRepositoryImpl() }
    single<LessonRepository> { LessonRepositoryImpl() }
    single<HomeworkRepository> { HomeworkRepositoryImpl() }
    single<ReminderRepository> { ReminderRepositoryImpl() }
}
