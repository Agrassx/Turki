package com.turki.bot.handler.callback.menu

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.i18n.S
import com.turki.bot.util.editOrSendHtml
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class KnowledgeTestAction : CallbackAction {
    override val action: String = "knowledge_test"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        context.editOrSendHtml(
            query,
            S.knowledgeTestTitle,
            replyMarkup = InlineKeyboardMarkup(
                listOf(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))
            )
        )
    }
}
