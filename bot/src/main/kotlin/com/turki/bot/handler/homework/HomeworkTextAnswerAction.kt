package com.turki.bot.handler.homework

import com.turki.bot.handler.HomeworkStateManager
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.HomeworkService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import org.slf4j.LoggerFactory
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from

class HomeworkTextAnswerAction(
    private val homeworkService: HomeworkService,
    private val userService: UserService,
    private val progressService: ProgressService,
    private val lessonService: LessonService,
    private val userStateService: UserStateService,
    private val analyticsService: AnalyticsService
) : HomeworkTextAction {
    override val action: String = "text_answer"

    override suspend fun invoke(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: run {
            logger.warn("handleTextAnswer: message.from is null")
            return
        }
        val telegramId = from.id.chatId.long

        val currentState = HomeworkStateManager.getCurrentQuestion(telegramId) ?: run {
            logger.debug("handleTextAnswer: no current question state for telegramId=$telegramId")
            return
        }
        val (homeworkId, questionId) = currentState

        val answer = message.content.text.trim()
        val answers = HomeworkStateManager.getAnswers(telegramId)
        answers[questionId] = answer
        HomeworkStateManager.setAnswers(telegramId, answers)

        deleteUserAndQuestionMessages(context, message, telegramId)

        val user = userService.findByTelegramId(telegramId) ?: run {
            logger.warn("handleTextAnswer: user not found for telegramId=$telegramId")
            return
        }
        val homework = homeworkService.getHomeworkById(homeworkId) ?: run {
            logger.warn("handleTextAnswer: homework not found for homeworkId=$homeworkId")
            return
        }

        val currentIndex = homework.questions.indexOfFirst { it.id == questionId }
        val isCorrect = currentQuestionIsCorrect(homework, questionId, answer, homeworkService)

        if (!isCorrect) {
            handleIncorrectAnswer(
                context,
                message,
                user,
                homework,
                homeworkId,
                questionId,
                telegramId,
                lessonService,
                userStateService
            )
            return
        }

        if (currentIndex < homework.questions.size - 1) {
            sendNextQuestion(
                context,
                message,
                user,
                homework,
                homeworkId,
                currentIndex,
                telegramId,
                userStateService
            )
        } else {
            submitAndShowResults(
                context,
                message,
                user,
                homework,
                homeworkId,
                answers,
                homeworkService,
                userStateService,
                progressService,
                analyticsService,
                lessonService
            )
        }
    }

    private companion object {
        val logger = LoggerFactory.getLogger("HomeworkHandler")
    }
}
