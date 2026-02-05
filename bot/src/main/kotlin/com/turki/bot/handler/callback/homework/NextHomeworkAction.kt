package com.turki.bot.handler.callback.homework

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.LessonService
import com.turki.bot.service.UserService
import com.turki.bot.util.sendHtml
import com.turki.core.domain.Language
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class NextHomeworkAction(
    private val lessonService: LessonService,
    private val userService: UserService,
    private val analyticsService: AnalyticsService
) : CallbackAction {
    override val action: String = "next_homework"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val lessonId = parts.getOrNull(1)?.toIntOrNull() ?: return
        val nextLesson = lessonService.getNextLesson(lessonId, Language.TURKISH)
        if (nextLesson == null) {
            context.sendHtml(query.from, S.homeworkNoNext)
            return
        }
        renderHomeworkStart(context, query, nextLesson.id, userService, analyticsService)
    }
}
