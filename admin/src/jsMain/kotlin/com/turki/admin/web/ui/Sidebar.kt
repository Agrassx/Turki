package com.turki.admin.web.ui

import androidx.compose.runtime.Composable
import com.turki.admin.common.Screen
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun Sidebar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    Div({ classes(AppStyles.sidebar) }) {
        Div({ classes(AppStyles.sidebarBrand) }) {
            Span({ classes(AppStyles.sidebarTitle) }) {
                Text("🇹🇷 Turki Admin")
            }
            Span({ classes(AppStyles.sidebarSubtitle) }) {
                Text("control room")
            }
        }

        SidebarItem(
            title = "Пользователи",
            icon = "👥",
            isSelected = currentScreen == Screen.USERS,
            onClick = { onScreenSelected(Screen.USERS) }
        )

        SidebarItem(
            title = "Уроки",
            icon = "📚",
            isSelected = currentScreen == Screen.LESSONS,
            onClick = { onScreenSelected(Screen.LESSONS) }
        )

        Div({ classes(AppStyles.navSpacer) })

        Div({ classes(AppStyles.sidebarFooter) }) {
            Text("v1.0.0")
        }
    }
}

@Composable
private fun SidebarItem(
    title: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val classes = if (isSelected) {
        arrayOf(AppStyles.navItem, AppStyles.navItemActive)
    } else {
        arrayOf(AppStyles.navItem)
    }

    Div({
        classes(*classes)
        onClick { onClick() }
    }) {
        Span { Text(icon) }
        Text(title)
    }
}
