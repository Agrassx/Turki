package com.turki.bot.handler.callback.lesson

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.UserService
import com.turki.bot.util.editOrSendHtml
import com.turki.core.domain.EventNames
import com.turki.core.domain.Language
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class LessonsListAction(
    private val lessonService: LessonService,
    private val userService: UserService,
    private val progressService: ProgressService,
    private val analyticsService: AnalyticsService
) : CallbackAction {
    override val action: String = "lessons_list"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val page = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val lessons = lessonService.getLessonsByLanguage(Language.TURKISH)
        val user = userService.findByTelegramId(query.from.id.chatId.long)
        val completed = if (user != null) progressService.getCompletedLessonIds(user.id) else emptySet()
        val perPage = 5
        val totalPages = (lessons.size + perPage - 1) / perPage
        val safePage = page.coerceIn(0, maxOf(totalPages - 1, 0))
        val pageLessons = lessons.drop(safePage * perPage).take(perPage)

        val buttons = pageLessons.map { lesson ->
            val topic = extractLessonTopic(lesson.title)
            val label = if (completed.contains(lesson.id)) {
                "${S.btnLesson} ${lesson.orderIndex} · $topic ✅"
            } else {
                "${S.btnLesson} ${lesson.orderIndex} · $topic"
            }
            listOf(dataInlineButton(label, "lesson_start:${lesson.id}"))
        }.toMutableList()

        if (totalPages > 1) {
            val nav = buildList {
                if (safePage > 0) add(dataInlineButton("◀️", "lessons_list:${safePage - 1}"))
                if (safePage < totalPages - 1) add(dataInlineButton("▶️", "lessons_list:${safePage + 1}"))
            }
            if (nav.isNotEmpty()) {
                buttons.add(nav)
            }
        }

        context.editOrSendHtml(
            query,
            if (totalPages > 1) "${S.lessonsTitle}\n\nСтраница ${safePage + 1}/$totalPages" else S.lessonsTitle,
            replyMarkup = InlineKeyboardMarkup(
                buttons + listOf(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))
            )
        )
        if (user != null) {
            analyticsService.log(EventNames.LESSONS_LIST_OPENED, user.id)
        }
    }
}
