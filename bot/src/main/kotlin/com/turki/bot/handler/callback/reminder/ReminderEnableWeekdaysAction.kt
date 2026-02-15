package com.turki.bot.handler.callback.reminder

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.ReminderPreferenceService
import com.turki.bot.service.UserService
import com.turki.bot.util.editOrSendHtml
import com.turki.core.domain.EventNames
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class ReminderEnableWeekdaysAction(
    private val userService: UserService,
    private val reminderPreferenceService: ReminderPreferenceService,
    private val analyticsService: AnalyticsService
) : CallbackAction {
    override val action: String = "reminder_enable_weekdays"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        val pref = reminderPreferenceService.setSchedule(user.id, "MON,TUE,WED,THU,FRI", "19:00")
        analyticsService.log(
            EventNames.REMINDER_CONFIGURED,
            user.id,
            props = mapOf("schedule" to "${pref.daysOfWeek} ${pref.timeLocal}")
        )
        val nextReminder = nextReminderDescription(pref.daysOfWeek, pref.timeLocal, user.timezone)
        context.editOrSendHtml(
            query,
            S.reminderEnabled(formatDaysForDisplay(pref.daysOfWeek), pref.timeLocal, nextReminder),
            replyMarkup = InlineKeyboardMarkup(
                listOf(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))
            )
        )
    }
}
