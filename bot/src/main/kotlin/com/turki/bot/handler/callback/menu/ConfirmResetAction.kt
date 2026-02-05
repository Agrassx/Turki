package com.turki.bot.handler.callback.menu

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.i18n.S
import com.turki.bot.service.DictionaryService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.ReviewService
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import com.turki.bot.util.sendHtml
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class ConfirmResetAction(
    private val userService: UserService,
    private val userStateService: UserStateService,
    private val progressService: ProgressService,
    private val dictionaryService: DictionaryService,
    private val reviewService: ReviewService
) : CallbackAction {
    override val action: String = "confirm_reset"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        userService.resetProgress(user.id)
        userStateService.clear(user.id)
        progressService.resetProgress(user.id)
        dictionaryService.clearUser(user.id)
        reviewService.clearUser(user.id)

        context.sendHtml(
            query.from,
            S.progressResetSuccess,
            replyMarkup = InlineKeyboardMarkup(
                listOf(listOf(dataInlineButton("${S.btnStartLesson} 1", "lesson_start:1")))
            )
        )
    }
}
