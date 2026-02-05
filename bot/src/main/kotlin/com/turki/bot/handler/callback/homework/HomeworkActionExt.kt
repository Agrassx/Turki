package com.turki.bot.handler.callback.homework

import com.turki.bot.handler.HomeworkStateManager
import com.turki.bot.i18n.S
import com.turki.bot.i18n.Strings
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.HomeworkService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.UserFlowState
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import com.turki.bot.util.editOrSendHtml
import com.turki.bot.util.replaceWithHtml
import com.turki.core.domain.EventNames
import com.turki.core.domain.Homework
import com.turki.core.domain.HomeworkQuestion
import com.turki.core.domain.Language
import com.turki.core.domain.QuestionType
import com.turki.core.domain.User
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import org.slf4j.LoggerFactory

internal fun normalizeText(text: String): String =
    text.lowercase().replace(Regex("[^\\p{L}\\p{Nd}]"), "")

internal suspend fun resolveHomeworkVocabularyId(
    lessonService: LessonService,
    lessonId: Int,
    question: HomeworkQuestion
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

internal suspend fun buildHomeworkWrongButtons(
    lessonService: LessonService,
    homeworkId: Int,
    questionId: Int,
    lessonId: Int,
    question: HomeworkQuestion
) = buildList {
    val vocabId = resolveHomeworkVocabularyId(lessonService, lessonId, question)
    if (vocabId != null) {
        add(listOf(dataInlineButton(S.btnAddToDictionary, "hw_add_dict:$homeworkId:$questionId")))
    } else {
        add(listOf(dataInlineButton(S.btnAddCustomWord, "dict_add_custom")))
    }
    add(listOf(dataInlineButton(S.btnNext, "hw_next:$homeworkId:$questionId")))
}

internal fun buildHomeworkFeedback(
    homework: Homework,
    answers: Map<Int, String>,
    score: Int,
    maxScore: Int,
    homeworkService: HomeworkService,
    strings: Strings
): String {
    val wrong = homework.questions.filter { question ->
        !homeworkService.isAnswerCorrect(question, answers[question.id])
    }

    if (wrong.isEmpty()) {
        if (score < maxScore) {
            return "Есть ошибки — попробуйте ещё раз."
        }
        return strings.homeworkFeedbackPerfect
    }

    val lines = wrong.take(3).joinToString("\n") { question ->
        "• ${question.questionText}\n  ${strings.homeworkCorrectAnswer(question.correctAnswer)}"
    }

    return strings.homeworkFeedbackSummary(lines, wrong.size)
}

internal suspend fun renderHomeworkStart(
    context: BehaviourContext,
    query: DataCallbackQuery,
    lessonId: Int,
    userService: UserService,
    analyticsService: AnalyticsService
) {
    context.editOrSendHtml(
        query,
        S.homeworkStart,
        replyMarkup = InlineKeyboardMarkup(
            listOf(
                listOf(dataInlineButton(S.btnStartHomework, "start_homework:$lessonId")),
                listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu"))
            )
        )
    )
    val user = userService.findByTelegramId(query.from.id.chatId.long)
    if (user != null) {
        analyticsService.log(EventNames.HOMEWORK_STARTED, user.id, props = mapOf("lessonId" to lessonId.toString()))
    }
}

internal suspend fun sendQuestion(
    context: BehaviourContext,
    query: DataCallbackQuery,
    question: HomeworkQuestion,
    index: Int,
    homeworkId: Int,
    firstName: String?,
    userService: UserService,
    userStateService: UserStateService
) {
    val processedQuestion = if (firstName != null && question.questionText.contains("...")) {
        question.questionText.replace("...", firstName)
    } else {
        question.questionText
    }
    val questionText = "${S.questionTitle(index + 1)}\n\n$processedQuestion"

    when (question.questionType) {
        QuestionType.MULTIPLE_CHOICE -> {
            val buttons = question.options.mapIndexed { optIndex, option ->
                listOf(dataInlineButton(option, "answer:$homeworkId:${question.id}:$optIndex"))
            }
            context.editOrSendHtml(
                query,
                questionText,
                replyMarkup = InlineKeyboardMarkup(buttons)
            )
        }
        QuestionType.TEXT_INPUT, QuestionType.TRANSLATION -> {
            val sentMessage = context.replaceWithHtml(query, "$questionText\n\n${S.writeYourAnswer}")
            val messageId = sentMessage.messageId.long
            HomeworkStateManager.setCurrentQuestion(query.from.id.chatId.long, homeworkId, question.id, messageId)
            val user = userService.findByTelegramId(query.from.id.chatId.long)
            if (user != null) {
                userStateService.set(user.id, UserFlowState.HOMEWORK_TEXT.name, "{}")
            }
        }
    }
}

internal suspend fun submitHomeworkResult(
    context: BehaviourContext,
    query: DataCallbackQuery,
    user: User,
    homework: Homework,
    answers: Map<Int, String>,
    homeworkService: HomeworkService,
    userStateService: UserStateService,
    progressService: ProgressService,
    analyticsService: AnalyticsService,
    lessonService: LessonService
) {
    val submission = homeworkService.submitHomework(user.id, homework.id, answers)
    HomeworkStateManager.clearState(user.telegramId)
    userStateService.clear(user.id)
    progressService.recordHomework(user.id)
    if (submission.score == submission.maxScore) {
        progressService.markLessonCompleted(user.id, homework.lessonId)
        analyticsService.log(EventNames.LESSON_COMPLETED, user.id, props = mapOf("lessonId" to homework.lessonId.toString()))
    }
    analyticsService.log(EventNames.HOMEWORK_COMPLETED, user.id, props = mapOf("score" to submission.score.toString()))

    val resultText = if (submission.score == submission.maxScore) {
        S.homeworkComplete(submission.score, submission.maxScore)
    } else {
        S.homeworkResult(submission.score, submission.maxScore)
    }

    val feedback = buildHomeworkFeedback(homework, answers, submission.score, submission.maxScore, homeworkService, S)

    val nextLesson = lessonService.getNextLesson(homework.lessonId, Language.TURKISH)
    val buttons = buildList {
        add(listOf(dataInlineButton(S.btnRepeatTopic, "lesson_start:${homework.lessonId}")))
        if (nextLesson != null) {
            add(listOf(dataInlineButton(S.btnNextLesson, "lesson_start:${nextLesson.id}")))
        }
        add(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))
    }
    val keyboard = InlineKeyboardMarkup(buttons)

    context.editOrSendHtml(query, "$resultText\n\n$feedback", replyMarkup = keyboard)
}

