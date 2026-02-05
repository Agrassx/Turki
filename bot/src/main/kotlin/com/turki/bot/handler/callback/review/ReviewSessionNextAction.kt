package com.turki.bot.handler.callback.review

import com.turki.bot.handler.callback.CALLBACK_JSON
import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.service.ReviewSessionPayload
import com.turki.bot.service.UserFlowState
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import kotlinx.serialization.decodeFromString
import org.slf4j.LoggerFactory

class ReviewSessionNextAction(
    private val userService: UserService,
    private val userStateService: UserStateService
) : CallbackAction {
    override val action: String = "review_session_next"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val telegramId = query.from.id.chatId.long
        val user = userService.findByTelegramId(telegramId) ?: run {
            logger.warn("handleReviewSessionNext: user not found for telegramId=$telegramId")
            return
        }

        val state = userStateService.get(user.id) ?: run {
            logger.warn("handleReviewSessionNext: no state for user=${user.id}")
            return
        }

        if (state.state != UserFlowState.REVIEW.name) {
            logger.warn("handleReviewSessionNext: wrong state '${state.state}'")
            return
        }

        val session = try {
            CALLBACK_JSON.decodeFromString<ReviewSessionPayload>(state.payload)
        } catch (e: Exception) {
            logger.warn("handleReviewSessionNext: failed to parse session: ${e.message}")
            return
        }

        sendReviewSessionQuestion(context, query, session)
    }

    private companion object {
        val logger = LoggerFactory.getLogger("ReviewSessionNextAction")
    }
}
