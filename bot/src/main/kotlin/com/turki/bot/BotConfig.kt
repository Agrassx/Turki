package com.turki.bot

import com.turki.bot.handler.CallbackHandler
import com.turki.bot.handler.CommandHandler
import com.turki.bot.handler.HomeworkHandler
import com.turki.bot.service.HomeworkService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ReminderService
import com.turki.bot.service.UserService
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import io.ktor.server.application.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

private val userService: UserService by inject(UserService::class.java)
private val lessonService: LessonService by inject(LessonService::class.java)
private val homeworkService: HomeworkService by inject(HomeworkService::class.java)
private val reminderService: ReminderService by inject(ReminderService::class.java)

fun Application.configureBot() {
    val token = EnvLoader.require("BOT_TOKEN")

    val bot = telegramBot(token)
    val commandHandler = CommandHandler(userService, lessonService)
    val callbackHandler = CallbackHandler(userService, lessonService, homeworkService, reminderService)
    val homeworkHandler = HomeworkHandler(homeworkService)

    CoroutineScope(Dispatchers.Default).launch {
        bot.buildBehaviourWithLongPolling {
            onCommand("start") { message ->
                commandHandler.handleStart(this, message)
            }

            onCommand("lesson") { message ->
                commandHandler.handleLesson(this, message)
            }

            onCommand("homework") { message ->
                commandHandler.handleHomework(this, message)
            }

            onCommand("progress") { message ->
                commandHandler.handleProgress(this, message)
            }

            onCommand("help") { message ->
                commandHandler.handleHelp(this, message)
            }

            onCommand("vocabulary") { message ->
                commandHandler.handleVocabulary(this, message)
            }

            onDataCallbackQuery { query ->
                callbackHandler.handleCallback(this, query)
            }

            onText { message ->
                if (!message.content.text.startsWith("/")) {
                    homeworkHandler.handleTextAnswer(this, message)
                }
            }
        }.join()
    }

    CoroutineScope(Dispatchers.Default).launch {
        startReminderScheduler(bot)
    }
}
