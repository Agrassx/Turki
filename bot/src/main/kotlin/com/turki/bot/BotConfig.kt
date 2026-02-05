package com.turki.bot

import com.turki.bot.handler.CallbackHandler
import com.turki.bot.handler.CommandHandler
import com.turki.bot.handler.HomeworkHandler
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.ErrorNotifierService
import com.turki.bot.service.SupportService
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import com.turki.bot.service.UserFlowState
import com.turki.bot.i18n.S
import com.turki.core.domain.EventNames
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
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import io.ktor.server.application.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import org.slf4j.LoggerFactory

private val userService: UserService by inject(UserService::class.java)
private val userStateService: UserStateService by inject(UserStateService::class.java)
private val supportService: SupportService by inject(SupportService::class.java)
private val callbackHandler: CallbackHandler by inject(CallbackHandler::class.java)
private val commandHandler: CommandHandler by inject(CommandHandler::class.java)
private val homeworkHandler: HomeworkHandler by inject(HomeworkHandler::class.java)
private val analyticsService: AnalyticsService by inject(AnalyticsService::class.java)
private val errorNotifier: ErrorNotifierService by inject(ErrorNotifierService::class.java)

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

    errorNotifier.setBot(bot)

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
    return BotHandlers(commandHandler, callbackHandler, homeworkHandler)
}

private val botLogger = LoggerFactory.getLogger("BotConfig")

private suspend fun BehaviourContext.safeCommand(
    handler: CommandHandler,
    cmd: String,
    message: CommonMessage<TextContent>
) {
    try {
        handler.handleCommand(cmd, this, message)
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        val userId = message.from?.id?.chatId?.long
        botLogger.error("Command error [/$cmd]: ${e.message}", e)
        errorNotifier.notify("CommandError", e.message ?: "Unknown", e, userId, "cmd=/$cmd")
    }
}

private suspend fun BehaviourContext.registerCommandHandlers(commandHandler: CommandHandler) {
    onCommand("start") { message -> safeCommand(commandHandler, "start", message) }
    onCommand("lesson") { message -> safeCommand(commandHandler, "lesson", message) }
    onCommand("menu") { message -> safeCommand(commandHandler, "menu", message) }
    onCommand("lessons") { message -> safeCommand(commandHandler, "lessons", message) }
    onCommand("practice") { message -> safeCommand(commandHandler, "practice", message) }
    onCommand("dictionary") { message -> safeCommand(commandHandler, "dictionary", message) }
    onCommand("review") { message -> safeCommand(commandHandler, "review", message) }
    onCommand("homework") { message -> safeCommand(commandHandler, "homework", message) }
    onCommand("progress") { message -> safeCommand(commandHandler, "progress", message) }
    onCommand("reminders") { message -> safeCommand(commandHandler, "reminders", message) }
    onCommand("reset") { message -> safeCommand(commandHandler, "reset", message) }
    onCommand("delete") { message -> safeCommand(commandHandler, "delete", message) }
    onCommand("export") { message -> safeCommand(commandHandler, "export", message) }
    onCommand("support") { message -> safeCommand(commandHandler, "support", message) }
    onCommand("help") { message -> safeCommand(commandHandler, "help", message) }
    onCommand("vocabulary") { message -> safeCommand(commandHandler, "vocabulary", message) }
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
        try {
            callbackHandler.handleCallback(this, query)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            LoggerFactory.getLogger("BotConfig").error("Callback error [${query.data}]: ${e.message}", e)
            errorNotifier.notify("CallbackError", e.message ?: "Unknown", e, userId, "data=${query.data}")
        }
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
        try {
            val text = message.content.text
            if (text.startsWith("/")) {
                return@onText
            }

            // Handle keyboard button presses (Russian labels)
            when (text) {
                "🏠 Меню" -> {
                    commandHandler.handleCommand("menu", this, message)
                    return@onText
                }
                "📚 Уроки" -> {
                    commandHandler.handleCommand("lessons", this, message)
                    return@onText
                }
                "❓ Помощь" -> {
                    commandHandler.handleCommand("help", this, message)
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
                    commandHandler.handleTextState(UserFlowState.DICT_SEARCH, this, message)
                }
                UserFlowState.DICT_ADD_CUSTOM.name -> {
                    userStateService.clear(user.id)
                    commandHandler.handleTextState(UserFlowState.DICT_ADD_CUSTOM, this, message)
                }
                UserFlowState.SUPPORT_MESSAGE.name -> {
                    userStateService.clear(user.id)
                    val sent = supportService.sendToAdmin(bot, user, text)
                    sendMessage(message.chat, if (sent) S.supportSent else "❌ Ошибка отправки")
                    if (sent) {
                        analyticsService.log(EventNames.SUPPORT_MESSAGE_SENT, user.id)
                    }
                }
                else -> homeworkHandler.handleTextAnswer(this, message)
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            val userId = message.from?.id?.chatId?.long
            botLogger.error("Text handler error: ${e.message}", e)
            errorNotifier.notify("TextHandlerError", e.message ?: "Unknown", e, userId)
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
        val replied = supportService.sendReplyToUser(bot, userTelegramId, adminReply)
        if (replied) {
            val targetUser = userService.findByTelegramId(userTelegramId)
            if (targetUser != null) {
                analyticsService.log(EventNames.SUPPORT_REPLY_SENT, targetUser.id)
            }
        }
    }
}

private fun launchSchedulers(bot: dev.inmo.tgbotapi.bot.TelegramBot) {
    CoroutineScope(Dispatchers.Default).launch {
        startReminderScheduler(bot)
    }

    CoroutineScope(Dispatchers.Default).launch {
        sendStartupReport(bot)
    }

    CoroutineScope(Dispatchers.Default).launch {
        startDailyReportScheduler(bot)
    }
}
