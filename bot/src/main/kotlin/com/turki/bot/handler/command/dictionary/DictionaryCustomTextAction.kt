package com.turki.bot.handler.command.dictionary

import com.turki.bot.handler.command.CommandTextAction
import com.turki.bot.i18n.S
import com.turki.bot.service.DictionaryService
import com.turki.bot.service.UserFlowState
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import com.turki.bot.util.sendHtml
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from

class DictionaryCustomTextAction(
    private val userService: UserService,
    private val dictionaryService: DictionaryService,
    private val userStateService: UserStateService
) : CommandTextAction {
    override val state: UserFlowState = UserFlowState.DICT_ADD_CUSTOM

    override suspend fun invoke(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val user = userService.findByTelegramId(from.id.chatId.long) ?: run {
            context.sendHtml(message.chat, S.notRegistered)
            return
        }
        val input = message.content.text.trim()
        val parsed = parseCustomDictionaryInput(input)
        if (parsed == null) {
            userStateService.set(user.id, UserFlowState.DICT_ADD_CUSTOM.name, "{}")
            context.sendHtml(message.chat, S.dictionaryAddFormatError)
            return
        }
        dictionaryService.addCustomWord(user.id, parsed.first, parsed.second)
        sendDictionaryList(context, message, user.id, 0, dictionaryService)
    }
}
