package com.turki.bot.service

import com.turki.bot.EnvLoader
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.message.HTMLParseMode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.slf4j.LoggerFactory

private const val MAX_MESSAGE_LENGTH = 4000
private const val MAX_STACK_TRACE_LENGTH = 1500
private const val DEDUP_WINDOW_MS = 60_000L
private const val MAX_MESSAGES_PER_MINUTE = 10

/**
 * Service that sends error notifications to a dedicated Telegram chat in real-time.
 * Includes rate limiting and deduplication to prevent message flooding.
 */
class ErrorNotifierService(
    private val clock: Clock = Clock.System
) {
    private val logger = LoggerFactory.getLogger("ErrorNotifier")
    private val errorChatId: Long? = EnvLoader.get("ERROR_CHAT_ID")?.toLongOrNull()

    private var bot: TelegramBot? = null
    private val mutex = Mutex()
    private val recentErrors = mutableListOf<ErrorEntry>()

    private data class ErrorEntry(val key: String, val timestamp: Long)

    fun setBot(telegramBot: TelegramBot) {
        bot = telegramBot
    }

    fun isConfigured(): Boolean = errorChatId != null

    /**
     * Send an error notification to the error chat.
     * Rate-limited and deduplicated to avoid flooding.
     */
    suspend fun notify(
        errorType: String,
        message: String,
        exception: Throwable? = null,
        userId: Long? = null,
        context: String? = null
    ) {
        val currentBot = bot
        if (errorChatId == null || currentBot == null) {
            return
        }

        val deduplicationKey = "$errorType:${message.take(100)}"
        if (!shouldSend(deduplicationKey)) {
            return
        }

        try {
            val text = formatErrorMessage(errorType, message, exception, userId, context)
            currentBot.sendMessage(
                chatId = ChatId(RawChatId(errorChatId)),
                text = text,
                parseMode = HTMLParseMode
            )
        } catch (e: Exception) {
            logger.error("Failed to send error notification: ${e.message}")
        }
    }

    private suspend fun shouldSend(key: String): Boolean = mutex.withLock {
        val now = clock.now().toEpochMilliseconds()

        // Clean old entries
        recentErrors.removeAll { now - it.timestamp > DEDUP_WINDOW_MS }

        // Check rate limit
        if (recentErrors.size >= MAX_MESSAGES_PER_MINUTE) {
            return@withLock false
        }

        // Check deduplication
        if (recentErrors.any { it.key == key }) {
            return@withLock false
        }

        recentErrors.add(ErrorEntry(key, now))
        true
    }

    private fun formatErrorMessage(
        errorType: String,
        message: String,
        exception: Throwable?,
        userId: Long?,
        context: String?
    ): String {
        val now = clock.now()
        val moscowTime = now.toLocalDateTime(TimeZone.of("Europe/Moscow"))

        val sb = StringBuilder()
        sb.appendLine("🚨 <b>Ошибка!</b>")
        sb.appendLine()
        sb.appendLine("📋 <b>Тип:</b> <code>$errorType</code>")
        sb.appendLine("💬 <b>Сообщение:</b> ${escapeHtml(message)}")

        if (userId != null) {
            sb.appendLine("👤 <b>User ID:</b> <code>$userId</code>")
        }

        if (context != null) {
            sb.appendLine("📎 <b>Контекст:</b> ${escapeHtml(context)}")
        }

        sb.appendLine("⏰ <b>Время:</b> $moscowTime")

        if (exception != null) {
            val stackTrace = exception.stackTraceToString()
                .take(MAX_STACK_TRACE_LENGTH)
                .let { if (it.length == MAX_STACK_TRACE_LENGTH) "$it\n..." else it }
            sb.appendLine()
            sb.appendLine("<pre>${escapeHtml(stackTrace)}</pre>")
        }

        val result = sb.toString()
        return if (result.length > MAX_MESSAGE_LENGTH) result.take(MAX_MESSAGE_LENGTH) + "..." else result
    }

    private fun escapeHtml(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
