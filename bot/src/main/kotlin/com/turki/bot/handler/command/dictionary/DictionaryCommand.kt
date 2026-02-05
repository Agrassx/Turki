package com.turki.bot.handler.command.dictionary

import com.turki.bot.handler.command.CommandAction
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.DictionaryService
import com.turki.bot.service.UserService
import com.turki.bot.util.sendHtml
import com.turki.core.domain.EventNames
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from

class DictionaryCommand(
    private val userService: UserService,
    private val dictionaryService: DictionaryService,
    private val analyticsService: AnalyticsService
) : CommandAction {
    override val command: String = "dictionary"

    override suspend fun invoke(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val user = userService.findByTelegramId(from.id.chatId.long) ?: run {
            context.sendHtml(message.chat, S.notRegistered)
            return
        }

        analyticsService.log(EventNames.COMMAND_USED, user.id, props = mapOf("command" to "dictionary"))

        val text = message.content.text
        val query = text.removePrefix("/dictionary").trim()
        if (query.isEmpty()) {
            sendDictionaryList(context, message, user.id, 0, dictionaryService)
            return
        }

        handleDictionaryQuery(context, message, user.id, query, dictionaryService, analyticsService)
    }
}
