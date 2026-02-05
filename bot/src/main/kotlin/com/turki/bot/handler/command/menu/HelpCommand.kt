package com.turki.bot.handler.command.menu

import com.turki.bot.handler.command.CommandAction
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.UserService
import com.turki.bot.util.sendHtml
import com.turki.core.domain.EventNames
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from

class HelpCommand(
    private val userService: UserService,
    private val analyticsService: AnalyticsService
) : CommandAction {
    override val command: String = "help"

    override suspend fun invoke(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from
        val userId = from?.let { userService.findByTelegramId(it.id.chatId.long)?.id }
        if (userId != null) {
            analyticsService.log(EventNames.COMMAND_USED, userId, props = mapOf("command" to "help"))
        }
        context.sendHtml(message.chat, S.help)
    }
}
