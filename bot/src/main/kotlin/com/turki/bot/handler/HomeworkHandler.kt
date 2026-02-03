package com.turki.bot.handler

import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.HomeworkService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.UserStateService
import com.turki.bot.service.UserFlowState
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
    private val progressService: ProgressService by inject(ProgressService::class.java)
    private val lessonService: LessonService by inject(LessonService::class.java)
    private val userStateService: UserStateService by inject(UserStateService::class.java)
    private val analyticsService: AnalyticsService by inject(AnalyticsService::class.java)

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

        val isCorrect = currentQuestionIsCorrect(homework, questionId, answer)
        if (!isCorrect) {
            HomeworkStateManager.clearCurrentQuestion(telegramId)
            userStateService.clear(user.id)
            val question = homework.questions.firstOrNull { it.id == questionId }
            val vocabId = question?.let { resolveHomeworkVocabularyId(homework.lessonId, it) }
            val addButton = if (vocabId != null) {
                listOf(dataInlineButton(S.btnAddToDictionary, "hw_add_dict:$homeworkId:$questionId"))
            } else {
                listOf(dataInlineButton(S.btnAddCustomWord, "dict_add_custom"))
            }
            val buttons = InlineKeyboardMarkup(
                listOf(
                    addButton,
                    listOf(dataInlineButton(S.btnNext, "hw_next:$homeworkId:$questionId"))
                )
            )
            val text = buildString {
                appendLine(S.exerciseIncorrect)
                if (question != null) {
                    appendLine(S.homeworkCorrectAnswer(question.correctAnswer))
                }
            }
            context.sendHtml(message.chat, text, replyMarkup = buttons)
            return
        }

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
                    userStateService.set(user.id, UserFlowState.HOMEWORK_TEXT.name, "{}")
                }
            }
        } else {
            val submission = homeworkService.submitHomework(user.id, homeworkId, answers)
            HomeworkStateManager.clearState(telegramId)
            userStateService.clear(user.id)
            progressService.recordHomework(user.id)
            if (submission.score == submission.maxScore) {
                progressService.markLessonCompleted(user.id, homework.lessonId)
            }

            val resultText = if (submission.score == submission.maxScore) {
                S.homeworkComplete(submission.score, submission.maxScore)
            } else {
                S.homeworkResult(submission.score, submission.maxScore)
            }

            val feedback = buildHomeworkFeedback(homework, answers, submission.score, submission.maxScore)
            val keyboard = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.btnRepeatTopic, "lesson_start:${homework.lessonId}")),
                    listOf(dataInlineButton(S.btnNextHomework, "next_homework:${homework.lessonId}"))
                )
            )

            analyticsService.log(
                "hw_autocheck_done",
                user.id,
                props = mapOf("score" to submission.score.toString())
            )
            analyticsService.log(
                "hw_submitted",
                user.id,
                props = mapOf("score" to submission.score.toString())
            )

            context.sendHtml(message.chat, "$resultText\n\n$feedback", replyMarkup = keyboard)
        }
    }

    private fun currentQuestionIsCorrect(
        homework: com.turki.core.domain.Homework,
        questionId: Int,
        answer: String
    ): Boolean {
        val question = homework.questions.firstOrNull { it.id == questionId } ?: return true
        return homeworkService.isAnswerCorrect(question, answer)
    }

    private suspend fun resolveHomeworkVocabularyId(
        lessonId: Int,
        question: com.turki.core.domain.HomeworkQuestion
    ): Int? {
        val vocabulary = lessonService.getVocabulary(lessonId)
        val answer = normalizeText(question.correctAnswer)
        val questionText = normalizeText(question.questionText)
        return vocabulary.firstOrNull { item ->
            val word = normalizeText(item.word)
            val translation = normalizeText(item.translation)
            word == answer || translation == answer || questionText.contains(word) || questionText.contains(translation)
        }?.id
    }

    private fun normalizeText(text: String): String =
        text.lowercase().replace(Regex("[^\\p{L}\\p{Nd}]"), "")

    private fun buildHomeworkFeedback(
        homework: com.turki.core.domain.Homework,
        answers: Map<Int, String>,
        score: Int,
        maxScore: Int
    ): String {
        val wrong = homework.questions.filter { question ->
            !homeworkService.isAnswerCorrect(question, answers[question.id])
        }

        if (wrong.isEmpty()) {
            if (score < maxScore) {
                return "Есть ошибки — попробуйте ещё раз."
            }
            return S.homeworkFeedbackPerfect
        }

        val lines = wrong.take(3).joinToString("\n") { question ->
            "• ${question.questionText}\n  ${S.homeworkCorrectAnswer(question.correctAnswer)}"
        }

        return S.homeworkFeedbackSummary(lines, wrong.size)
    }
}
