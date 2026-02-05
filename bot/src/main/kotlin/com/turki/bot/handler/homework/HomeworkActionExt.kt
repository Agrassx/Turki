package com.turki.bot.handler.homework

import com.turki.bot.handler.HomeworkStateManager
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.HomeworkService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.UserFlowState
import com.turki.bot.service.UserStateService
import com.turki.bot.util.sendHtml
import com.turki.core.domain.Language
import com.turki.core.domain.QuestionType
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import org.slf4j.LoggerFactory

internal suspend fun deleteUserAndQuestionMessages(
    context: BehaviourContext,
    message: CommonMessage<TextContent>,
    telegramId: Long
) {
    try {
        context.delete(message)
    } catch (e: Exception) {
        logger.debug("Could not delete user message: ${e.message}")
    }

    val questionMessageId = HomeworkStateManager.getQuestionMessageId(telegramId)
    if (questionMessageId != null) {
        try {
            context.bot.execute(
                dev.inmo.tgbotapi.requests.DeleteMessage(
                    message.chat.id,
                    MessageId(questionMessageId)
                )
            )
        } catch (e: Exception) {
            logger.debug("Could not delete question message: ${e.message}")
        }
    }
}

internal suspend fun handleIncorrectAnswer(
    context: BehaviourContext,
    message: CommonMessage<TextContent>,
    user: com.turki.core.domain.User,
    homework: com.turki.core.domain.Homework,
    homeworkId: Int,
    questionId: Int,
    telegramId: Long,
    lessonService: LessonService,
    userStateService: UserStateService
) {
    HomeworkStateManager.clearCurrentQuestion(telegramId)
    userStateService.clear(user.id)
    val question = homework.questions.firstOrNull { it.id == questionId }
    val vocabId = question?.let { resolveHomeworkVocabularyId(lessonService, homework.lessonId, it) }
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
}

internal suspend fun sendNextQuestion(
    context: BehaviourContext,
    message: CommonMessage<TextContent>,
    user: com.turki.core.domain.User,
    homework: com.turki.core.domain.Homework,
    homeworkId: Int,
    currentIndex: Int,
    telegramId: Long,
    userStateService: UserStateService
) {
    val nextQuestion = homework.questions[currentIndex + 1]
    val processedQuestion = if (nextQuestion.questionText.contains("...")) {
        nextQuestion.questionText.replace("...", user.firstName)
    } else {
        nextQuestion.questionText
    }
    val questionText = "${S.questionTitle(currentIndex + 2)}\n\n$processedQuestion"

    when (nextQuestion.questionType) {
        QuestionType.MULTIPLE_CHOICE -> {
            val buttons = nextQuestion.options.mapIndexed { optIndex, option ->
                listOf(dataInlineButton(option, "answer:$homeworkId:${nextQuestion.id}:$optIndex"))
            }
            context.sendHtml(
                message.chat,
                questionText,
                replyMarkup = InlineKeyboardMarkup(buttons)
            )
        }
        else -> {
            val sentMessage = context.sendHtml(message.chat, "$questionText\n\n${S.writeYourAnswer}")
            val msgId = sentMessage.messageId.long
            HomeworkStateManager.setCurrentQuestion(telegramId, homeworkId, nextQuestion.id, msgId)
            userStateService.set(user.id, UserFlowState.HOMEWORK_TEXT.name, "{}")
        }
    }
}

internal suspend fun submitAndShowResults(
    context: BehaviourContext,
    message: CommonMessage<TextContent>,
    user: com.turki.core.domain.User,
    homework: com.turki.core.domain.Homework,
    homeworkId: Int,
    answers: MutableMap<Int, String>,
    homeworkService: HomeworkService,
    userStateService: UserStateService,
    progressService: ProgressService,
    analyticsService: AnalyticsService,
    lessonService: LessonService
) {
    val telegramId = user.telegramId
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

    val feedback = buildHomeworkFeedback(homework, answers, submission.score, submission.maxScore, homeworkService)
    val nextLesson = lessonService.getNextLesson(homework.lessonId, Language.TURKISH)
    val buttons = buildList {
        add(listOf(dataInlineButton(S.btnRepeatTopic, "lesson_start:${homework.lessonId}")))
        if (nextLesson != null) {
            add(listOf(dataInlineButton(S.btnNextLesson, "lesson_start:${nextLesson.id}")))
        }
        add(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))
    }
    val keyboard = InlineKeyboardMarkup(buttons)

    analyticsService.log("hw_autocheck_done", user.id, props = mapOf("score" to submission.score.toString()))
    analyticsService.log("hw_submitted", user.id, props = mapOf("score" to submission.score.toString()))

    context.sendHtml(message.chat, "$resultText\n\n$feedback", replyMarkup = keyboard)
}

internal suspend fun resolveHomeworkVocabularyId(
    lessonService: LessonService,
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

internal fun normalizeText(text: String): String =
    text.lowercase().replace(Regex("[^\\p{L}\\p{Nd}]"), "")

internal fun buildHomeworkFeedback(
    homework: com.turki.core.domain.Homework,
    answers: Map<Int, String>,
    score: Int,
    maxScore: Int,
    homeworkService: HomeworkService
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

internal fun currentQuestionIsCorrect(
    homework: com.turki.core.domain.Homework,
    questionId: Int,
    answer: String,
    homeworkService: HomeworkService
): Boolean {
    val question = homework.questions.firstOrNull { it.id == questionId } ?: return true
    return homeworkService.isAnswerCorrect(question, answer)
}

private val logger = LoggerFactory.getLogger("HomeworkHandler")
