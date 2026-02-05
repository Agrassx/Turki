package com.turki.bot.handler

import com.turki.bot.handler.callback.CallbackAction
import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import org.slf4j.LoggerFactory

class CallbackHandler(
    actions: List<CallbackAction>
) {
    private val logger = LoggerFactory.getLogger("CallbackHandler")
    private val actionMap = actions.associateBy { it.action }

    suspend fun handleCallback(context: BehaviourContext, query: DataCallbackQuery) {
        val data = query.data
        val parts = data.split(":")
        val action = parts[0]

        context.answer(query)

        val handler = actionMap[action]
        if (handler != null) {
            handler(context, query, parts)
        } else {
            logger.warn("Unknown callback action: '$action', data: '$data'")
        }
    }
}
