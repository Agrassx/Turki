package com.turki.bot.handler

import com.turki.bot.service.HomeworkService
import com.turki.bot.util.Messages
import com.turki.core.domain.QuestionType
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import org.koin.java.KoinJavaComponent.inject

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
            val questionText = "❓ *Вопрос ${currentIndex + 2}*\n\n${nextQuestion.questionText}"

            when (nextQuestion.questionType) {
                QuestionType.MULTIPLE_CHOICE -> {
                    val buttons = nextQuestion.options.mapIndexed { optIndex, option ->
                        listOf(dataInlineButton(option, "answer:$homeworkId:${nextQuestion.id}:$optIndex:$option"))
                    }
                    context.sendMessage(
                        message.chat,
                        questionText,
                        replyMarkup = InlineKeyboardMarkup(buttons)
                    )
                }
                else -> {
                    context.sendMessage(message.chat, "$questionText\n\n_Напишите ваш ответ:_")
                    HomeworkStateManager.setCurrentQuestion(telegramId, homeworkId, nextQuestion.id)
                }
            }
        } else {
            val submission = homeworkService.submitHomework(user.id, homeworkId, answers)
            HomeworkStateManager.clearState(telegramId)

            val resultText = if (submission.score == submission.maxScore) {
                Messages.homeworkComplete(submission.score, submission.maxScore)
            } else {
                Messages.homeworkResult(submission.score, submission.maxScore)
            }

            val keyboard = if (submission.score == submission.maxScore) {
                InlineKeyboardMarkup(listOf(listOf(dataInlineButton("➡️ Следующий урок", "next_lesson"))))
            } else {
                InlineKeyboardMarkup(
                    listOf(listOf(dataInlineButton("🔄 Попробовать снова", "start_homework:${user.currentLessonId}")))
                )
            }

            context.sendMessage(message.chat, resultText, replyMarkup = keyboard)
        }
    }
}
