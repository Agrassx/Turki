package com.turki.admin.web.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.turki.admin.common.domain.User
import com.turki.admin.common.viewmodel.UsersViewModel
import com.turki.admin.web.ui.AppButton
import com.turki.admin.web.ui.AppStyles
import com.turki.admin.web.ui.ButtonTone
import com.turki.admin.web.ui.LoadingState
import com.turki.admin.web.ui.StatCard
import com.turki.admin.web.ui.StatusPill
import com.turki.admin.web.ui.StatusTone
import com.turki.admin.web.ui.ToggleSwitch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.koin.compose.koinInject

@Composable
fun UsersScreen(
    viewModel: UsersViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    Div({ classes(AppStyles.page) }) {
        Div({ classes(AppStyles.pageHeader) }) {
            Div {
                H2({ classes(AppStyles.pageTitle) }) {
                    Text("Пользователи")
                }
                Span({ classes(AppStyles.pageSubtitle) }) {
                    Text("Управление подписками и прогрессом студентов")
                }
            }

            Div({ classes(AppStyles.actionsRow) }) {
                AppButton(
                    label = "Обновить",
                    onClick = { viewModel.loadUsers() },
                    tone = ButtonTone.Primary,
                    leadingIcon = "🔄"
                )
                AppButton(
                    label = "Сбросить прогресс всех",
                    onClick = { viewModel.resetAllProgress() },
                    tone = ButtonTone.Danger,
                    leadingIcon = "🗑"
                )
            }
        }

        Div({ classes(AppStyles.statsGrid) }) {
            StatCard(
                label = "Всего пользователей",
                value = state.users.size.toString(),
                hint = "в системе",
                accentColor = "#4BC6B9",
                delayMs = 0
            )
            StatCard(
                label = "Активные подписки",
                value = state.users.count { it.subscriptionActive }.toString(),
                hint = "прямо сейчас",
                accentColor = "#3BA4E7",
                delayMs = 80
            )
        }

        if (state.isLoading) {
            LoadingState("Загрузка пользователей...")
        } else {
            Div({ classes(AppStyles.cardList) }) {
                state.users.forEachIndexed { index, user ->
                    UserCard(
                        user = user,
                        delayMs = index * 40,
                        onToggleSubscription = { viewModel.toggleSubscription(user) },
                        onResetProgress = { viewModel.resetUserProgress(user) }
                    )
                }
            }
        }
    }
}

@Composable
private fun UserCard(
    user: User,
    delayMs: Int,
    onToggleSubscription: () -> Unit,
    onResetProgress: () -> Unit
) {
    val statusTone = if (user.subscriptionActive) StatusTone.Active else StatusTone.Inactive
    val statusLabel = if (user.subscriptionActive) "Активна" else "Неактивна"

    Div(attrs = {
        classes(AppStyles.card, AppStyles.cardSplit)
        style { property("--stagger-delay", "${delayMs}ms") }
    }) {
        Div({ classes(AppStyles.cardMain) }) {
            Span({ classes(AppStyles.cardTitle) }) {
                Text("${user.firstName} ${user.lastName ?: ""}")
            }
            Span({ classes(AppStyles.cardMeta) }) {
                Text("@${user.username ?: "no username"} • ID: ${user.telegramId}")
            }
            Div({ classes(AppStyles.metaRow) }) {
                Span({ classes(AppStyles.cardMeta) }) {
                    Text("Урок: ${user.currentLessonId}")
                }
                Span({ classes(AppStyles.cardMeta) }) {
                    Text("Регистрация: ${formatDate(user.createdAt)}")
                }
            }
        }

        Div({ classes(AppStyles.columnActions) }) {
            AppButton(
                label = "Сброс",
                onClick = onResetProgress,
                tone = ButtonTone.Soft,
                leadingIcon = "↩"
            )

            Div({ classes(AppStyles.switchWrap) }) {
                StatusPill(text = statusLabel, tone = statusTone)
                ToggleSwitch(
                    checked = user.subscriptionActive,
                    onToggle = onToggleSubscription,
                    ariaLabel = "Подписка пользователя ${user.telegramId}"
                )
            }
        }
    }
}

private fun formatDate(instant: kotlinx.datetime.Instant): String {
    val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.dayOfMonth.toString().padStart(2, '0')}.${
        dt.monthNumber.toString().padStart(2, '0')
    }.${dt.year}"
}
