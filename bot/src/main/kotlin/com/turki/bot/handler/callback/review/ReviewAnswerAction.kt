package com.turki.bot.handler.callback.review

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.ReviewService
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import org.slf4j.LoggerFactory

class ReviewAnswerAction(
    private val userService: UserService,
    private val userStateService: UserStateService,
    private val reviewService: ReviewService,
    private val progressService: ProgressService,
    private val analyticsService: AnalyticsService,
    private val lessonService: LessonService
) : CallbackAction {
    override val action: String = "review_answer"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        handleLegacyReviewAnswer(
            context,
            query,
            parts,
            userService,
            userStateService,
            reviewService,
            progressService,
            analyticsService,
            lessonService
        )
    }

    private companion object {
        val logger = LoggerFactory.getLogger("ReviewAnswerAction")
    }
}
