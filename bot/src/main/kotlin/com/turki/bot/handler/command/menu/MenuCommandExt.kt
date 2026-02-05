package com.turki.bot.handler.command.menu

import com.turki.bot.i18n.S
import com.turki.bot.service.UserStateService
import com.turki.bot.util.sendHtml
import com.turki.core.domain.User
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent

internal suspend fun sendMainMenu(
    context: BehaviourContext,
    message: CommonMessage<TextContent>,
    user: User,
    header: String,
    userStateService: UserStateService
) {
    val hasState = userStateService.get(user.id) != null
    val buttons = buildList {
        if (hasState) {
            add(listOf(dataInlineButton(S.btnContinue, "continue")))
        }
        addAll(
            listOf(
                listOf(dataInlineButton(S.btnLessons, "lessons_list")),
                listOf(dataInlineButton(S.btnPractice, "practice_start")),
                listOf(dataInlineButton(S.btnDictionary, "dictionary_prompt")),
                listOf(dataInlineButton(S.btnReview, "review_start")),
                listOf(dataInlineButton(S.btnHomework, "homework:${user.currentLessonId}")),
                listOf(dataInlineButton(S.btnProgress, "progress")),
                listOf(dataInlineButton(S.btnReminders, "reminders")),
                listOf(dataInlineButton(S.btnResetProgress, "reset_progress")),
                listOf(dataInlineButton(S.btnHelp, "help"))
            )
        )
    }

    context.sendHtml(
        message.chat,
        header,
        replyMarkup = InlineKeyboardMarkup(buttons)
    )
}
