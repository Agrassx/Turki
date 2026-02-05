package com.turki.bot.handler.callback.reminder

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.i18n.S
import com.turki.bot.service.ReminderService
import com.turki.bot.service.UserService
import com.turki.bot.util.sendHtml
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class SetReminderAction(
    private val userService: UserService,
    private val reminderService: ReminderService
) : CallbackAction {
    override val action: String = "set_reminder"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        reminderService.createHomeworkReminder(user.id)
        context.sendHtml(query.from, S.reminderSet)
    }
}
