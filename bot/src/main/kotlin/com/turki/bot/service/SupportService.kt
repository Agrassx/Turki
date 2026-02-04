package com.turki.bot.service

import com.turki.bot.EnvLoader
import com.turki.bot.i18n.S
import com.turki.core.domain.User
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.message.HTMLParseMode
import org.slf4j.LoggerFactory

/**
 * Service for handling support messages between users and admin.
 */
class SupportService {
    private val logger = LoggerFactory.getLogger("SupportService")
    private val supportChatId: Long? = EnvLoader.get("SUPPORT_CHAT_ID")?.toLongOrNull()

    /**
     * Sends a support message from user to admin chat.
     * Returns true if message was sent successfully.
     */
    suspend fun sendToAdmin(bot: TelegramBot, user: User, message: String): Boolean {
        if (supportChatId == null) {
            logger.warn("SUPPORT_CHAT_ID not configured, support message dropped")
            return false
        }

        return try {
            bot.sendMessage(
                chatId = ChatId(RawChatId(supportChatId)),
                text = S.supportMessageToAdmin(
                    userId = user.telegramId,
                    username = user.username,
                    firstName = user.firstName,
                    message = message
                ),
                parseMode = HTMLParseMode
            )
            logger.info("Support message sent from user ${user.telegramId}")
            true
        } catch (e: Exception) {
            logger.error("Failed to send support message: ${e.message}")
            false
        }
    }

    /**
     * Sends a reply from admin to user.
     */
    suspend fun sendReplyToUser(bot: TelegramBot, userTelegramId: Long, replyText: String): Boolean {
        return try {
            bot.sendMessage(
                chatId = ChatId(RawChatId(userTelegramId)),
                text = S.supportReply + replyText,
                parseMode = HTMLParseMode
            )
            logger.info("Support reply sent to user $userTelegramId")
            true
        } catch (e: Exception) {
            logger.error("Failed to send reply to user $userTelegramId: ${e.message}")
            false
        }
    }

    /**
     * Extracts user ID from admin's reply message.
     * Returns null if the message is not a valid support message reply.
     */
    fun extractUserIdFromReply(replyText: String): Long? {
        // Look for 🆔 <code>123456789</code> pattern
        val regex = """🆔\s*<code>(\d+)</code>""".toRegex()
        val match = regex.find(replyText)
        return match?.groupValues?.get(1)?.toLongOrNull()
    }

    fun isConfigured(): Boolean = supportChatId != null
}
