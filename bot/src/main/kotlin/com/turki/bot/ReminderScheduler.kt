package com.turki.bot

import com.turki.bot.service.ReminderService
import com.turki.bot.service.UserService
import com.turki.core.domain.ReminderType
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
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
                    ReminderType.LESSON_REMINDER -> """
                        |⏰ *Напоминание о занятии!*
                        |
                        |Пора продолжить изучение турецкого языка!
                        |
                        |Отправьте /lesson чтобы продолжить обучение.
                    """.trimMargin()

                    ReminderType.HOMEWORK_REMINDER -> """
                        |📝 *Напоминание о домашнем задании!*
                        |
                        |Не забудьте выполнить домашнее задание.
                        |
                        |Отправьте /homework чтобы начать.
                    """.trimMargin()

                    ReminderType.SUBSCRIPTION_EXPIRING -> """
                        |⚠️ *Ваша подписка скоро заканчивается!*
                        |
                        |Продлите подписку, чтобы продолжить обучение.
                    """.trimMargin()
                }

                try {
                    bot.sendMessage(ChatId(RawChatId(user.telegramId)), message)
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
