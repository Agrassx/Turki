package com.turki.bot

import com.turki.bot.handler.CallbackHandler
import com.turki.bot.handler.CommandHandler
import com.turki.bot.handler.HomeworkHandler
import com.turki.bot.service.AnalyticsService
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
import io.ktor.server.application.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

private val userService: UserService by inject(UserService::class.java)
private val userStateService: UserStateService by inject(UserStateService::class.java)
private val supportService: SupportService by inject(SupportService::class.java)
private val callbackHandler: CallbackHandler by inject(CallbackHandler::class.java)
private val commandHandler: CommandHandler by inject(CommandHandler::class.java)
private val homeworkHandler: HomeworkHandler by inject(HomeworkHandler::class.java)
private val analyticsService: AnalyticsService by inject(AnalyticsService::class.java)

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
    return BotHandlers(commandHandler, callbackHandler, homeworkHandler)
}

private suspend fun BehaviourContext.registerCommandHandlers(commandHandler: CommandHandler) {
    onCommand("start") { message -> commandHandler.handleCommand("start", this, message) }
    onCommand("lesson") { message -> commandHandler.handleCommand("lesson", this, message) }
    onCommand("menu") { message -> commandHandler.handleCommand("menu", this, message) }
    onCommand("lessons") { message -> commandHandler.handleCommand("lessons", this, message) }
    onCommand("practice") { message -> commandHandler.handleCommand("practice", this, message) }
    onCommand("dictionary") { message -> commandHandler.handleCommand("dictionary", this, message) }
    onCommand("review") { message -> commandHandler.handleCommand("review", this, message) }
    onCommand("homework") { message -> commandHandler.handleCommand("homework", this, message) }
    onCommand("progress") { message -> commandHandler.handleCommand("progress", this, message) }
    onCommand("reminders") { message -> commandHandler.handleCommand("reminders", this, message) }
    onCommand("reset") { message -> commandHandler.handleCommand("reset", this, message) }
    onCommand("delete") { message -> commandHandler.handleCommand("delete", this, message) }
    onCommand("export") { message -> commandHandler.handleCommand("export", this, message) }
    onCommand("support") { message -> commandHandler.handleCommand("support", this, message) }
    onCommand("help") { message -> commandHandler.handleCommand("help", this, message) }
    onCommand("vocabulary") { message -> commandHandler.handleCommand("vocabulary", this, message) }
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
