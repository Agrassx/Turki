package com.turki.bot.handler.callback.learn

import com.turki.bot.handler.callback.CALLBACK_JSON
import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.LearnSessionPayload
import com.turki.bot.service.ReviewService
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

class LearnAnswerAction(
    private val userService: UserService,
    private val userStateService: UserStateService,
    private val reviewService: ReviewService,
    private val analyticsService: AnalyticsService
) : CallbackAction {
    override val action: String = "learn_answer"

    @Suppress("ReturnCount")
    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val answerIdx = parts.getOrNull(1)?.toIntOrNull()
        if (answerIdx == null) {
            logger.warn("learn_answer: invalid index '${parts.getOrNull(1)}'")
            return
        }

        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return

        val state = userStateService.get(user.id) ?: return
        if (state.state != UserFlowState.LEARN_WORDS.name) return

        val session = try {
            CALLBACK_JSON.decodeFromString<LearnSessionPayload>(state.payload)
        } catch (e: Exception) {
            logger.warn("learn_answer: bad payload: ${e.message}")
            return
        }

        val question = session.questions.getOrNull(session.currentIndex) ?: return
        val selectedAnswer = question.options.getOrNull(answerIdx) ?: ""
        val isCorrect = selectedAnswer == question.correctAnswer

        // Update spaced repetition card
        reviewService.updateCard(user.id, question.vocabularyId, isCorrect)

        val newCorrect = if (isCorrect) session.correctCount + 1 else session.correctCount
        val nextIndex = session.currentIndex + 1

        // Session complete?
        if (nextIndex >= session.questions.size) {
            userStateService.clear(user.id)
            val resultText = buildString {
                appendLine(S.learnWordsDone)
                appendLine()
                appendLine("✅ Правильных: $newCorrect из ${session.questions.size}")
            }
            context.editOrSendHtml(
                query,
                resultText,
                replyMarkup = InlineKeyboardMarkup(
                    listOf(
                        listOf(dataInlineButton(S.btnTryAgain, "learn_words")),
                        listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu"))
                    )
                )
            )
            analyticsService.log(
                EventNames.LEARN_COMPLETED, user.id,
                props = mapOf("correct" to newCorrect.toString(), "total" to session.questions.size.toString())
            )
            return
        }

        // Show result and advance
        val resultEmoji = if (isCorrect) "✅" else "❌"
        val resultText = buildString {
            appendLine("$resultEmoji ${if (isCorrect) "Верно!" else "Неверно"}")
            if (!isCorrect) {
                appendLine()
                appendLine("Правильный ответ: <b>${question.correctAnswer}</b>")
            }
        }

        val nextSession = session.copy(currentIndex = nextIndex, correctCount = newCorrect)
        userStateService.set(user.id, UserFlowState.LEARN_WORDS.name, CALLBACK_JSON.encodeToString(nextSession))

        context.editOrSendHtml(
            query,
            resultText,
            replyMarkup = InlineKeyboardMarkup(
                listOf(listOf(dataInlineButton(S.btnNext, "learn_next")))
            )
        )
    }

    private companion object {
        val logger = LoggerFactory.getLogger("LearnAnswerAction")
    }
}
