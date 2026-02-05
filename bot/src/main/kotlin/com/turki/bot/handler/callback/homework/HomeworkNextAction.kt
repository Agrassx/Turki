package com.turki.bot.handler.callback.homework

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.HomeworkService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class HomeworkNextAction(
    private val userService: UserService,
    private val homeworkService: HomeworkService,
    private val userStateService: UserStateService,
    private val progressService: ProgressService,
    private val analyticsService: AnalyticsService,
    private val lessonService: LessonService
) : CallbackAction {
    override val action: String = "hw_next"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        advanceHomework(
            context,
            query,
            parts,
            userService,
            homeworkService,
            userStateService,
            progressService,
            analyticsService,
            lessonService
        )
    }
}
