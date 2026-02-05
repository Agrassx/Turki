package com.turki.bot.handler.callback.dictionary

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.i18n.S
import com.turki.bot.service.UserFlowState
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import com.turki.bot.util.editOrSendHtml
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class DictAddCustomAction(
    private val userService: UserService,
    private val userStateService: UserStateService
) : CallbackAction {
    override val action: String = "dict_add_custom"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        userStateService.set(user.id, UserFlowState.DICT_ADD_CUSTOM.name, "{}")
        context.editOrSendHtml(
            query,
            S.dictionaryAddPrompt,
            replyMarkup = InlineKeyboardMarkup(
                listOf(listOf(dataInlineButton(S.btnCancel, "dictionary_prompt")))
            )
        )
    }
}
