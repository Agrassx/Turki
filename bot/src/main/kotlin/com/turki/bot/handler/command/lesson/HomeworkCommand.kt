package com.turki.bot.handler.command.lesson

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

class HomeworkCommand(
    private val userService: UserService,
    private val analyticsService: AnalyticsService
) : CommandAction {
    override val command: String = "homework"

    override suspend fun invoke(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val user = userService.findByTelegramId(from.id.chatId.long)
        if (user == null) {
            context.sendHtml(message.chat, S.notRegistered)
            return
        }

        analyticsService.log(EventNames.COMMAND_USED, user.id, props = mapOf("command" to "homework"))

        context.sendHtml(
            message.chat,
            S.homeworkStart,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.btnStartHomework, "start_homework:${user.currentLessonId}"))
                )
            )
        )
    }
}
