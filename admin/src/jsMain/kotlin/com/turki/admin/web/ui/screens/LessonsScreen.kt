package com.turki.admin.web.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.turki.admin.common.domain.Lesson
import com.turki.admin.common.viewmodel.LessonsViewModel
import com.turki.admin.web.ui.AppButton
import com.turki.admin.web.ui.AppStyles
import com.turki.admin.web.ui.ButtonTone
import com.turki.admin.web.ui.LoadingState
import com.turki.admin.web.ui.StatCard
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.koin.compose.koinInject

@Composable
fun LessonsScreen(
    viewModel: LessonsViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadLessons()
    }

    Div({ classes(AppStyles.page) }) {
        Div({ classes(AppStyles.pageHeader) }) {
            Div {
                H2({ classes(AppStyles.pageTitle) }) {
                    Text("Уроки")
                }
                Span({ classes(AppStyles.pageSubtitle) }) {
                    Text("Каталог уроков и языков")
                }
            }

            AppButton(
                label = "Обновить",
                onClick = { viewModel.loadLessons() },
                tone = ButtonTone.Ghost,
                leadingIcon = "🔄"
            )
        }

        Div({ classes(AppStyles.statsGrid) }) {
            StatCard(
                label = "Всего уроков",
                value = state.lessons.size.toString(),
                hint = "в активном наборе",
                accentColor = "#F4A261",
                delayMs = 0
            )
        }

        if (state.isLoading) {
            LoadingState("Загрузка уроков...")
        } else {
            Div({ classes(AppStyles.cardList) }) {
                state.lessons.forEachIndexed { index, lesson ->
                    LessonCard(
                        lesson = lesson,
                        delayMs = index * 40
                    )
                }
            }
        }
    }
}

@Composable
private fun LessonCard(
    lesson: Lesson,
    delayMs: Int
) {
    Div(attrs = {
        classes(AppStyles.card)
        style { property("--stagger-delay", "${delayMs}ms") }
    }) {
        Div({ classes(AppStyles.cardMain) }) {
            Div({ classes(AppStyles.cardHeader) }) {
                Div({ classes(AppStyles.headerGroup) }) {
                    Span({ classes(AppStyles.iconBadge) }) { Text("📚") }
                    Div({ classes(AppStyles.titleStack) }) {
                        Span({ classes(AppStyles.cardTitle) }) {
                            Text("Урок ${lesson.orderIndex}: ${lesson.title}")
                        }
                        Span({ classes(AppStyles.cardMeta) }) {
                            Text("Язык: ${lesson.targetLanguage.displayName}")
                        }
                    }
                }

                Span({ classes(AppStyles.cardMeta) }) {
                    Text("ID: ${lesson.id}")
                }
            }

            Div({ classes(AppStyles.cardMeta, AppStyles.paragraph) }) {
                Text(lesson.description.take(150) + if (lesson.description.length > 150) "..." else "")
            }

            Div({ classes(AppStyles.metaRow) }) {
                Span({ classes(AppStyles.chip) }) {
                    Text("📖 ${lesson.vocabularyItems.size} слов")
                }
            }
        }
    }
}
