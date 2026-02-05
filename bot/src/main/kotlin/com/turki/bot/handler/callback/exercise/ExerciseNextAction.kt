package com.turki.bot.handler.callback.exercise

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class ExerciseNextAction(
    private val userService: UserService,
    private val userStateService: UserStateService,
    private val progressService: ProgressService,
    private val lessonService: LessonService,
    private val analyticsService: AnalyticsService
) : CallbackAction {
    override val action: String = "exercise_next"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        advanceExercise(context, query, userService, userStateService, progressService, lessonService, analyticsService)
    }
}
