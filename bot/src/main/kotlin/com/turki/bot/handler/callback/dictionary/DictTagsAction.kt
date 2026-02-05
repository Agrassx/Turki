package com.turki.bot.handler.callback.dictionary

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.i18n.S
import com.turki.bot.service.UserService
import com.turki.bot.util.sendHtml
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class DictTagsAction(
    private val userService: UserService
) : CallbackAction {
    override val action: String = "dict_tags"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val vocabId = parts.getOrNull(1)?.toIntOrNull() ?: return
        userService.findByTelegramId(query.from.id.chatId.long) ?: return
        val tags = listOf("фразы", "глаголы", "существительные", "часто")
        val buttons = tags.map { tag ->
            listOf(dataInlineButton(tag, "dict_tag:$vocabId:$tag"))
        }
        context.sendHtml(
            query.from,
            S.dictionaryTagPrompt,
            replyMarkup = InlineKeyboardMarkup(buttons + listOf(listOf(dataInlineButton(S.btnBack, "dictionary_prompt"))))
        )
    }
}
