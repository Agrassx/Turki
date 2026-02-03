package com.turki.bot

import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.ReminderPreferenceService
import com.turki.bot.service.ReminderService
import com.turki.bot.service.UserService
import com.turki.core.domain.ReminderType
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.message.HTMLParseMode
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.delay
import org.koin.java.KoinJavaComponent.inject
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.minutes

private val logger = LoggerFactory.getLogger("ReminderScheduler")

private val reminderService: ReminderService by inject(ReminderService::class.java)
private val userService: UserService by inject(UserService::class.java)
private val reminderPreferenceService: ReminderPreferenceService by inject(ReminderPreferenceService::class.java)
private val progressService: ProgressService by inject(ProgressService::class.java)
private val analyticsService: AnalyticsService by inject(AnalyticsService::class.java)

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
                    logger.warn("Failed to send reminder to user: ${e.message}")
                }
            }

            sendScheduledReminders(bot)
            sendWeeklyReports(bot)
        } catch (e: Exception) {
            logger.error("Error in reminder scheduler: ${e.message}")
        }

        delay(1.minutes)
    }
}

private suspend fun sendScheduledReminders(bot: TelegramBot) {
    val users = userService.getAllUsers()
    val now = Clock.System.now()
    val local = now.toLocalDateTime(TimeZone.currentSystemDefault())
    val currentDay = local.date.dayOfWeek
    val currentTime = "%02d:%02d".format(local.hour, local.minute)

    users.forEach { user ->
        val pref = reminderPreferenceService.getOrDefault(user.id)
        if (!pref.isEnabled) {
            return@forEach
        }
        if (!pref.daysOfWeek.split(",").contains(currentDay.name)) {
            return@forEach
        }
        if (pref.timeLocal != currentTime) {
            return@forEach
        }
        val lastFired = pref.lastFiredAt?.toLocalDateTime(TimeZone.currentSystemDefault())?.date
        if (lastFired == local.date) {
            return@forEach
        }

        bot.sendMessage(
            chatId = ChatId(RawChatId(user.telegramId)),
            text = S.reminderLesson,
            parseMode = HTMLParseMode
        )
        reminderPreferenceService.markFired(user.id)
        analyticsService.log("reminder_fired", user.id)
    }
}

private suspend fun sendWeeklyReports(bot: TelegramBot) {
    val users = userService.getAllUsers()
    val now = Clock.System.now()
    val zone = TimeZone.currentSystemDefault()
    val local = now.toLocalDateTime(zone)
    if (local.date.dayOfWeek != DayOfWeek.SUNDAY) {
        return
    }

    users.forEach { user ->
        val stats = progressService.getWeeklyStats(user.id)
        val lastReportDate = stats.lastWeeklyReportAt?.toLocalDateTime(zone)?.date
        if (lastReportDate == local.date) {
            return@forEach
        }
        val report = S.weeklyReport(
            lessons = stats.weeklyLessons,
            practice = stats.weeklyPractice,
            review = stats.weeklyReview,
            homework = stats.weeklyHomework
        )
        bot.sendMessage(
            chatId = ChatId(RawChatId(user.telegramId)),
            text = report,
            parseMode = HTMLParseMode
        )
        progressService.resetWeekly(user.id)
        analyticsService.log("weekly_report_sent", user.id)
    }
}
