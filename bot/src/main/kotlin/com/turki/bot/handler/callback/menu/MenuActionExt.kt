package com.turki.bot.handler.callback.menu

import com.turki.bot.i18n.S
import com.turki.bot.service.UserStateService
import com.turki.bot.util.editOrSendHtml
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import org.slf4j.LoggerFactory

internal suspend fun renderMainMenu(
    context: BehaviourContext,
    query: DataCallbackQuery,
    userId: Long,
    currentLessonId: Int,
    userStateService: UserStateService
) {
    val hasState = userStateService.get(userId) != null
    val buttons = buildList {
        if (hasState) {
            add(listOf(dataInlineButton(S.btnContinue, "continue")))
        }
        addAll(
            listOf(
                listOf(dataInlineButton(S.btnLessons, "lessons_list")),
                listOf(dataInlineButton(S.btnLearnWords, "learn_words")),
                listOf(dataInlineButton(S.btnPractice, "practice_start")),
                listOf(dataInlineButton(S.btnDictionary, "dictionary_prompt")),
                listOf(dataInlineButton(S.btnReview, "review_start")),
                listOf(dataInlineButton(S.btnHomework, "homework:$currentLessonId")),
                listOf(dataInlineButton(S.btnProgress, "progress")),
                listOf(dataInlineButton(S.btnReminders, "reminders")),
                listOf(dataInlineButton(S.btnResetProgress, "reset_progress")),
                listOf(dataInlineButton(S.btnHelp, "help"))
            )
        )
    }

    context.editOrSendHtml(
        query,
        S.menuTitle,
        replyMarkup = InlineKeyboardMarkup(buttons)
    )
}

internal val menuLogger = LoggerFactory.getLogger("MenuActions")
