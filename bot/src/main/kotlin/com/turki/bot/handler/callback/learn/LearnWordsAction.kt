package com.turki.bot.handler.callback.learn

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.i18n.S
import com.turki.bot.service.UserService
import com.turki.bot.util.editOrSendHtml
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class LearnWordsAction(
    private val userService: UserService
) : CallbackAction {
    override val action: String = "learn_words"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        userService.findByTelegramId(query.from.id.chatId.long) ?: return

        context.editOrSendHtml(
            query,
            S.learnWordsIntro,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.learnDifficultyEasy, "learn_difficulty:EASY")),
                    listOf(dataInlineButton(S.learnDifficultyMedium, "learn_difficulty:MEDIUM")),
                    listOf(dataInlineButton(S.learnDifficultyHard, "learn_difficulty:HARD")),
                    listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu"))
                )
            )
        )
    }
}
