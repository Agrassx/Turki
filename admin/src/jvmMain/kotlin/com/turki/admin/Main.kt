package com.turki.admin

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.turki.admin.di.desktopAdminModule
import com.turki.core.database.DatabaseFactory
import com.turki.core.di.coreModule
import org.koin.core.context.startKoin

fun main() = application {
    startKoin {
        modules(coreModule, desktopAdminModule)
    }

    val dbUrl = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/turki"
    val dbUser = System.getenv("DB_USER") ?: "turki"
    val dbPassword = System.getenv("DB_PASSWORD")
        ?: error("DB_PASSWORD environment variable is required")
    DatabaseFactory.init(dbUrl, dbUser, dbPassword)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Turki Admin Panel",
        state = rememberWindowState(width = 1200.dp, height = 800.dp)
    ) {
        App()
    }
}
