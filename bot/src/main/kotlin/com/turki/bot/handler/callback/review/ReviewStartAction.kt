package com.turki.bot.handler.callback.review

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.i18n.S
import com.turki.bot.service.UserService
import com.turki.bot.util.editOrSendHtml
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import org.slf4j.LoggerFactory

class ReviewStartAction(
    private val userService: UserService
) : CallbackAction {
    override val action: String = "review_start"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val telegramId = query.from.id.chatId.long
        userService.findByTelegramId(telegramId) ?: run {
            logger.warn("handleReviewStart: user not found for telegramId=$telegramId")
            return
        }

        context.editOrSendHtml(
            query,
            S.reviewSelectDifficulty,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.reviewDifficultyWarmup, "review_difficulty:WARMUP")),
                    listOf(dataInlineButton(S.reviewDifficultyTraining, "review_difficulty:TRAINING")),
                    listOf(dataInlineButton(S.reviewDifficultyMarathon, "review_difficulty:MARATHON")),
                    listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu"))
                )
            )
        )
    }

    private companion object {
        val logger = LoggerFactory.getLogger("ReviewStartAction")
    }
}
