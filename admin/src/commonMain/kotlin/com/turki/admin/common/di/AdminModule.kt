package com.turki.admin.common.di

import com.turki.admin.common.api.AdminApi
import com.turki.admin.common.viewmodel.LessonsViewModel
import com.turki.admin.common.viewmodel.UsersViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

expect fun provideAdminApi(): AdminApi

val adminModule: Module = module {
    single<AdminApi> { provideAdminApi() }
    factory { UsersViewModel(get()) }
    factory { LessonsViewModel(get()) }
}

fun initKoin() {
    org.koin.core.context.startKoin {
        modules(adminModule)
    }
}
