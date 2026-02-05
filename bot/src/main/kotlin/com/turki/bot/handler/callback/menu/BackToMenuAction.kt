package com.turki.bot.handler.callback.menu

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class BackToMenuAction(
    private val userService: UserService,
    private val userStateService: UserStateService
) : CallbackAction {
    override val action: String = "back_to_menu"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        renderMainMenu(context, query, user.id, user.currentLessonId, userStateService)
    }
}
