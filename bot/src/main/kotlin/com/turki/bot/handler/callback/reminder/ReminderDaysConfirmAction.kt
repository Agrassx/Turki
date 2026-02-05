package com.turki.bot.handler.callback.reminder

import com.turki.bot.handler.callback.CallbackAction
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class ReminderDaysConfirmAction : CallbackAction {
    override val action: String = "reminder_days_confirm"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        if (parts.size < 2) return
        val days = parts[1]
        showTimeSelection(context, query, days)
    }
}
