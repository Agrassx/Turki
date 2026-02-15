package com.turki.bot.handler.callback.learn

import com.turki.bot.handler.callback.CALLBACK_JSON
import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.LearnDifficulty
import com.turki.bot.service.LearnWordsService
import com.turki.bot.service.UserFlowState
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import com.turki.bot.util.editOrSendHtml
import com.turki.core.domain.EventNames
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import kotlinx.serialization.encodeToString
import org.slf4j.LoggerFactory

class LearnDifficultyAction(
    private val userService: UserService,
    private val learnWordsService: LearnWordsService,
    private val userStateService: UserStateService,
    private val analyticsService: AnalyticsService
) : CallbackAction {
    override val action: String = "learn_difficulty"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val difficultyStr = parts.getOrNull(1)
        val difficulty = try {
            difficultyStr?.let { LearnDifficulty.valueOf(it) } ?: LearnDifficulty.EASY
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid learn difficulty '$difficultyStr': ${e.message}")
            LearnDifficulty.EASY
        }

        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return

        val session = learnWordsService.buildSession(user.currentLessonId, difficulty)

        if (session.questions.isEmpty()) {
            context.editOrSendHtml(
                query,
                S.learnWordsEmpty,
                replyMarkup = InlineKeyboardMarkup(
                    listOf(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))
                )
            )
            return
        }

        userStateService.set(user.id, UserFlowState.LEARN_WORDS.name, CALLBACK_JSON.encodeToString(session))
        analyticsService.log(EventNames.LEARN_STARTED, user.id, props = mapOf("difficulty" to difficulty.name))
        sendLearnQuestion(context, query, session)
    }

    private companion object {
        val logger = LoggerFactory.getLogger("LearnDifficultyAction")
    }
}
