package com.turki.admin.web

import com.turki.admin.common.di.initKoin
import com.turki.admin.web.ui.WebApp
import org.jetbrains.compose.web.renderComposable

fun main() {
    initKoin()
    
    renderComposable(rootElementId = "root") {
        WebApp()
    }
}
