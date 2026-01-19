package com.turki.bot.handler

import com.turki.bot.service.HomeworkService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ReminderService
import com.turki.bot.service.UserService
import com.turki.bot.util.Messages
import com.turki.core.domain.Language
import com.turki.core.domain.QuestionType
import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

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
        }
    }

    private suspend fun handleLessonCallback(
        context: BehaviourContext,
        query: DataCallbackQuery,
        lessonId: Int?
    ) {
        if (lessonId == null) return

        val lesson = lessonService.getLessonById(lessonId) ?: run {
            context.sendMessage(query.from, Messages.LESSON_NOT_FOUND)
            return
        }

        val lessonText = buildString {
            appendLine("📚 *Урок ${lesson.orderIndex}: ${lesson.title}*")
            appendLine()
            appendLine(lesson.description)
            appendLine()
            appendLine("---")
            appendLine()
            appendLine(lesson.content)
        }

        context.sendMessage(
            query.from,
            lessonText,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton("📖 Словарь урока", "vocabulary:${lesson.id}")),
                    listOf(dataInlineButton("📝 Перейти к заданию", "homework:${lesson.id}")),
                    listOf(dataInlineButton("⏰ Напомнить о занятии", "set_reminder"))
                )
            )
        )
    }

    private suspend fun handleVocabularyCallback(
        context: BehaviourContext,
        query: DataCallbackQuery,
        lessonId: Int?
    ) {
        if (lessonId == null) return

        val vocabulary = lessonService.getVocabulary(lessonId)

        if (vocabulary.isEmpty()) {
            context.sendMessage(query.from, "Словарь для этого урока пока пуст.")
            return
        }

        val vocabText = buildString {
            appendLine("📖 *Словарь урока*")
            appendLine()
            vocabulary.forEach { item ->
                appendLine("• *${item.word}* — ${item.translation}")
                item.pronunciation?.let { appendLine("  🔊 [$it]") }
                item.example?.let { appendLine("  📝 _${it}_") }
                appendLine()
            }
        }

        context.sendMessage(
            query.from,
            vocabText,
            replyMarkup = InlineKeyboardMarkup(
                listOf(listOf(dataInlineButton("📝 Перейти к заданию", "homework:$lessonId")))
            )
        )
    }

    private suspend fun handleHomeworkCallback(
        context: BehaviourContext,
        query: DataCallbackQuery,
        lessonId: Int?
    ) {
        if (lessonId == null) return

        context.sendMessage(
            query.from,
            Messages.HOMEWORK_START,
            replyMarkup = InlineKeyboardMarkup(
                listOf(listOf(dataInlineButton("📝 Начать домашнее задание", "start_homework:$lessonId")))
            )
        )
    }

    private suspend fun handleStartHomework(
        context: BehaviourContext,
        query: DataCallbackQuery,
        lessonId: Int?
    ) {
        if (lessonId == null) return

        val homework = homeworkService.getHomeworkForLesson(lessonId) ?: run {
            context.sendMessage(query.from, "Домашнее задание для этого урока пока не готово.")
            return
        }

        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return

        if (homeworkService.hasCompletedHomework(user.id, homework.id)) {
            context.sendMessage(
                query.from,
                "Вы уже выполнили это задание! ✅",
                replyMarkup = InlineKeyboardMarkup(
                    listOf(listOf(dataInlineButton("➡️ Следующий урок", "next_lesson")))
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
        val questionText = "❓ *Вопрос ${index + 1}*\n\n${question.questionText}"

        when (question.questionType) {
            QuestionType.MULTIPLE_CHOICE -> {
                val buttons = question.options.mapIndexed { optIndex, option ->
                    listOf(dataInlineButton(option, "answer:$homeworkId:${question.id}:$optIndex:$option"))
                }
                context.sendMessage(
                    query.from,
                    questionText,
                    replyMarkup = InlineKeyboardMarkup(buttons)
                )
            }
            QuestionType.TEXT_INPUT, QuestionType.TRANSLATION -> {
                context.sendMessage(query.from, "$questionText\n\n_Напишите ваш ответ:_")
                HomeworkStateManager.setCurrentQuestion(query.from.id.chatId.long, homeworkId, question.id)
            }
        }
    }

    private suspend fun handleAnswer(
        context: BehaviourContext,
        query: DataCallbackQuery,
        parts: List<String>
    ) {
        if (parts.size < 5) return

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

            context.sendMessage(query.from, resultText, replyMarkup = keyboard)
        }
    }

    private suspend fun handleProgressCallback(context: BehaviourContext, query: DataCallbackQuery) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: run {
            context.sendMessage(query.from, Messages.NOT_REGISTERED)
            return
        }

        val totalLessons = lessonService.getLessonsByLanguage(Language.TURKISH).size
        val completedLessons = user.currentLessonId - 1

        val progressText = Messages.progress(
            firstName = user.firstName,
            completedLessons = completedLessons,
            totalLessons = totalLessons,
            subscriptionActive = user.subscriptionActive
        )

        context.sendMessage(query.from, progressText)
    }

    private suspend fun handleNextLessonCallback(context: BehaviourContext, query: DataCallbackQuery) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return

        val nextLesson = lessonService.getNextLesson(user.currentLessonId, Language.TURKISH)

        if (nextLesson == null) {
            context.sendMessage(query.from, Messages.ALL_LESSONS_COMPLETED)
            return
        }

        val lessonText = buildString {
            appendLine("📚 *Урок ${nextLesson.orderIndex}: ${nextLesson.title}*")
            appendLine()
            appendLine(nextLesson.description)
            appendLine()
            appendLine("---")
            appendLine()
            appendLine(nextLesson.content)
        }

        context.sendMessage(
            query.from,
            lessonText,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton("📖 Словарь урока", "vocabulary:${nextLesson.id}")),
                    listOf(dataInlineButton("📝 Перейти к заданию", "homework:${nextLesson.id}"))
                )
            )
        )
    }

    private suspend fun handleSetReminder(context: BehaviourContext, query: DataCallbackQuery) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        reminderService.createHomeworkReminder(user.id)
        context.sendMessage(query.from, Messages.REMINDER_SET)
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
