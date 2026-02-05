package com.turki.bot.handler.callback.exercise

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.ExerciseService
import com.turki.bot.service.LessonService
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class LessonPracticeAction(
    private val lessonService: LessonService,
    private val exerciseService: ExerciseService,
    private val userService: UserService,
    private val userStateService: UserStateService,
    private val analyticsService: AnalyticsService
) : CallbackAction {
    override val action: String = "lesson_practice"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val lessonId = parts.getOrNull(1)?.toIntOrNull()
        if (lessonId == null) {
            return
        }
        startLessonPractice(
            context,
            query,
            lessonId,
            lessonService,
            exerciseService,
            userService,
            userStateService,
            analyticsService
        )
    }
}
