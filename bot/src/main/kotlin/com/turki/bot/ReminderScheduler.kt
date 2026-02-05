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

suspend fun startReminderScheduler(
    bot: TelegramBot,
    clock: Clock = Clock.System,
    timeZone: TimeZone = TimeZone.of("Europe/Moscow")
) {
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

            sendScheduledReminders(bot, clock, timeZone)
            sendWeeklyReports(bot, clock, timeZone)
        } catch (e: Exception) {
            logger.error("Error in reminder scheduler: ${e.message}")
        }

        delay(1.minutes)
    }
}

/**
 * Maps abbreviated day codes (MON, TUE, ...) stored in DB to [DayOfWeek].
 */
private val dayCodeToEnum = mapOf(
    "MON" to DayOfWeek.MONDAY,
    "TUE" to DayOfWeek.TUESDAY,
    "WED" to DayOfWeek.WEDNESDAY,
    "THU" to DayOfWeek.THURSDAY,
    "FRI" to DayOfWeek.FRIDAY,
    "SAT" to DayOfWeek.SATURDAY,
    "SUN" to DayOfWeek.SUNDAY
)

private suspend fun sendScheduledReminders(
    bot: TelegramBot,
    clock: Clock,
    timeZone: TimeZone
) {
    val users = userService.getAllUsers()
    val now = clock.now()
    val local = now.toLocalDateTime(timeZone)
    val currentDay = local.date.dayOfWeek
    val currentTime = "%02d:%02d".format(local.hour, local.minute)

    users.forEach { user ->
        try {
            val pref = reminderPreferenceService.getOrDefault(user.id)
            if (!pref.isEnabled) return@forEach

            val activeDays = pref.daysOfWeek.split(",").mapNotNull { dayCodeToEnum[it.trim()] }
            if (currentDay !in activeDays) return@forEach
            if (pref.timeLocal != currentTime) return@forEach

            val lastFired = pref.lastFiredAt?.toLocalDateTime(timeZone)?.date
            if (lastFired == local.date) return@forEach

            bot.sendMessage(
                chatId = ChatId(RawChatId(user.telegramId)),
                text = S.reminderLesson,
                parseMode = HTMLParseMode
            )
            reminderPreferenceService.markFired(user.id)
            analyticsService.log("reminder_fired", user.id)
        } catch (e: Exception) {
            logger.warn("Failed to send scheduled reminder to userId=${user.id}: ${e.message}")
        }
    }
}

private suspend fun sendWeeklyReports(
    bot: TelegramBot,
    clock: Clock,
    timeZone: TimeZone
) {
    val users = userService.getAllUsers()
    val now = clock.now()
    val local = now.toLocalDateTime(timeZone)
    if (local.date.dayOfWeek != DayOfWeek.SUNDAY) {
        return
    }

    users.forEach { user ->
        try {
            val stats = progressService.getWeeklyStats(user.id)
            val lastReportDate = stats.lastWeeklyReportAt?.toLocalDateTime(timeZone)?.date
            if (lastReportDate == local.date) return@forEach

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
        } catch (e: Exception) {
            logger.warn("Failed to send weekly report to userId=${user.id}: ${e.message}")
        }
    }
}
