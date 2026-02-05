package com.turki.bot.handler.callback.exercise

import com.turki.bot.handler.callback.CALLBACK_JSON
import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.ExerciseFlowPayload
import com.turki.bot.service.UserFlowState
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import com.turki.bot.util.editOrSendHtml
import com.turki.core.domain.EventNames
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import kotlinx.serialization.decodeFromString
import org.slf4j.LoggerFactory

class ExerciseAnswerAction(
    private val userService: UserService,
    private val userStateService: UserStateService,
    private val analyticsService: AnalyticsService
) : CallbackAction {
    override val action: String = "exercise_answer"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val telegramId = query.from.id.chatId.long
        if (parts.size < 4) {
            logger.warn("handleExerciseAnswer: invalid parts count ${parts.size}")
            return
        }
        val vocabId = parts[2].toIntOrNull() ?: run {
            logger.warn("handleExerciseAnswer: invalid vocabId '${parts[2]}'")
            return
        }
        val selectedIndex = parts[3].toIntOrNull() ?: run {
            logger.warn("handleExerciseAnswer: invalid selectedIndex '${parts[3]}'")
            return
        }

        val user = userService.findByTelegramId(telegramId) ?: run {
            logger.warn("handleExerciseAnswer: user not found for telegramId=$telegramId")
            return
        }
        val state = userStateService.get(user.id) ?: run {
            logger.warn("handleExerciseAnswer: no state for user=${user.id}")
            return
        }
        if (state.state != UserFlowState.EXERCISE.name) {
            logger.warn("handleExerciseAnswer: wrong state '${state.state}', expected EXERCISE")
            return
        }

        val payload = CALLBACK_JSON.decodeFromString<ExerciseFlowPayload>(state.payload)
        val options = payload.optionsByVocabId[vocabId] ?: run {
            logger.warn("handleExerciseAnswer: no options for vocabId=$vocabId")
            return
        }
        val selected = options.getOrNull(selectedIndex) ?: run {
            logger.warn("handleExerciseAnswer: invalid selectedIndex=$selectedIndex, options count=${options.size}")
            return
        }
        val correct = payload.correctByVocabId[vocabId] ?: run {
            logger.warn("handleExerciseAnswer: no correct answer for vocabId=$vocabId")
            return
        }
        val isCorrect = selected == correct

        val verdict = if (isCorrect) S.exerciseCorrect else S.exerciseIncorrect

        analyticsService.log(
            EventNames.EXERCISE_ANSWERED,
            user.id,
            props = mapOf("isCorrect" to isCorrect.toString())
        )

        val explanation = payload.explanationsByVocabId[vocabId] ?: ""
        val buttons = if (isCorrect) {
            listOf(listOf(dataInlineButton(S.btnNext, "exercise_next")))
        } else {
            listOf(
                listOf(dataInlineButton(S.btnAddToDictionary, "exercise_add_dict:$vocabId")),
                listOf(dataInlineButton(S.btnNext, "exercise_next"))
            )
        }

        context.editOrSendHtml(
            query,
            buildString {
                appendLine(verdict)
                if (explanation.isNotBlank()) {
                    appendLine(explanation)
                }
            },
            replyMarkup = InlineKeyboardMarkup(buttons)
        )
    }

    private companion object {
        val logger = LoggerFactory.getLogger("ExerciseAnswerAction")
    }
}
