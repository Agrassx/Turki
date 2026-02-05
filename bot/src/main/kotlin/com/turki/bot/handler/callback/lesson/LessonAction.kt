package com.turki.bot.handler.callback.lesson

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.i18n.S
import com.turki.bot.service.LessonService
import com.turki.bot.util.markdownToHtml
import com.turki.bot.util.sendHtml
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import org.slf4j.LoggerFactory

class LessonAction(
    private val lessonService: LessonService
) : CallbackAction {
    override val action: String = "lesson"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val lessonId = parts.getOrNull(1)?.toIntOrNull()
        if (lessonId == null) {
            logger.warn("handleLessonCallback: lessonId is null")
            return
        }

        val lesson = lessonService.getLessonById(lessonId) ?: run {
            logger.warn("handleLessonCallback: lesson not found for id=$lessonId")
            context.sendHtml(query.from, S.lessonNotFound)
            return
        }

        val lessonText = buildString {
            appendLine(S.lessonTitle(lesson.orderIndex, lesson.title))
            appendLine()
            appendLine(lesson.description.markdownToHtml())
            appendLine()
            appendLine("─────────────────────")
            appendLine()
            appendLine(lesson.content.markdownToHtml())
        }

        context.sendHtml(
            query.from,
            lessonText,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.btnVocabulary, "vocabulary:${lesson.id}")),
                    listOf(dataInlineButton(S.btnGoToHomework, "homework:${lesson.id}")),
                    listOf(dataInlineButton(S.btnStartPractice, "lesson_practice:${lesson.id}")),
                    listOf(dataInlineButton(S.btnSetReminder, "set_reminder"))
                )
            )
        )
    }

    private companion object {
        val logger = LoggerFactory.getLogger("LessonAction")
    }
}
