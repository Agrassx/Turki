package com.turki.admin.di

import com.turki.admin.viewmodel.LessonsViewModel
import com.turki.admin.viewmodel.UsersViewModel
import org.koin.dsl.module

val adminModule = module {
    factory { UsersViewModel(get()) }
    factory { LessonsViewModel(get()) }
}
