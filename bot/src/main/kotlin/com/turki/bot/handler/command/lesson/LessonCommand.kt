package com.turki.bot.handler.command.lesson

import com.turki.bot.handler.command.CommandAction
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.LessonService
import com.turki.bot.service.UserService
import com.turki.bot.util.markdownToHtml
import com.turki.bot.util.sendHtml
import com.turki.core.domain.EventNames
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from

class LessonCommand(
    private val userService: UserService,
    private val lessonService: LessonService,
    private val analyticsService: AnalyticsService
) : CommandAction {
    override val command: String = "lesson"

    override suspend fun invoke(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val user = userService.findByTelegramId(from.id.chatId.long) ?: run {
            context.sendHtml(message.chat, S.notRegistered)
            return
        }

        analyticsService.log(EventNames.COMMAND_USED, user.id, props = mapOf("command" to "lesson"))

        val lesson = lessonService.getLessonById(user.currentLessonId)

        if (lesson == null) {
            context.sendHtml(message.chat, S.allLessonsCompleted)
            return
        }

        analyticsService.log(EventNames.LESSON_STARTED, user.id, props = mapOf("lessonId" to lesson.id.toString()))

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
            message.chat,
            lessonText,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.btnVocabulary, "vocabulary:${lesson.id}")),
                    listOf(dataInlineButton(S.btnGoToHomework, "homework:${lesson.id}")),
                    listOf(dataInlineButton(S.btnStartPractice, "lesson_practice:${lesson.id}"))
                )
            )
        )
    }
}
