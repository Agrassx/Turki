package com.turki.bot.handler.callback.review

import com.turki.bot.handler.callback.CALLBACK_JSON
import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.ReviewService
import com.turki.bot.service.ReviewSessionPayload
import com.turki.bot.service.ReviewSourceType
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
import kotlinx.serialization.encodeToString
import org.slf4j.LoggerFactory

class ReviewSessionAnswerAction(
    private val userService: UserService,
    private val userStateService: UserStateService,
    private val reviewService: ReviewService,
    private val progressService: ProgressService,
    private val analyticsService: AnalyticsService,
    private val lessonService: LessonService
) : CallbackAction {
    override val action: String = "review_session_answer"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val telegramId = query.from.id.chatId.long
        if (parts.size < 2) {
            logger.warn("handleReviewSessionAnswer: invalid parts count ${parts.size}")
            return
        }

        val user = userService.findByTelegramId(telegramId) ?: run {
            logger.warn("handleReviewSessionAnswer: user not found for telegramId=$telegramId")
            return
        }

        val state = userStateService.get(user.id) ?: run {
            logger.warn("handleReviewSessionAnswer: no state for user=${user.id}")
            return
        }

        if (state.state != UserFlowState.REVIEW.name) {
            logger.warn("handleReviewSessionAnswer: wrong state '${state.state}'")
            return
        }

        val session = try {
            CALLBACK_JSON.decodeFromString<ReviewSessionPayload>(state.payload)
        } catch (e: Exception) {
            logger.debug("handleReviewSessionAnswer: trying legacy format: ${e.message}")
            handleLegacyReviewAnswer(
                context,
                query,
                parts,
                userService,
                userStateService,
                reviewService,
                progressService,
                analyticsService,
                lessonService
            )
            return
        }

        val question = session.questions.getOrNull(session.currentIndex) ?: return
        val answerIdx = parts[1].toIntOrNull()

        val isCorrect = if (answerIdx != null && question.options != null) {
            val selectedAnswer = question.options.getOrNull(answerIdx) ?: ""
            selectedAnswer == question.correctAnswer
        } else {
            false
        }

        if (question.sourceType == ReviewSourceType.VOCABULARY ||
            question.sourceType == ReviewSourceType.USER_DICTIONARY) {
            reviewService.updateCard(user.id, question.sourceId, isCorrect)
        }

        progressService.recordReview(user.id)

        val newCorrectCount = if (isCorrect) session.correctCount + 1 else session.correctCount
        val nextIndex = session.currentIndex + 1

        if (nextIndex >= session.questions.size) {
            userStateService.clear(user.id)
            val resultText = buildString {
                appendLine(S.reviewDone)
                appendLine()
                appendLine("✅ Правильных ответов: $newCorrectCount из ${session.questions.size}")
            }
            context.editOrSendHtml(
                query,
                resultText,
                replyMarkup = InlineKeyboardMarkup(
                    listOf(
                        listOf(dataInlineButton(S.btnTryAgain, "review_start")),
                        listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu"))
                    )
                )
            )
            analyticsService.log(
                EventNames.REVIEW_COMPLETED,
                user.id,
                props = mapOf(
                    "correct" to newCorrectCount.toString(),
                    "total" to session.questions.size.toString()
                )
            )
            return
        }

        val resultEmoji = if (isCorrect) "✅" else "❌"
        val resultText = buildString {
            appendLine("$resultEmoji ${if (isCorrect) "Верно!" else "Неверно"}")
            if (!isCorrect) {
                appendLine()
                appendLine("Правильный ответ: <b>${question.correctAnswer}</b>")
            }
        }

        val nextSession = session.copy(
            currentIndex = nextIndex,
            correctCount = newCorrectCount
        )
        userStateService.set(user.id, UserFlowState.REVIEW.name, CALLBACK_JSON.encodeToString(nextSession))

        context.editOrSendHtml(
            query,
            resultText,
            replyMarkup = InlineKeyboardMarkup(
                listOf(listOf(dataInlineButton(S.btnNext, "review_session_next")))
            )
        )
    }

    private companion object {
        val logger = LoggerFactory.getLogger("ReviewSessionAnswerAction")
    }
}
