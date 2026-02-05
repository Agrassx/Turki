package com.turki.bot.handler.callback.lesson

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.UserService
import com.turki.bot.util.markdownToHtml
import com.turki.bot.util.replaceWithHtml
import com.turki.bot.util.sendHtml
import com.turki.core.domain.EventNames
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import org.slf4j.LoggerFactory

class LessonStartAction(
    private val lessonService: LessonService,
    private val userService: UserService,
    private val progressService: ProgressService,
    private val analyticsService: AnalyticsService
) : CallbackAction {
    override val action: String = "lesson_start"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val telegramId = query.from.id.chatId.long
        val lessonId = parts.getOrNull(1)?.toIntOrNull()
        if (lessonId == null) {
            logger.warn("handleLessonStart: lessonId is null")
            return
        }
        val lesson = lessonService.getLessonById(lessonId) ?: run {
            logger.warn("handleLessonStart: lesson not found for id=$lessonId")
            context.sendHtml(query.from, S.lessonNotFound)
            return
        }

        val introText = buildString {
            appendLine(S.lessonIntroTitle(lesson.orderIndex, lesson.title))
            appendLine()
            appendLine(lesson.description.markdownToHtml())
            appendLine()
            appendLine("─────────────────────")
            appendLine()
            appendLine(lesson.content.markdownToHtml())
        }

        context.replaceWithHtml(
            query,
            introText,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.btnVocabulary, "vocabulary:${lesson.id}")),
                    listOf(dataInlineButton(S.btnGoToHomework, "homework:${lesson.id}")),
                    listOf(dataInlineButton(S.btnStartPractice, "lesson_practice:${lesson.id}")),
                    listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu"))
                )
            )
        )
        val user = userService.findByTelegramId(telegramId) ?: run {
            logger.warn("handleLessonStart: user not found for telegramId=$telegramId")
            return
        }
        progressService.markLessonStarted(user.id, lesson.id)
        analyticsService.log(EventNames.LESSON_STARTED, user.id, props = mapOf("lessonId" to lesson.id.toString()))
    }

    private companion object {
        val logger = LoggerFactory.getLogger("LessonStartAction")
    }
}
