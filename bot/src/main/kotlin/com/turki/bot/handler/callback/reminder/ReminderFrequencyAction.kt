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

class ReminderFrequencyAction(
    private val userService: UserService,
    private val reminderPreferenceService: ReminderPreferenceService
) : CallbackAction {
    override val action: String = "reminder_frequency"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val frequency = parts.getOrNull(1)
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return

        when (frequency) {
            "menu" -> {
                context.editOrSendHtml(
                    query,
                    S.reminderSelectFrequency,
                    replyMarkup = InlineKeyboardMarkup(
                        listOf(
                            listOf(dataInlineButton(S.reminderFrequencyDaily, "reminder_frequency:daily")),
                            listOf(dataInlineButton(S.reminderFrequency4x, "reminder_frequency:4")),
                            listOf(dataInlineButton(S.reminderFrequency3x, "reminder_frequency:3")),
                            listOf(dataInlineButton(S.reminderFrequency2x, "reminder_frequency:2")),
                            listOf(dataInlineButton(S.reminderFrequency1x, "reminder_frequency:1")),
                            listOf(dataInlineButton(S.btnBack, "reminders"))
                        )
                    )
                )
            }
            "daily" -> {
                showTimeSelection(context, query, "MON,TUE,WED,THU,FRI,SAT,SUN")
            }
            "1", "2", "3", "4" -> {
                val needed = frequency.toInt()
                showDaySelection(context, query, emptySet(), needed)
            }
            else -> {
                val pref = reminderPreferenceService.getOrDefault(user.id)
                val status = if (pref.isEnabled) S.reminderStatusOn(pref.daysOfWeek, pref.timeLocal) else S.reminderStatusOff
                renderRemindersMenu(context, query, status)
            }
        }
    }
}
