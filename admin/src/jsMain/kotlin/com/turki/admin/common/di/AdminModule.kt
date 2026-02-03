package com.turki.admin.common.di

import com.turki.admin.common.api.AdminApi
import com.turki.admin.web.api.HttpAdminApi

actual fun provideAdminApi(): AdminApi {
    return HttpAdminApi()
}
