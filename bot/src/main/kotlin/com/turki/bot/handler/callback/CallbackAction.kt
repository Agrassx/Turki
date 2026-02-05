package com.turki.bot.handler.callback

import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

interface CallbackAction {
    val action: String
    suspend operator fun invoke(
        context: BehaviourContext,
        query: DataCallbackQuery,
        parts: List<String>
    )
}
