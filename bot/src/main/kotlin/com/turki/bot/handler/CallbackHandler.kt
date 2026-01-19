package com.turki.bot.handler

import com.turki.bot.i18n.S
import com.turki.bot.service.HomeworkService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ReminderService
import com.turki.bot.service.UserService
import com.turki.bot.util.markdownToHtml
import com.turki.bot.util.sendHtml
import com.turki.core.domain.Language
import com.turki.core.domain.QuestionType
import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

/**
 * Handler for Telegram bot inline callback queries.
 *
 * This class processes all inline button callbacks sent by users, including:
 * - Lesson navigation (lesson, next_lesson)
 * - Vocabulary display (vocabulary)
 * - Homework management (homework, start_homework, answer)
 * - Progress tracking (progress)
 * - Settings and configuration (settings, reset_progress, select_level)
 * - Reminders (set_reminder)
 * - Menu navigation (back_to_menu)
 *
 * Callback data format: `action:param1:param2:...`
 * Examples:
 * - `lesson:1` - Show lesson with ID 1
 * - `answer:5:10:2:Option` - Submit answer for homework 5, question 10, option index 2
 * - `vocabulary:3` - Show vocabulary for lesson 3
 *
 * All callbacks are processed asynchronously and send HTML-formatted responses.
 */
