package com.turki.bot.handler.callback.menu

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.i18n.S
import com.turki.bot.service.UserDataService
import com.turki.bot.service.UserService
import com.turki.bot.util.sendHtml
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class ConfirmDeleteAction(
    private val userService: UserService,
    private val userDataService: UserDataService
) : CallbackAction {
    override val action: String = "confirm_delete"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: run {
            context.sendHtml(query.from, S.notRegistered)
            return
        }
        userDataService.deleteUserData(user.id)
        context.sendHtml(query.from, S.deleteDataSuccess)
    }
}
