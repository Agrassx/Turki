package com.turki.admin.web.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.Style

@Composable
fun WebTheme(content: @Composable () -> Unit) {
    Style(AppStyles)
    content()
}
