package com.turki.admin.web.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.turki.admin.common.Screen
import com.turki.admin.web.ui.screens.LessonsScreen
import com.turki.admin.web.ui.screens.UsersScreen
import org.jetbrains.compose.web.dom.Div


@Composable
fun WebApp() {
    var currentScreen by remember { mutableStateOf(Screen.USERS) }

    WebTheme {
        Div({
            classes(AppStyles.appRoot)
        }) {
            Div({
                classes(AppStyles.appShell)
            }) {
                Sidebar(currentScreen) { currentScreen = it }

                Div({
                    classes(AppStyles.mainContent)
                }) {
                    when (currentScreen) {
                        Screen.USERS -> UsersScreen()
                        Screen.LESSONS -> LessonsScreen()
                    }
                }
            }
        }
    }
}
