package com.turki.admin.common.di

import com.turki.admin.common.api.AdminApi

actual fun provideAdminApi(): AdminApi {
    // This will be provided via Koin in desktopAdminModule
    throw UnsupportedOperationException("AdminApi should be provided via Koin in desktopAdminModule")
}
