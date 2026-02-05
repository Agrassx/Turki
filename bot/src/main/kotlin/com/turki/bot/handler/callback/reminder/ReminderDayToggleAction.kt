package com.turki.bot.handler.callback.reminder

import com.turki.bot.handler.callback.CallbackAction
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class ReminderDayToggleAction : CallbackAction {
    override val action: String = "reminder_day_toggle"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        if (parts.size < 4) return
        val needed = parts[1].toIntOrNull() ?: return
        val day = parts[2]
        val currentDays = parts[3].split(",").filter { it.isNotEmpty() }.toMutableSet()

        if (currentDays.contains(day)) {
            currentDays.remove(day)
        } else if (currentDays.size < needed) {
            currentDays.add(day)
        }

        showDaySelection(context, query, currentDays, needed)
    }
}