internal suspend fun advanceHomework(
    context: BehaviourContext,
    query: DataCallbackQuery,
    parts: List<String>,
    userService: UserService,
    homeworkService: HomeworkService,
    userStateService: UserStateService,
    progressService: ProgressService,
    analyticsService: AnalyticsService,
    lessonService: LessonService
) {
    val telegramId = query.from.id.chatId.long
    if (parts.size < 3) {
        logger.warn("handleHomeworkNext: invalid parts count ${parts.size}")
        return
    }
    val homeworkId = parts[1].toIntOrNull() ?: run {
        logger.warn("handleHomeworkNext: invalid homeworkId '${parts[1]}'")
        return
    }
    val questionId = parts[2].toIntOrNull() ?: run {
        logger.warn("handleHomeworkNext: invalid questionId '${parts[2]}'")
        return
    }
    val user = userService.findByTelegramId(telegramId) ?: run {
        logger.warn("handleHomeworkNext: user not found for telegramId=$telegramId")
        return
    }
    val homework = homeworkService.getHomeworkById(homeworkId) ?: run {
        logger.warn("handleHomeworkNext: homework not found for homeworkId=$homeworkId")
        return
    }
    val currentIndex = homework.questions.indexOfFirst { it.id == questionId }
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
        val answers = HomeworkStateManager.getAnswers(user.telegramId)
        submitHomeworkResult(
            context,
            query,
            user,
            homework,
            answers,
            homeworkService,
            userStateService,
            progressService,
            analyticsService,
            lessonService
        )
    }
}

private val logger = LoggerFactory.getLogger("HomeworkActions")
