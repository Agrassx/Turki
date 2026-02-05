package com.turki.bot.handler.callback.homework

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.UserService
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class HomeworkAction(
    private val userService: UserService,
    private val analyticsService: AnalyticsService
) : CallbackAction {
    override val action: String = "homework"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val lessonId = parts.getOrNull(1)?.toIntOrNull() ?: return
        renderHomeworkStart(context, query, lessonId, userService, analyticsService)
    }
}
