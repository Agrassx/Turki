package com.turki.admin

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.turki.admin.di.adminModule
import com.turki.core.database.DatabaseFactory
import com.turki.core.di.coreModule
import org.koin.core.context.startKoin

fun main() = application {
    startKoin {
        modules(coreModule, adminModule)
    }

    val dbPath = System.getenv("DB_PATH") ?: "data/turki.db"
    DatabaseFactory.init(dbPath)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Turki Admin Panel",
        state = rememberWindowState(width = 1200.dp, height = 800.dp)
    ) {
        App()
    }
}
