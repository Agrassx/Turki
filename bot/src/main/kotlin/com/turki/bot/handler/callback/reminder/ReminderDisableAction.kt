package com.turki.bot.handler.callback.reminder

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.i18n.S
import com.turki.bot.service.ReminderPreferenceService
import com.turki.bot.service.UserService
import com.turki.bot.util.editOrSendHtml
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class ReminderDisableAction(
    private val userService: UserService,
    private val reminderPreferenceService: ReminderPreferenceService
) : CallbackAction {
    override val action: String = "reminder_disable"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        reminderPreferenceService.setEnabled(user.id, false)
        context.editOrSendHtml(
            query,
            S.reminderDisabled,
            replyMarkup = InlineKeyboardMarkup(
                listOf(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))
            )
        )
    }
}
