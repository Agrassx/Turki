package com.turki.bot.handler

import com.turki.bot.service.LessonService
import com.turki.bot.service.UserService
import com.turki.bot.util.Messages
import com.turki.core.domain.Language
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent

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

        val welcomeMessage = Messages.welcome(user.firstName)

        context.sendMessage(
            message.chat,
            welcomeMessage,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton("📚 Начать урок", "lesson:${user.currentLessonId}")),
                    listOf(dataInlineButton("📝 Домашнее задание", "homework:${user.currentLessonId}")),
                    listOf(dataInlineButton("📊 Мой прогресс", "progress"))
                )
            )
        )
    }

    suspend fun handleLesson(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val user = userService.findByTelegramId(from.id.chatId.long) ?: run {
            context.sendMessage(message.chat, Messages.NOT_REGISTERED)
            return
        }

        val lesson = lessonService.getLessonById(user.currentLessonId)

        if (lesson == null) {
            context.sendMessage(message.chat, Messages.ALL_LESSONS_COMPLETED)
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
            message.chat,
            lessonText,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton("📖 Словарь урока", "vocabulary:${lesson.id}")),
                    listOf(dataInlineButton("📝 Перейти к заданию", "homework:${lesson.id}"))
                )
            )
        )
    }

    suspend fun handleHomework(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val user = userService.findByTelegramId(from.id.chatId.long) ?: run {
            context.sendMessage(message.chat, Messages.NOT_REGISTERED)
            return
        }

        context.sendMessage(
            message.chat,
            Messages.HOMEWORK_START,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton("📝 Начать домашнее задание", "start_homework:${user.currentLessonId}"))
                )
            )
        )
    }

    suspend fun handleProgress(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val user = userService.findByTelegramId(from.id.chatId.long) ?: run {
            context.sendMessage(message.chat, Messages.NOT_REGISTERED)
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

        context.sendMessage(message.chat, progressText)
    }

    suspend fun handleHelp(context: BehaviourContext, message: CommonMessage<TextContent>) {
        context.sendMessage(message.chat, Messages.HELP)
    }

    suspend fun handleVocabulary(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val user = userService.findByTelegramId(from.id.chatId.long) ?: run {
            context.sendMessage(message.chat, Messages.NOT_REGISTERED)
            return
        }

        val vocabulary = lessonService.getVocabulary(user.currentLessonId)

        if (vocabulary.isEmpty()) {
            context.sendMessage(message.chat, "Словарь для этого урока пока пуст.")
            return
        }

        val vocabText = buildString {
            appendLine("📖 *Словарь урока ${user.currentLessonId}*")
            appendLine()
            vocabulary.forEach { item ->
                appendLine("• *${item.word}* — ${item.translation}")
                item.pronunciation?.let { appendLine("  🔊 [$it]") }
                item.example?.let { appendLine("  📝 _${it}_") }
                appendLine()
            }
        }

        context.sendMessage(message.chat, vocabText)
    }
}
