package com.turki.admin.web.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

enum class ButtonTone {
    Primary,
    Danger,
    Ghost,
    Soft
}

enum class StatusTone {
    Active,
    Inactive
}

@Composable
fun AppButton(
    label: String,
    onClick: () -> Unit,
    tone: ButtonTone = ButtonTone.Primary,
    leadingIcon: String? = null
) {
    val toneClass = when (tone) {
        ButtonTone.Primary -> AppStyles.buttonPrimary
        ButtonTone.Danger -> AppStyles.buttonDanger
        ButtonTone.Ghost -> AppStyles.buttonGhost
        ButtonTone.Soft -> AppStyles.buttonSoft
    }

    Button(attrs = {
        onClick { onClick() }
        classes(AppStyles.button, toneClass)
    }) {
        if (leadingIcon != null) {
            Span { Text(leadingIcon) }
        }
        Text(label)
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    hint: String,
    accentColor: String,
    delayMs: Int = 0
) {
    Div(attrs = {
        classes(AppStyles.statCard)
        style {
            property("--stat-accent", accentColor)
            property("--stagger-delay", "${delayMs}ms")
        }
    }) {
        Span({ classes(AppStyles.statLabel) }) { Text(label) }
        Span({ classes(AppStyles.statValue) }) { Text(value) }
        Span({ classes(AppStyles.statHint) }) { Text(hint) }
    }
}

@Composable
fun StatusPill(
    text: String,
    tone: StatusTone
) {
    val toneClass = if (tone == StatusTone.Active) {
        AppStyles.pillActive
    } else {
        AppStyles.pillInactive
    }

    Span({ classes(AppStyles.pill, toneClass) }) {
        Text(text)
    }
}

@Composable
fun LoadingState(label: String) {
    Div({ classes(AppStyles.loadingState) }) {
        Div({ classes(AppStyles.spinner) })
        Text(label)
    }
}

@Composable
fun ToggleSwitch(
    checked: Boolean,
    onToggle: () -> Unit,
    ariaLabel: String
) {
    Label(attrs = { classes(AppStyles.switchContainer) }) {
        Input(InputType.Checkbox) {
            classes(AppStyles.switchInput)
            checked(checked)
            onChange { onToggle() }
            attr("aria-label", ariaLabel)
        }
        Span({ classes(AppStyles.switchTrack) }) {
            Span({ classes(AppStyles.switchThumb) })
        }
    }
}
