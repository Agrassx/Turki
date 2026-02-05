package com.turki.bot

import com.turki.bot.handler.CallbackHandler
import com.turki.bot.handler.CommandHandler
import com.turki.bot.handler.HomeworkHandler
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.DictionaryService
import com.turki.bot.service.HomeworkService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.ReminderPreferenceService
import com.turki.bot.service.SupportService
import com.turki.bot.service.UserDataService
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import com.turki.bot.service.UserFlowState
import com.turki.bot.i18n.S
import com.turki.bot.util.RateLimiter
import com.turki.bot.util.UpdateDeduper
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
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
private val userStateService: UserStateService by inject(UserStateService::class.java)
private val progressService: ProgressService by inject(ProgressService::class.java)
private val dictionaryService: DictionaryService by inject(DictionaryService::class.java)
private val reminderPreferenceService: ReminderPreferenceService by inject(ReminderPreferenceService::class.java)
private val analyticsService: AnalyticsService by inject(AnalyticsService::class.java)
private val userDataService: UserDataService by inject(UserDataService::class.java)
private val supportService: SupportService by inject(SupportService::class.java)
private val callbackHandler: CallbackHandler by inject(CallbackHandler::class.java)

private data class BotHandlers(
    val commandHandler: CommandHandler,
    val callbackHandler: CallbackHandler,
    val homeworkHandler: HomeworkHandler
)

fun Application.configureBot() {
    val bot = telegramBot(EnvLoader.require("BOT_TOKEN"))
    val deduper = UpdateDeduper()
    val rateLimiter = RateLimiter()
    val handlers = buildHandlers()

    CoroutineScope(Dispatchers.Default).launch {
        bot.buildBehaviourWithLongPolling {
            registerCommandHandlers(handlers.commandHandler)
            registerCallbackHandlers(handlers.callbackHandler, deduper, rateLimiter)
            registerUserTextHandlers(bot, handlers.commandHandler, handlers.homeworkHandler, deduper, rateLimiter)
        }.join()
    }

    launchSchedulers(bot)
}

private fun buildHandlers(): BotHandlers {
    val commandHandler = CommandHandler(
        userService,
        lessonService,
        progressService,
        dictionaryService,
        userStateService,
        analyticsService,
        reminderPreferenceService,
        userDataService
    )
    val homeworkHandler = HomeworkHandler(homeworkService)
    return BotHandlers(commandHandler, callbackHandler, homeworkHandler)
}

private suspend fun BehaviourContext.registerCommandHandlers(commandHandler: CommandHandler) {
    onCommand("start") { message -> commandHandler.handleStart(this, message) }
    onCommand("lesson") { message -> commandHandler.handleLesson(this, message) }
    onCommand("menu") { message -> commandHandler.handleMenu(this, message) }
    onCommand("lessons") { message -> commandHandler.handleLessons(this, message) }
    onCommand("practice") { message -> commandHandler.handlePractice(this, message) }
    onCommand("dictionary") { message -> commandHandler.handleDictionary(this, message) }
    onCommand("review") { message -> commandHandler.handleReview(this, message) }
    onCommand("homework") { message -> commandHandler.handleHomework(this, message) }
    onCommand("progress") { message -> commandHandler.handleProgress(this, message) }
    onCommand("reminders") { message -> commandHandler.handleReminders(this, message) }
    onCommand("reset") { message -> commandHandler.handleReset(this, message) }
    onCommand("delete") { message -> commandHandler.handleDelete(this, message) }
    onCommand("export") { message -> commandHandler.handleExport(this, message) }
    onCommand("support") { message -> commandHandler.handleSupport(this, message) }
    onCommand("help") { message -> commandHandler.handleHelp(this, message) }
    onCommand("vocabulary") { message -> commandHandler.handleVocabulary(this, message) }
}

private suspend fun BehaviourContext.registerCallbackHandlers(
    callbackHandler: CallbackHandler,
    deduper: UpdateDeduper,
    rateLimiter: RateLimiter
) {
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
}

private suspend fun BehaviourContext.registerUserTextHandlers(
    bot: dev.inmo.tgbotapi.bot.TelegramBot,
    commandHandler: CommandHandler,
    homeworkHandler: HomeworkHandler,
    deduper: UpdateDeduper,
    rateLimiter: RateLimiter
) {
    onText { message ->
        val text = message.content.text
        if (text.startsWith("/")) {
            return@onText
        }

        // Handle keyboard button presses (Russian labels)
        when (text) {
            "🏠 Меню" -> {
                commandHandler.handleMenu(this, message)
                return@onText
            }
            "📚 Уроки" -> {
                commandHandler.handleLessons(this, message)
                return@onText
            }
            "❓ Помощь" -> {
                commandHandler.handleHelp(this, message)
                return@onText
            }
        }

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
            UserFlowState.SUPPORT_MESSAGE.name -> {
                userStateService.clear(user.id)
                val sent = supportService.sendToAdmin(bot, user, text)
                sendMessage(message.chat, if (sent) S.supportSent else "❌ Ошибка отправки")
            }
            else -> homeworkHandler.handleTextAnswer(this, message)
        }
    }

    // Handle admin replies to support messages
    onText { message ->
        val supportChatId = EnvLoader.get("SUPPORT_CHAT_ID")?.toLongOrNull() ?: return@onText
        if (message.chat.id.chatId.long != supportChatId) return@onText

        // Check if this is a reply to another message
        val replyToMessage = message.replyTo ?: return@onText
        val replyText = (replyToMessage as? dev.inmo.tgbotapi.types.message.abstracts.ContentMessage<*>)
            ?.content
            ?.let { (it as? dev.inmo.tgbotapi.types.message.content.TextContent)?.text }
            ?: return@onText

        val userTelegramId = supportService.extractUserIdFromReply(replyText) ?: return@onText
        val adminReply = message.content.text
        supportService.sendReplyToUser(bot, userTelegramId, adminReply)
    }
}

private fun launchSchedulers(bot: dev.inmo.tgbotapi.bot.TelegramBot) {
    CoroutineScope(Dispatchers.Default).launch {
        startReminderScheduler(bot)
    }

    CoroutineScope(Dispatchers.Default).launch {
        startDailyReportScheduler(bot)
    }
}
