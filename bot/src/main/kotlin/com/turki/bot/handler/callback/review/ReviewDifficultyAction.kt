package com.turki.bot.handler.callback.review

import com.turki.bot.handler.callback.CALLBACK_JSON
import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.ReviewDifficulty
import com.turki.bot.service.ReviewService
import com.turki.bot.service.UserFlowState
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import com.turki.bot.util.editOrSendHtml
import com.turki.core.domain.EventNames
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import kotlinx.serialization.encodeToString
import org.slf4j.LoggerFactory

class ReviewDifficultyAction(
    private val userService: UserService,
    private val reviewService: ReviewService,
    private val userStateService: UserStateService,
    private val analyticsService: AnalyticsService
) : CallbackAction {
    override val action: String = "review_difficulty"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val telegramId = query.from.id.chatId.long
        val difficultyStr = parts.getOrNull(1)
        val difficulty = try {
            difficultyStr?.let { ReviewDifficulty.valueOf(it) } ?: ReviewDifficulty.WARMUP
        } catch (e: IllegalArgumentException) {
            logger.warn("handleReviewDifficulty: invalid difficulty '$difficultyStr': ${e.message}")
            ReviewDifficulty.WARMUP
        }

        val user = userService.findByTelegramId(telegramId) ?: run {
            logger.warn("handleReviewDifficulty: user not found for telegramId=$telegramId")
            return
        }

        val session = reviewService.buildReviewSession(user.id, user.currentLessonId, difficulty)

        if (session.questions.isEmpty()) {
            context.editOrSendHtml(
                query,
                S.reviewEmpty,
                replyMarkup = InlineKeyboardMarkup(
                    listOf(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))
                )
            )
            return
        }

        userStateService.set(user.id, UserFlowState.REVIEW.name, CALLBACK_JSON.encodeToString(session))
        analyticsService.log(EventNames.REVIEW_STARTED, user.id, props = mapOf("difficulty" to difficulty.name))
        sendReviewSessionQuestion(context, query, session)
    }

    private companion object {
        val logger = LoggerFactory.getLogger("ReviewDifficultyAction")
    }
}
