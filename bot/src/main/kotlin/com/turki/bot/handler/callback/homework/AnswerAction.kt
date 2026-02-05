package com.turki.bot.handler.callback.homework

import com.turki.bot.handler.HomeworkStateManager
import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.HomeworkService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import com.turki.bot.util.editOrSendHtml
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import org.slf4j.LoggerFactory

class AnswerAction(
    private val userService: UserService,
    private val homeworkService: HomeworkService,
    private val lessonService: LessonService,
    private val userStateService: UserStateService,
    private val progressService: ProgressService,
    private val analyticsService: AnalyticsService
) : CallbackAction {
    override val action: String = "answer"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val telegramId = query.from.id.chatId.long
        if (parts.size < 4) {
            logger.warn("handleAnswer: invalid parts count ${parts.size}, expected 4. Data: $parts")
            return
        }

        val homeworkId = parts[1].toIntOrNull() ?: run {
            logger.warn("handleAnswer: invalid homeworkId '${parts[1]}'")
            return
        }
        val questionId = parts[2].toIntOrNull() ?: run {
            logger.warn("handleAnswer: invalid questionId '${parts[2]}'")
            return
        }
        val optionIndex = parts[3].toIntOrNull() ?: run {
            logger.warn("handleAnswer: invalid optionIndex '${parts[3]}'")
            return
        }

        val user = userService.findByTelegramId(telegramId) ?: run {
            logger.warn("handleAnswer: user not found for telegramId=$telegramId")
            return
        }
        val homework = homeworkService.getHomeworkById(homeworkId) ?: run {
            logger.warn("handleAnswer: homework not found for homeworkId=$homeworkId, user=${user.id}")
            return
        }
        val question = homework.questions.firstOrNull { it.id == questionId } ?: run {
            logger.warn("handleAnswer: question not found for questionId=$questionId in homework=$homeworkId")
            return
        }

        val answer = question.options.getOrNull(optionIndex) ?: run {
            logger.warn("handleAnswer: invalid optionIndex=$optionIndex, options count=${question.options.size}")
            return
        }

        val currentAnswers = HomeworkStateManager.getAnswers(user.telegramId)
        currentAnswers[questionId] = answer
        HomeworkStateManager.setAnswers(user.telegramId, currentAnswers)

        val currentIndex = homework.questions.indexOfFirst { it.id == questionId }

        val isCorrect = homeworkService.isAnswerCorrect(question, answer)

        if (!isCorrect) {
            val buttons = buildHomeworkWrongButtons(lessonService, homeworkId, questionId, homework.lessonId, question)
            val text = buildString {
                appendLine(S.exerciseIncorrect)
                appendLine(S.homeworkCorrectAnswer(question.correctAnswer))
            }
            context.editOrSendHtml(
                query,
                text,
                replyMarkup = InlineKeyboardMarkup(buttons)
            )
            return
        }

        if (currentIndex < homework.questions.size - 1) {
            sendQuestion(
                context,
                query,
                homework.questions[currentIndex + 1],
                currentIndex + 1,
                homeworkId,
                user.firstName,
                userService,
                userStateService
            )
        } else {
            submitHomeworkResult(
                context,
                query,
                user,
                homework,
                currentAnswers,
                homeworkService,
                userStateService,
                progressService,
                analyticsService,
                lessonService
            )
        }
    }

    private companion object {
        val logger = LoggerFactory.getLogger("AnswerAction")
    }
}
