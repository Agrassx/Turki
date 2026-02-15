package com.turki.bot.handler.callback.reminder

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.i18n.S
import com.turki.bot.service.ProgressService
import com.turki.bot.service.ReminderPreferenceService
import com.turki.bot.service.UserService
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class RemindersAction(
    private val userService: UserService,
    private val reminderPreferenceService: ReminderPreferenceService,
    private val progressService: ProgressService
) : CallbackAction {
    override val action: String = "reminders"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        val pref = reminderPreferenceService.getOrDefault(user.id)
        val stats = progressService.getWeeklyStats(user.id)

        val reminderStatus = if (pref.isEnabled) S.reminderStatusOn(
            formatDaysForDisplay(pref.daysOfWeek),
            pref.timeLocal
        ) else S.reminderStatusOff

        val weeklyStatus = if (stats.weeklyReportsEnabled) S.weeklyReportsStatusOn
        else S.weeklyReportsStatusOff

        val status = "$reminderStatus\n$weeklyStatus"

        renderRemindersMenu(context, query, status, stats.weeklyReportsEnabled)
    }
}