class CallbackHandler(
    private val userService: UserService,
    private val lessonService: LessonService,
    private val homeworkService: HomeworkService,
    private val reminderService: ReminderService
) {

    suspend fun handleCallback(context: BehaviourContext, query: DataCallbackQuery) {
        val data = query.data
        val parts = data.split(":")
        val action = parts[0]

        context.answer(query)

        when (action) {
            "lesson" -> handleLessonCallback(context, query, parts.getOrNull(1)?.toIntOrNull())
            "vocabulary" -> handleVocabularyCallback(context, query, parts.getOrNull(1)?.toIntOrNull())
            "homework" -> handleHomeworkCallback(context, query, parts.getOrNull(1)?.toIntOrNull())
            "start_homework" -> handleStartHomework(context, query, parts.getOrNull(1)?.toIntOrNull())
            "answer" -> handleAnswer(context, query, parts)
            "progress" -> handleProgressCallback(context, query)
            "next_lesson" -> handleNextLessonCallback(context, query)
            "set_reminder" -> handleSetReminder(context, query)
            "settings" -> handleSettings(context, query)
            "reset_progress" -> handleResetProgress(context, query)
            "confirm_reset" -> handleConfirmReset(context, query)
            "select_level" -> handleSelectLevel(context, query)
            "set_level" -> handleSetLevel(context, query, parts.getOrNull(1))
            "knowledge_test" -> handleKnowledgeTest(context, query)
            "back_to_menu" -> handleBackToMenu(context, query)
        }
    }

    private suspend fun handleLessonCallback(
        context: BehaviourContext,
        query: DataCallbackQuery,
        lessonId: Int?
    ) {
        if (lessonId == null) {
            return
        }

        val lesson = lessonService.getLessonById(lessonId) ?: run {
            context.sendHtml(query.from, S.lessonNotFound)
            return
        }

        val lessonText = buildString {
            appendLine(S.lessonTitle(lesson.orderIndex, lesson.title))
            appendLine()
            appendLine(lesson.description.markdownToHtml())
            appendLine()
            appendLine("─────────────────────")
            appendLine()
            appendLine(lesson.content.markdownToHtml())
        }

        context.sendHtml(
            query.from,
            lessonText,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.btnVocabulary, "vocabulary:${lesson.id}")),
                    listOf(dataInlineButton(S.btnGoToHomework, "homework:${lesson.id}")),
                    listOf(dataInlineButton(S.btnSetReminder, "set_reminder"))
                )
            )
        )
    }

    private suspend fun handleVocabularyCallback(
        context: BehaviourContext,
        query: DataCallbackQuery,
        lessonId: Int?
    ) {
        if (lessonId == null) {
            return
        }

        val vocabulary = lessonService.getVocabulary(lessonId)

        if (vocabulary.isEmpty()) {
            context.sendHtml(query.from, S.vocabularyEmpty)
            return
        }

        val vocabText = buildString {
            appendLine(S.vocabularyTitle)
            appendLine()
            vocabulary.forEach { item ->
                appendLine(S.vocabularyItem(item.word, item.translation))
                item.pronunciation?.let { appendLine(S.vocabularyPronunciation(it)) }
                item.example?.let { appendLine(S.vocabularyExample(it)) }
                appendLine()
            }
        }

        context.sendHtml(
            query.from,
            vocabText,
            replyMarkup = InlineKeyboardMarkup(
                listOf(listOf(dataInlineButton(S.btnGoToHomework, "homework:$lessonId")))
            )
        )
    }

    private suspend fun handleHomeworkCallback(
        context: BehaviourContext,
        query: DataCallbackQuery,
        lessonId: Int?
    ) {
        if (lessonId == null) {
            return
        }

        context.sendHtml(
            query.from,
            S.homeworkStart,
            replyMarkup = InlineKeyboardMarkup(
                listOf(listOf(dataInlineButton(S.btnStartHomework, "start_homework:$lessonId")))
            )
        )
    }

    private suspend fun handleStartHomework(
        context: BehaviourContext,
        query: DataCallbackQuery,
        lessonId: Int?
    ) {
        if (lessonId == null) {
            return
        }

        val homework = homeworkService.getHomeworkForLesson(lessonId) ?: run {
            context.sendHtml(query.from, S.homeworkNotReady)
            return
        }

        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return

        if (homeworkService.hasCompletedHomework(user.id, homework.id)) {
            context.sendHtml(
                query.from,
                S.homeworkAlreadyCompleted,
                replyMarkup = InlineKeyboardMarkup(
                    listOf(listOf(dataInlineButton(S.btnNextLesson, "next_lesson")))
                )
            )
            return
        }

        sendQuestion(context, query, homework.questions.first(), 0, homework.id)
    }

    private suspend fun sendQuestion(
        context: BehaviourContext,
        query: DataCallbackQuery,
        question: com.turki.core.domain.HomeworkQuestion,
        index: Int,
        homeworkId: Int
    ) {
        val questionText = "${S.questionTitle(index + 1)}\n\n${question.questionText}"

        when (question.questionType) {
            QuestionType.MULTIPLE_CHOICE -> {
                val buttons = question.options.mapIndexed { optIndex, option ->
                    listOf(dataInlineButton(option, "answer:$homeworkId:${question.id}:$optIndex:$option"))
                }
                context.sendHtml(
                    query.from,
                    questionText,
                    replyMarkup = InlineKeyboardMarkup(buttons)
                )
            }
            QuestionType.TEXT_INPUT, QuestionType.TRANSLATION -> {
                context.sendHtml(query.from, "$questionText\n\n${S.writeYourAnswer}")
                HomeworkStateManager.setCurrentQuestion(query.from.id.chatId.long, homeworkId, question.id)
            }
        }
    }

    private suspend fun handleAnswer(
        context: BehaviourContext,
        query: DataCallbackQuery,
        parts: List<String>
    ) {
        if (parts.size < 5) {
            return
        }

        val homeworkId = parts[1].toIntOrNull() ?: return
        val questionId = parts[2].toIntOrNull() ?: return
        val answer = parts[4]

        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        val currentAnswers = HomeworkStateManager.getAnswers(user.telegramId)
        currentAnswers[questionId] = answer
        HomeworkStateManager.setAnswers(user.telegramId, currentAnswers)

        val homework = homeworkService.getHomeworkForLesson(user.currentLessonId) ?: return
        val currentIndex = homework.questions.indexOfFirst { it.id == questionId }

        if (currentIndex < homework.questions.size - 1) {
            sendQuestion(context, query, homework.questions[currentIndex + 1], currentIndex + 1, homeworkId)
        } else {
            val submission = homeworkService.submitHomework(user.id, homeworkId, currentAnswers)
            HomeworkStateManager.clearState(user.telegramId)

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

            context.sendHtml(query.from, resultText, replyMarkup = keyboard)
        }
    }

    private suspend fun handleProgressCallback(context: BehaviourContext, query: DataCallbackQuery) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: run {
            context.sendHtml(query.from, S.notRegistered)
            return
        }

        val totalLessons = lessonService.getLessonsByLanguage(Language.TURKISH).size
        val completedLessons = user.currentLessonId - 1

        val progressText = S.progress(
            firstName = user.firstName,
            completedLessons = completedLessons,
            totalLessons = totalLessons,
            subscriptionActive = user.subscriptionActive
        )

        context.sendHtml(query.from, progressText)
    }

    private suspend fun handleNextLessonCallback(context: BehaviourContext, query: DataCallbackQuery) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return

        val nextLesson = lessonService.getNextLesson(user.currentLessonId, Language.TURKISH)

        if (nextLesson == null) {
            context.sendHtml(query.from, S.allLessonsCompleted)
            return
        }

        val lessonText = buildString {
            appendLine(S.lessonTitle(nextLesson.orderIndex, nextLesson.title))
            appendLine()
            appendLine(nextLesson.description.markdownToHtml())
            appendLine()
            appendLine("─────────────────────")
            appendLine()
            appendLine(nextLesson.content.markdownToHtml())
        }

        context.sendHtml(
            query.from,
            lessonText,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.btnVocabulary, "vocabulary:${nextLesson.id}")),
                    listOf(dataInlineButton(S.btnGoToHomework, "homework:${nextLesson.id}"))
                )
            )
        )
    }

    private suspend fun handleSetReminder(context: BehaviourContext, query: DataCallbackQuery) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        reminderService.createHomeworkReminder(user.id)
        context.sendHtml(query.from, S.reminderSet)
    }

    private suspend fun handleSettings(context: BehaviourContext, query: DataCallbackQuery) {
        context.sendHtml(
            query.from,
            S.settingsTitle,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.btnResetProgress, "reset_progress")),
                    listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu"))
                )
            )
        )
    }

    private suspend fun handleResetProgress(context: BehaviourContext, query: DataCallbackQuery) {
        context.sendHtml(
            query.from,
            S.resetProgressConfirm,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.btnConfirmReset, "confirm_reset")),
                    listOf(dataInlineButton(S.btnCancel, "settings"))
                )
            )
        )
    }

    private suspend fun handleConfirmReset(context: BehaviourContext, query: DataCallbackQuery) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        userService.resetProgress(user.id)

        context.sendHtml(
            query.from,
            S.progressResetSuccess,
            replyMarkup = InlineKeyboardMarkup(
                listOf(listOf(dataInlineButton("${S.btnStartLesson} 1", "lesson:1")))
            )
        )
    }

    private suspend fun handleSelectLevel(context: BehaviourContext, query: DataCallbackQuery) {
        context.sendHtml(
            query.from,
            S.selectLevelTitle,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(
                        dataInlineButton(S.btnLevelWithStatus("A1", true), "set_level:A1"),
                        dataInlineButton(S.btnLevelWithStatus("A2", false), "set_level:A2")
                    ),
                    listOf(
                        dataInlineButton(S.btnLevelWithStatus("B1", false), "set_level:B1"),
                        dataInlineButton(S.btnLevelWithStatus("B2", false), "set_level:B2")
                    ),
                    listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu"))
                )
            )
        )
    }

    private suspend fun handleSetLevel(context: BehaviourContext, query: DataCallbackQuery, level: String?) {
        if (level == null) {
            return
        }

        val message = when (level) {
            "A1" -> S.levelA1Active
            else -> S.levelLocked(level)
        }

        context.sendHtml(
            query.from,
            message,
            replyMarkup = InlineKeyboardMarkup(
                listOf(listOf(dataInlineButton(S.btnBack, "select_level")))
            )
        )
    }

    private suspend fun handleKnowledgeTest(context: BehaviourContext, query: DataCallbackQuery) {
        context.sendHtml(
            query.from,
            S.knowledgeTestTitle,
            replyMarkup = InlineKeyboardMarkup(
                listOf(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))
            )
        )
    }

    private suspend fun handleBackToMenu(context: BehaviourContext, query: DataCallbackQuery) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return

        context.sendHtml(
            query.from,
            S.mainMenuTitle,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.btnContinueLesson, "lesson:${user.currentLessonId}")),
                    listOf(dataInlineButton(S.btnHomework, "homework:${user.currentLessonId}")),
                    listOf(dataInlineButton(S.btnProgress, "progress")),
                    listOf(
                        dataInlineButton(S.btnSelectLevel, "select_level"),
                        dataInlineButton(S.btnKnowledgeTest, "knowledge_test")
                    ),
                    listOf(dataInlineButton(S.btnSettings, "settings"))
                )
            )
        )
    }
}

object HomeworkStateManager {
    private val currentQuestions = mutableMapOf<Long, Pair<Int, Int>>()
    private val userAnswers = mutableMapOf<Long, MutableMap<Int, String>>()

    fun setCurrentQuestion(telegramId: Long, homeworkId: Int, questionId: Int) {
        currentQuestions[telegramId] = homeworkId to questionId
    }

    fun getCurrentQuestion(telegramId: Long): Pair<Int, Int>? = currentQuestions[telegramId]

    fun getAnswers(telegramId: Long): MutableMap<Int, String> =
        userAnswers.getOrPut(telegramId) { mutableMapOf() }

    fun setAnswers(telegramId: Long, answers: MutableMap<Int, String>) {
        userAnswers[telegramId] = answers
    }

    fun clearState(telegramId: Long) {
        currentQuestions.remove(telegramId)
        userAnswers.remove(telegramId)
    }
}
