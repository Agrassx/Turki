package com.turki.bot.handler

import com.turki.bot.i18n.S
import com.turki.bot.service.LessonService
import com.turki.bot.service.UserService
import com.turki.bot.util.markdownToHtml
import com.turki.bot.util.sendHtml
import com.turki.core.domain.Language
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent

/**
 * Handler for Telegram bot text commands.
 *
 * This class processes all text commands sent by users to the bot, including:
 * - /start - User registration and welcome message
 * - /lesson - Display current lesson
 * - /homework - Show homework options
 * - /progress - Display user progress
 * - /help - Show help information
 * - /vocabulary - Display lesson vocabulary
 *
 * All commands are processed asynchronously and send HTML-formatted responses.
 */
class CommandHandler(
    private val userService: UserService,
    private val lessonService: LessonService
) {

    suspend fun handleStart(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val user = userService.findOrCreateUser(
            telegramId = from.id.chatId.long,
            username = from.username?.username,
            firstName = from.firstName,
            lastName = from.lastName
        )

        val welcomeMessage = S.welcome(user.firstName)

        context.sendHtml(
            message.chat,
            welcomeMessage,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.btnStartLesson, "lesson:${user.currentLessonId}")),
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

    suspend fun handleLesson(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val user = userService.findByTelegramId(from.id.chatId.long) ?: run {
            context.sendHtml(message.chat, S.notRegistered)
            return
        }

        val lesson = lessonService.getLessonById(user.currentLessonId)

        if (lesson == null) {
            context.sendHtml(message.chat, S.allLessonsCompleted)
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
            message.chat,
            lessonText,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.btnVocabulary, "vocabulary:${lesson.id}")),
                    listOf(dataInlineButton(S.btnGoToHomework, "homework:${lesson.id}"))
                )
            )
        )
    }

    suspend fun handleHomework(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from
        if (from == null) {
            return
        }
        val user = userService.findByTelegramId(from.id.chatId.long)
        if (user == null) {
            context.sendHtml(message.chat, S.notRegistered)
            return
        }

        context.sendHtml(
            message.chat,
            S.homeworkStart,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.btnStartHomework, "start_homework:${user.currentLessonId}"))
                )
            )
        )
    }

    suspend fun handleProgress(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from
        if (from == null) {
            return
        }
        val user = userService.findByTelegramId(from.id.chatId.long)
        if (user == null) {
            context.sendHtml(message.chat, S.notRegistered)
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

        context.sendHtml(message.chat, progressText)
    }

    suspend fun handleHelp(context: BehaviourContext, message: CommonMessage<TextContent>) {
        context.sendHtml(message.chat, S.help)
    }

    suspend fun handleVocabulary(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from
        if (from == null) {
            return
        }
        val user = userService.findByTelegramId(from.id.chatId.long)
        if (user == null) {
            context.sendHtml(message.chat, S.notRegistered)
            return
        }

        val vocabulary = lessonService.getVocabulary(user.currentLessonId)

        if (vocabulary.isEmpty()) {
            context.sendHtml(message.chat, S.vocabularyEmpty)
            return
        }

        val vocabText = buildString {
            appendLine(S.vocabularyForLesson(user.currentLessonId))
            appendLine()
            vocabulary.forEach { item ->
                appendLine(S.vocabularyItem(item.word, item.translation))
                item.pronunciation?.let { appendLine(S.vocabularyPronunciation(it)) }
                item.example?.let { appendLine(S.vocabularyExample(it)) }
                appendLine()
            }
        }

        context.sendHtml(message.chat, vocabText)
    }
}
