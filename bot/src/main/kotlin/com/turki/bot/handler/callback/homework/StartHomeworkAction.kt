package com.turki.bot.handler.callback.homework

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.HomeworkService
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import com.turki.bot.util.editOrSendHtml
import com.turki.core.domain.EventNames
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import org.slf4j.LoggerFactory

class StartHomeworkAction(
    private val homeworkService: HomeworkService,
    private val userService: UserService,
    private val userStateService: UserStateService,
    private val analyticsService: AnalyticsService
) : CallbackAction {
    override val action: String = "start_homework"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val telegramId = query.from.id.chatId.long
        val lessonId = parts.getOrNull(1)?.toIntOrNull()
        if (lessonId == null) {
            logger.warn("handleStartHomework: lessonId is null")
            return
        }

        val homework = homeworkService.getHomeworkForLesson(lessonId) ?: run {
            logger.info("handleStartHomework: no homework for lessonId=$lessonId")
            context.editOrSendHtml(
                query,
                S.homeworkNotReady,
                replyMarkup = InlineKeyboardMarkup(
                    listOf(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))
                )
            )
            return
        }

        val user = userService.findByTelegramId(telegramId) ?: run {
            logger.warn("handleStartHomework: user not found for telegramId=$telegramId")
            return
        }

        if (homeworkService.hasCompletedHomework(user.id, homework.id)) {
            context.editOrSendHtml(
                query,
                S.homeworkAlreadyCompleted,
                replyMarkup = InlineKeyboardMarkup(
                    listOf(
                        listOf(dataInlineButton(S.btnContinue, "lesson_start:${user.currentLessonId}")),
                        listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu"))
                    )
                )
            )
            return
        }

        sendQuestion(context, query, homework.questions.first(), 0, homework.id, user.firstName, userService, userStateService)
        analyticsService.log(EventNames.HOMEWORK_STARTED, user.id, props = mapOf("lessonId" to lessonId.toString()))
    }

    private companion object {
        val logger = LoggerFactory.getLogger("StartHomeworkAction")
    }
}
