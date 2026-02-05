package com.turki.bot.handler.command.lesson

import com.turki.bot.handler.command.CommandAction
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.UserService
import com.turki.bot.util.sendHtml
import com.turki.core.domain.EventNames
import com.turki.core.domain.Language
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from

class LessonsCommand(
    private val userService: UserService,
    private val lessonService: LessonService,
    private val progressService: ProgressService,
    private val analyticsService: AnalyticsService
) : CommandAction {
    override val command: String = "lessons"

    override suspend fun invoke(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val user = userService.findByTelegramId(from.id.chatId.long) ?: run {
            context.sendHtml(message.chat, S.notRegistered)
            return
        }

        analyticsService.log(EventNames.COMMAND_USED, user.id, props = mapOf("command" to "lessons"))

        val lessons = lessonService.getLessonsByLanguage(Language.TURKISH)
        val completed = progressService.getCompletedLessonIds(user.id)
        val perPage = 5
        val totalPages = (lessons.size + perPage - 1) / perPage
        val pageLessons = lessons.take(perPage)
        val buttons = pageLessons.map { lesson ->
            val topic = extractLessonTopic(lesson.title)
            val label = if (completed.contains(lesson.id)) {
                "${S.btnLesson} ${lesson.orderIndex} · $topic ✅"
            } else {
                "${S.btnLesson} ${lesson.orderIndex} · $topic"
            }
            listOf(dataInlineButton(label, "lesson_start:${lesson.id}"))
        }.toMutableList()

        if (totalPages > 1) {
            buttons.add(listOf(dataInlineButton("▶️", "lessons_list:1")))
        }

        context.sendHtml(
            message.chat,
            if (totalPages > 1) "${S.lessonsTitle}\n\nСтраница 1/$totalPages" else S.lessonsTitle,
            replyMarkup = InlineKeyboardMarkup(buttons + listOf(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu"))))
        )
    }
}
