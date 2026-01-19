package com.turki.bot.handler

import com.turki.bot.i18n.S
import com.turki.bot.service.HomeworkService
import com.turki.bot.util.sendHtml
import com.turki.core.domain.QuestionType
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import org.koin.java.KoinJavaComponent.inject

/**
 * Handler for processing text answers to homework questions.
 *
 * This handler manages the interactive homework flow where users type answers
 * to open-ended questions (TEXT_INPUT, TRANSLATION). It:
 * - Tracks the current question state for each user
 * - Collects answers as users respond
 * - Advances to the next question or submits homework when complete
 * - Displays results and appropriate navigation buttons
 *
 * The handler uses [HomeworkStateManager] to maintain state between messages.
 * State is stored in memory and cleared after homework submission.
 */
class HomeworkHandler(private val homeworkService: HomeworkService) {

    private val userService: com.turki.bot.service.UserService by inject(
        com.turki.bot.service.UserService::class.java
    )

    suspend fun handleTextAnswer(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val telegramId = from.id.chatId.long

        val currentState = HomeworkStateManager.getCurrentQuestion(telegramId) ?: return
        val (homeworkId, questionId) = currentState

        val answer = message.content.text.trim()
        val answers = HomeworkStateManager.getAnswers(telegramId)
        answers[questionId] = answer
        HomeworkStateManager.setAnswers(telegramId, answers)

        val user = userService.findByTelegramId(telegramId) ?: return
        val homework = homeworkService.getHomeworkForLesson(user.currentLessonId) ?: return

        val currentIndex = homework.questions.indexOfFirst { it.id == questionId }

        if (currentIndex < homework.questions.size - 1) {
            val nextQuestion = homework.questions[currentIndex + 1]
            val questionText = "${S.questionTitle(currentIndex + 2)}\n\n${nextQuestion.questionText}"

            when (nextQuestion.questionType) {
                QuestionType.MULTIPLE_CHOICE -> {
                    val buttons = nextQuestion.options.mapIndexed { optIndex, option ->
                        listOf(dataInlineButton(option, "answer:$homeworkId:${nextQuestion.id}:$optIndex:$option"))
                    }
                    context.sendHtml(
                        message.chat,
                        questionText,
                        replyMarkup = InlineKeyboardMarkup(buttons)
                    )
                }
                else -> {
                    context.sendHtml(message.chat, "$questionText\n\n${S.writeYourAnswer}")
                    HomeworkStateManager.setCurrentQuestion(telegramId, homeworkId, nextQuestion.id)
                }
            }
        } else {
            val submission = homeworkService.submitHomework(user.id, homeworkId, answers)
            HomeworkStateManager.clearState(telegramId)

            val resultText = if (submission.score == submission.maxScore) {
                S.homeworkComplete(submission.score, submission.maxScore)
            } else {
                S.homeworkResult(submission.score, submission.maxScore)
            }

            val keyboard = if (submission.score == submission.maxScore) {
                InlineKeyboardMarkup(listOf(listOf(dataInlineButton(S.btnNextLesson, "next_lesson"))))
            } else {
                InlineKeyboardMarkup(
                    listOf(listOf(dataInlineButton(S.btnTryAgain, "start_homework:${user.currentLessonId}")))
                )
            }

            context.sendHtml(message.chat, resultText, replyMarkup = keyboard)
        }
    }
}
