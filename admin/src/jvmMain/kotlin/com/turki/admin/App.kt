package com.turki.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.turki.admin.common.Screen
import com.turki.admin.ui.LessonsScreen
import com.turki.admin.ui.Sidebar
import com.turki.admin.ui.UsersScreen

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6DD5FA),
    secondary = Color(0xFF2980B9),
    tertiary = Color(0xFF1ABC9C),
    background = Color(0xFF1A1A2E),
    surface = Color(0xFF16213E),
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFE8E8E8),
    onSurface = Color(0xFFE8E8E8)
)

@Composable
fun App() {
    var currentScreen by remember { mutableStateOf(Screen.USERS) }

    MaterialTheme(colorScheme = DarkColorScheme) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Sidebar(
                currentScreen = currentScreen,
                onScreenSelected = { currentScreen = it }
            )

            when (currentScreen) {
                Screen.USERS -> UsersScreen()
                Screen.LESSONS -> LessonsScreen()
            }
        }
    }
}
