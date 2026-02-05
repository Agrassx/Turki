package com.turki.bot.handler.callback.lesson

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.i18n.S
import com.turki.bot.service.LessonService
import com.turki.bot.service.UserService
import com.turki.bot.util.markdownToHtml
import com.turki.bot.util.sendHtml
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class NextLessonAction(
    private val userService: UserService,
    private val lessonService: LessonService
) : CallbackAction {
    override val action: String = "next_lesson"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return

        val nextLesson = lessonService.getLessonById(user.currentLessonId)

        if (nextLesson == null) {
            context.sendHtml(query.from, S.allLessonsCompleted)
            return
        }

        val lessonText = buildString {
            appendLine(S.lessonTitle(nextLesson.orderIndex, nextLesson.title))
            appendLine()
            appendLine(nextLesson.description.markdownToHtml())
            appendLine()
            appendLine("─────────────────────")
            appendLine()
            appendLine(nextLesson.content.markdownToHtml())
        }

        context.sendHtml(
            query.from,
            lessonText,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.btnVocabulary, "vocabulary:${nextLesson.id}")),
                    listOf(dataInlineButton(S.btnGoToHomework, "homework:${nextLesson.id}"))
                )
            )
        )
    }
}
