package com.turki.bot.handler.callback.learn

import com.turki.bot.i18n.S
import com.turki.bot.service.LearnQuestionType
import com.turki.bot.service.LearnSessionPayload
import com.turki.bot.util.editOrSendHtml
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

/**
 * Renders the current question in a learn session.
 */
internal suspend fun sendLearnQuestion(
    context: BehaviourContext,
    query: DataCallbackQuery,
    session: LearnSessionPayload
) {
    val question = session.questions.getOrNull(session.currentIndex) ?: return
    val progress = S.learnWordsProgress(session.currentIndex + 1, session.questions.size)

    val directionLabel = when (question.type) {
        LearnQuestionType.MCQ_RU_TO_TR -> S.learnTranslateToTurkish
        LearnQuestionType.MCQ_TR_TO_RU -> S.learnTranslateToRussian
        LearnQuestionType.MCQ_CHOOSE_TR -> S.learnChooseTurkish
        LearnQuestionType.MCQ_CHOOSE_RU -> S.learnChooseRussian
    }

    val text = buildString {
        appendLine("<b>$progress</b>")
        appendLine()
        appendLine(directionLabel)
        appendLine()
        appendLine("<code>${question.questionText}</code>")
    }

    val buttons = question.options.mapIndexed { idx, option ->
        listOf(dataInlineButton(option, "learn_answer:$idx"))
    } + listOf(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))

    context.editOrSendHtml(
        query,
        text,
        replyMarkup = InlineKeyboardMarkup(buttons)
    )
}
