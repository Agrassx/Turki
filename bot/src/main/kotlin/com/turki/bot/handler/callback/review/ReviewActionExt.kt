package com.turki.bot.handler.callback.review

import com.turki.bot.handler.callback.CALLBACK_JSON
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.ReviewFlowPayload
import com.turki.bot.service.ReviewService
import com.turki.bot.service.ReviewSessionPayload
import com.turki.bot.service.TranslationDirection
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

internal suspend fun sendReviewSessionQuestion(
    context: BehaviourContext,
    query: DataCallbackQuery,
    session: ReviewSessionPayload
) {
    val question = session.questions.getOrNull(session.currentIndex) ?: return
    val progress = S.reviewProgress(session.currentIndex + 1, session.questions.size)

    val directionLabel = when (question.direction) {
        TranslationDirection.RU_TO_TR -> S.reviewTranslateToTurkish
        TranslationDirection.TR_TO_RU -> S.reviewTranslateToRussian
    }

    val text = buildString {
        appendLine("<b>$progress</b>")
        appendLine()
        appendLine(directionLabel)
        appendLine()
        appendLine("<code>${question.questionText}</code>")
    }

    val buttons = if (question.options != null) {
        question.options.mapIndexed { idx, option ->
            listOf(dataInlineButton(option, "review_session_answer:$idx"))
        }
    } else {
        listOf(listOf(dataInlineButton("💡 Показать ответ", "review_session_answer:show")))
    }

    context.editOrSendHtml(
        query,
        text,
        replyMarkup = InlineKeyboardMarkup(buttons)
    )
}

internal suspend fun sendReviewCard(
    context: BehaviourContext,
    query: DataCallbackQuery,
    payload: ReviewFlowPayload,
    lessonService: LessonService
) {
    val vocabId = payload.vocabularyIds.getOrNull(payload.index) ?: return
    val item = lessonService.findVocabularyById(vocabId) ?: return
    val card = buildString {
        appendLine(S.reviewCardTitle(item.word))
        appendLine(S.reviewCardTranslation(item.translation))
        item.example?.let { appendLine(S.dictionaryExample(it)) }
    }

    context.editOrSendHtml(
        query,
        card,
        replyMarkup = InlineKeyboardMarkup(
            listOf(
                listOf(
                    dataInlineButton(S.btnRemember, "review_answer:$vocabId:1"),
                    dataInlineButton(S.btnAgain, "review_answer:$vocabId:0")
                )
            )
        )
    )
}

internal suspend fun handleLegacyReviewAnswer(
    context: BehaviourContext,
    query: DataCallbackQuery,
    parts: List<String>,
    userService: UserService,
    userStateService: UserStateService,
    reviewService: ReviewService,
    progressService: ProgressService,
    analyticsService: AnalyticsService,
    lessonService: LessonService
) {
    val telegramId = query.from.id.chatId.long
    if (parts.size < 3) {
        logger.warn("handleReviewAnswer: invalid parts count ${parts.size}")
        return
    }
    val vocabId = parts[1].toIntOrNull() ?: run {
        logger.warn("handleReviewAnswer: invalid vocabId '${parts[1]}'")
        return
    }
    val isCorrect = parts[2] == "1"

    val user = userService.findByTelegramId(telegramId) ?: run {
        logger.warn("handleReviewAnswer: user not found for telegramId=$telegramId")
        return
    }
    reviewService.updateCard(user.id, vocabId, isCorrect)
    progressService.recordReview(user.id)
    analyticsService.log(EventNames.REVIEW_COMPLETED, user.id, props = mapOf("isCorrect" to isCorrect.toString()))

    val state = userStateService.get(user.id) ?: run {
        logger.warn("handleReviewAnswer: no state for user=${user.id}")
        return
    }
    if (state.state != UserFlowState.REVIEW.name) {
        logger.warn("handleReviewAnswer: wrong state '${state.state}', expected REVIEW")
        return
    }
    val payload = CALLBACK_JSON.decodeFromString<ReviewFlowPayload>(state.payload)
    val nextIndex = payload.index + 1
    if (nextIndex >= payload.vocabularyIds.size) {
        userStateService.clear(user.id)
        context.editOrSendHtml(
            query,
            S.reviewDone,
            replyMarkup = InlineKeyboardMarkup(
                listOf(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))
            )
        )
        return
    }
    val nextPayload = payload.copy(index = nextIndex)
    userStateService.set(user.id, UserFlowState.REVIEW.name, CALLBACK_JSON.encodeToString(nextPayload))
    sendReviewCard(context, query, nextPayload, lessonService)
}

private val logger = LoggerFactory.getLogger("ReviewActions")
