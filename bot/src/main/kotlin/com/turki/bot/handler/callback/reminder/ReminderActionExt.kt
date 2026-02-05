package com.turki.bot.handler.callback.reminder

import com.turki.bot.i18n.S
import com.turki.bot.util.editOrSendHtml
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

private val REMINDER_DAY_MAP = mapOf(
    "MON" to "Понедельник",
    "TUE" to "Вторник",
    "WED" to "Среда",
    "THU" to "Четверг",
    "FRI" to "Пятница",
    "SAT" to "Суббота",
    "SUN" to "Воскресенье"
)

internal fun formatDaysForDisplay(days: String): String {
    return days.split(",").mapNotNull { REMINDER_DAY_MAP[it] }.joinToString(", ")
}

internal suspend fun showDaySelection(
    context: BehaviourContext,
    query: DataCallbackQuery,
    selectedDays: Set<String>,
    needed: Int
) {
    val dayButtons = listOf(
        "MON" to S.btnMon,
        "TUE" to S.btnTue,
        "WED" to S.btnWed,
        "THU" to S.btnThu,
        "FRI" to S.btnFri,
        "SAT" to S.btnSat,
        "SUN" to S.btnSun
    )

    val buttons = dayButtons.chunked(4).map { row ->
        row.map { (code, label) ->
            val isSelected = selectedDays.contains(code)
            val displayLabel = if (isSelected) "✅ $label" else label
            dataInlineButton(displayLabel, "reminder_day_toggle:$needed:$code:${selectedDays.joinToString(",")}")
        }
    }.toMutableList()

    if (selectedDays.size == needed) {
        buttons.add(listOf(dataInlineButton(S.btnConfirmDays, "reminder_days_confirm:${selectedDays.joinToString(",")}")))
    }
    buttons.add(listOf(dataInlineButton(S.btnBack, "reminder_frequency:menu")))

    val text = "${S.reminderSelectDays}\n\n${S.reminderDaysSelected(selectedDays.size, needed)}"
    context.editOrSendHtml(query, text, replyMarkup = InlineKeyboardMarkup(buttons))
}

internal suspend fun showTimeSelection(
    context: BehaviourContext,
    query: DataCallbackQuery,
    days: String
) {
    context.editOrSendHtml(
        query,
        S.reminderSelectTime,
        replyMarkup = InlineKeyboardMarkup(
            listOf(
                listOf(dataInlineButton(S.reminderTimeMorning, "reminder_time:$days|08:00")),
                listOf(dataInlineButton(S.reminderTimeDay, "reminder_time:$days|14:00")),
                listOf(dataInlineButton(S.reminderTimeEvening, "reminder_time:$days|20:00")),
                listOf(dataInlineButton(S.reminderTimeNight, "reminder_time:$days|00:00")),
                listOf(dataInlineButton(S.btnBack, "reminder_frequency:menu"))
            )
        )
    )
}

internal suspend fun renderRemindersMenu(
    context: BehaviourContext,
    query: DataCallbackQuery,
    status: String
) {
    context.editOrSendHtml(
        query,
        status,
        replyMarkup = InlineKeyboardMarkup(
            listOf(
                listOf(dataInlineButton(S.btnConfigureReminders, "reminder_frequency:menu")),
                listOf(dataInlineButton(S.btnEnableWeekdays, "reminder_enable_weekdays")),
                listOf(dataInlineButton(S.btnDisableReminders, "reminder_disable")),
                listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu"))
            )
        )
    )
}
