package com.turki.bot

import com.turki.bot.handler.CallbackHandler
import com.turki.bot.handler.CommandHandler
import com.turki.bot.handler.HomeworkHandler
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.DictionaryService
import com.turki.bot.service.ExerciseService
import com.turki.bot.service.HomeworkService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.ReminderPreferenceService
import com.turki.bot.service.ReminderService
import com.turki.bot.service.ReviewService
import com.turki.bot.service.UserDataService
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import com.turki.bot.service.UserFlowState
import com.turki.bot.util.RateLimiter
import com.turki.bot.util.UpdateDeduper
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import io.ktor.server.application.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

private val userService: UserService by inject(UserService::class.java)
private val lessonService: LessonService by inject(LessonService::class.java)
private val homeworkService: HomeworkService by inject(HomeworkService::class.java)
private val reminderService: ReminderService by inject(ReminderService::class.java)
private val userStateService: UserStateService by inject(UserStateService::class.java)
private val exerciseService: ExerciseService by inject(ExerciseService::class.java)
private val progressService: ProgressService by inject(ProgressService::class.java)
private val dictionaryService: DictionaryService by inject(DictionaryService::class.java)
private val reviewService: ReviewService by inject(ReviewService::class.java)
private val reminderPreferenceService: ReminderPreferenceService by inject(ReminderPreferenceService::class.java)
private val analyticsService: AnalyticsService by inject(AnalyticsService::class.java)
private val userDataService: UserDataService by inject(UserDataService::class.java)

fun Application.configureBot() {
    val token = EnvLoader.require("BOT_TOKEN")

    val bot = telegramBot(token)
    val deduper = UpdateDeduper()
    val rateLimiter = RateLimiter()
    val commandHandler = CommandHandler(
        userService,
        lessonService,
        progressService,
        dictionaryService,
        reviewService,
        exerciseService,
        userStateService,
        analyticsService,
        reminderPreferenceService
    )
    val callbackHandler = CallbackHandler(
        userService,
        lessonService,
        homeworkService,
        reminderService,
        userStateService,
        exerciseService,
        progressService,
        dictionaryService,
        reviewService,
        reminderPreferenceService,
        analyticsService,
        userDataService
    )
    val homeworkHandler = HomeworkHandler(homeworkService)

    CoroutineScope(Dispatchers.Default).launch {
        bot.buildBehaviourWithLongPolling {
            onCommand("start") { message ->
                commandHandler.handleStart(this, message)
            }

            onCommand("lesson") { message ->
                commandHandler.handleLesson(this, message)
            }

            onCommand("menu") { message ->
                commandHandler.handleMenu(this, message)
            }

            onCommand("lessons") { message ->
                commandHandler.handleLessons(this, message)
            }

            onCommand("practice") { message ->
                commandHandler.handlePractice(this, message)
            }

            onCommand("dictionary") { message ->
                commandHandler.handleDictionary(this, message)
            }

            onCommand("review") { message ->
                commandHandler.handleReview(this, message)
            }

            onCommand("homework") { message ->
                commandHandler.handleHomework(this, message)
            }

            onCommand("progress") { message ->
                commandHandler.handleProgress(this, message)
            }

            onCommand("reminders") { message ->
                commandHandler.handleReminders(this, message)
            }

            onCommand("reset") { message ->
                commandHandler.handleReset(this, message)
            }

            onCommand("delete") { message ->
                commandHandler.handleDelete(this, message)
            }

            onCommand("help") { message ->
                commandHandler.handleHelp(this, message)
            }

            onCommand("vocabulary") { message ->
                commandHandler.handleVocabulary(this, message)
            }

            onDataCallbackQuery { query ->
                val userId = query.from.id.chatId.long
                if (!rateLimiter.allow(userId)) {
                    return@onDataCallbackQuery
                }
                if (!deduper.shouldProcess("cb:${query.id}")) {
                    return@onDataCallbackQuery
                }
                callbackHandler.handleCallback(this, query)
            }

            onText { message ->
                if (!message.content.text.startsWith("/")) {
                    val from = message.from ?: return@onText
                    val user = userService.findByTelegramId(from.id.chatId.long) ?: return@onText
                    if (!rateLimiter.allow(user.id)) {
                        return@onText
                    }
                    if (!deduper.shouldProcess("msg:${message.messageId}")) {
                        return@onText
                    }
                    val state = userStateService.get(user.id)
                    when (state?.state) {
                        UserFlowState.DICT_SEARCH.name -> {
                            userStateService.clear(user.id)
                            commandHandler.handleDictionaryQueryText(this, message)
                        }
                        UserFlowState.DICT_ADD_CUSTOM.name -> {
                            userStateService.clear(user.id)
                            commandHandler.handleDictionaryCustomText(this, message)
                        }
                        else -> homeworkHandler.handleTextAnswer(this, message)
                    }
                }
            }
        }.join()
    }

    CoroutineScope(Dispatchers.Default).launch {
        startReminderScheduler(bot)
    }
}
