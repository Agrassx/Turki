package com.turki.bot.handler.command.system

import com.turki.bot.handler.command.CommandAction
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.UserService
import com.turki.bot.util.sendHtml
import com.turki.core.domain.EventNames
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from

class DeleteCommand(
    private val userService: UserService,
    private val analyticsService: AnalyticsService
) : CommandAction {
    override val command: String = "delete"

    override suspend fun invoke(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val user = userService.findByTelegramId(from.id.chatId.long) ?: run {
            context.sendHtml(message.chat, S.notRegistered)
            return
        }
        analyticsService.log(EventNames.COMMAND_USED, user.id, props = mapOf("command" to "delete"))
        context.sendHtml(
            message.chat,
            S.deleteDataConfirm,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.btnConfirmDelete, "confirm_delete")),
                    listOf(dataInlineButton(S.btnCancel, "back_to_menu"))
                )
            )
        )
    }
}
