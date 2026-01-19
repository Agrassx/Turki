package com.turki.bot

import com.turki.bot.i18n.S
import com.turki.bot.service.ReminderService
import com.turki.bot.service.UserService
import com.turki.core.domain.ReminderType
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.message.HTMLParseMode
import kotlinx.coroutines.delay
import org.koin.java.KoinJavaComponent.inject
import kotlin.time.Duration.Companion.minutes

private val reminderService: ReminderService by inject(ReminderService::class.java)
private val userService: UserService by inject(UserService::class.java)

suspend fun startReminderScheduler(bot: TelegramBot) {
    while (true) {
        try {
            val pendingReminders = reminderService.getPendingReminders()

            for (reminder in pendingReminders) {
                val user = userService.getAllUsers().find { it.id == reminder.userId } ?: continue

                val message = when (reminder.type) {
                    ReminderType.LESSON_REMINDER -> S.reminderLesson
                    ReminderType.HOMEWORK_REMINDER -> S.reminderHomework
                    ReminderType.SUBSCRIPTION_EXPIRING -> S.reminderSubscription
                }

                try {
                    bot.sendMessage(
                        chatId = ChatId(RawChatId(user.telegramId)),
                        text = message,
                        parseMode = HTMLParseMode
                    )
                    reminderService.markReminderAsSent(reminder.id)
                } catch (e: Exception) {
                    println("Failed to send reminder to user ${user.telegramId}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("Error in reminder scheduler: ${e.message}")
        }

        delay(1.minutes)
    }
}
