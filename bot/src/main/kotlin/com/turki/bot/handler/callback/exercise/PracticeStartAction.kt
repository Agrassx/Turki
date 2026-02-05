package com.turki.bot.handler.callback.exercise

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.ExerciseService
import com.turki.bot.service.LessonService
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class PracticeStartAction(
    private val lessonService: LessonService,
    private val exerciseService: ExerciseService,
    private val userService: UserService,
    private val userStateService: UserStateService,
    private val analyticsService: AnalyticsService
) : CallbackAction {
    override val action: String = "practice_start"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        startLessonPractice(
            context,
            query,
            user.currentLessonId,
            lessonService,
            exerciseService,
            userService,
            userStateService,
            analyticsService
        )
    }
}
