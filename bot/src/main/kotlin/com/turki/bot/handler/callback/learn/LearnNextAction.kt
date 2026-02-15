package com.turki.bot.handler.callback.learn

import com.turki.bot.handler.callback.CALLBACK_JSON
import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.service.LearnSessionPayload
import com.turki.bot.service.UserFlowState
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import kotlinx.serialization.decodeFromString
import org.slf4j.LoggerFactory

class LearnNextAction(
    private val userService: UserService,
    private val userStateService: UserStateService
) : CallbackAction {
    override val action: String = "learn_next"

    @Suppress("ReturnCount")
    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return

        val state = userStateService.get(user.id) ?: return
        if (state.state != UserFlowState.LEARN_WORDS.name) return

        val session = try {
            CALLBACK_JSON.decodeFromString<LearnSessionPayload>(state.payload)
        } catch (e: Exception) {
            logger.warn("learn_next: bad payload: ${e.message}")
            return
        }

        sendLearnQuestion(context, query, session)
    }

    private companion object {
        val logger = LoggerFactory.getLogger("LearnNextAction")
    }
}
