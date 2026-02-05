package com.turki.bot.handler.command.system

import com.turki.bot.handler.command.CommandAction
import com.turki.bot.handler.command.menu.sendMainMenu
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import com.turki.bot.util.sendHtml
import com.turki.core.domain.EventNames
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from

class StartCommand(
    private val userService: UserService,
    private val analyticsService: AnalyticsService,
    private val userStateService: UserStateService
) : CommandAction {
    override val command: String = "start"

    override suspend fun invoke(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val isNewUser = userService.findByTelegramId(from.id.chatId.long) == null
        val user = userService.findOrCreateUser(
            telegramId = from.id.chatId.long,
            username = from.username?.username,
            firstName = from.firstName,
            lastName = from.lastName
        )

        if (isNewUser) {
            analyticsService.log(EventNames.USER_REGISTERED, user.id)
        } else {
            analyticsService.log(EventNames.USER_RETURNED, user.id)
        }
        analyticsService.log(EventNames.SESSION_START, user.id)
        analyticsService.log(EventNames.COMMAND_USED, user.id, props = mapOf("command" to "start"))
        context.sendHtml(message.chat, S.welcome(user.firstName))
        sendMainMenu(context, message, user, S.mainMenuTitle, userStateService)
    }
}
