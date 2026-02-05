package com.turki.bot.handler.command.system

import com.turki.bot.handler.command.CommandAction
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.UserDataService
import com.turki.bot.service.UserService
import com.turki.bot.util.sendHtml
import com.turki.core.domain.EventNames
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from

class ExportCommand(
    private val userService: UserService,
    private val userDataService: UserDataService,
    private val analyticsService: AnalyticsService
) : CommandAction {
    override val command: String = "export"

    override suspend fun invoke(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val user = userService.findByTelegramId(from.id.chatId.long) ?: run {
            context.sendHtml(message.chat, S.notRegistered)
            return
        }

        analyticsService.log(EventNames.COMMAND_USED, user.id, props = mapOf("command" to "export"))
        context.sendHtml(message.chat, S.exportDataPreparing)

        val exportJson = userDataService.exportUserData(user)
        val fileName = "turki_data_${user.telegramId}.json"
        val fileBytes = exportJson.toByteArray(Charsets.UTF_8)

        context.sendDocument(
            message.chat,
            fileBytes.asMultipartFile(fileName),
            text = S.exportDataReady
        )

        analyticsService.log(EventNames.DATA_EXPORTED, user.id)
    }
}
