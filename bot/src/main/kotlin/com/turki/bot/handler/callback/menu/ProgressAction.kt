package com.turki.bot.handler.callback.menu

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.UserService
import com.turki.bot.util.editOrSendHtml
import com.turki.bot.util.sendHtml
import com.turki.core.domain.EventNames
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class ProgressAction(
    private val userService: UserService,
    private val progressService: ProgressService,
    private val analyticsService: AnalyticsService
) : CallbackAction {
    override val action: String = "progress"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: run {
            context.sendHtml(query.from, S.notRegistered)
            return
        }

        val summary = progressService.getProgressSummary(user.id)

        val progressText = S.progress(
            firstName = user.firstName,
            completedLessons = summary.completedLessons,
            totalLessons = summary.totalLessons,
            subscriptionActive = user.subscriptionActive,
            currentLevel = summary.currentLevel,
            streakDays = summary.currentStreak
        )

        context.editOrSendHtml(
            query,
            progressText,
            replyMarkup = InlineKeyboardMarkup(
                listOf(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))
            )
        )
        analyticsService.log(EventNames.PROGRESS_VIEWED, user.id)
    }
}
