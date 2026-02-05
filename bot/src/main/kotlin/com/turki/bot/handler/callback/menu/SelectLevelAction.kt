package com.turki.bot.handler.callback.menu

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.i18n.S
import com.turki.bot.util.editOrSendHtml
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class SelectLevelAction : CallbackAction {
    override val action: String = "select_level"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        context.editOrSendHtml(
            query,
            S.selectLevelTitle,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(
                        dataInlineButton(S.btnLevelWithStatus("A1", true), "set_level:A1"),
                        dataInlineButton(S.btnLevelWithStatus("A2", false), "set_level:A2")
                    ),
                    listOf(
                        dataInlineButton(S.btnLevelWithStatus("B1", false), "set_level:B1"),
                        dataInlineButton(S.btnLevelWithStatus("B2", false), "set_level:B2")
                    ),
                    listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu"))
                )
            )
        )
    }
}
