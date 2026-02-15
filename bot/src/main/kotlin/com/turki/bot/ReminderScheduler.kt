package com.turki.bot

import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.ErrorNotifierService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.ReminderPreferenceService
import com.turki.bot.service.ReminderService
import com.turki.bot.service.UserService
import com.turki.core.domain.ReminderType
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.message.HTMLParseMode
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.delay
import org.koin.java.KoinJavaComponent.inject
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes

private val logger = LoggerFactory.getLogger("ReminderScheduler")

/** Hour (in user's local time) at which weekly reports are sent. */
private const val WEEKLY_REPORT_HOUR = 9

/** Tracks the last scheduler-sent message per user to delete it before sending a new one. */
private val lastSchedulerMessage = ConcurrentHashMap<Long, Long>() // telegramId -> messageId

private val reminderService: ReminderService by inject(ReminderService::class.java)
private val userService: UserService by inject(UserService::class.java)
private val reminderPreferenceService: ReminderPreferenceService by inject(ReminderPreferenceService::class.java)
private val progressService: ProgressService by inject(ProgressService::class.java)
private val analyticsService: AnalyticsService by inject(AnalyticsService::class.java)
private val errorNotifierScheduler: ErrorNotifierService by inject(ErrorNotifierService::class.java)

/**
 * Sends a scheduler message to the user, deleting the previous scheduler message if any.
 */
private suspend fun sendSchedulerMessage(
    bot: TelegramBot,
    telegramId: Long,
    text: String,
    replyMarkup: InlineKeyboardMarkup
) {
    val chatId = ChatId(RawChatId(telegramId))

    // Try to delete the previous scheduler message
    lastSchedulerMessage[telegramId]?.let { oldMsgId ->
        try {
            bot.deleteMessage(chatId, MessageId(oldMsgId))
        } catch (_: Exception) {
            // Message may already be deleted or too old — ignore
        }
    }

    val sent = bot.sendMessage(
        chatId = chatId,
        text = text,
        parseMode = HTMLParseMode,
        replyMarkup = replyMarkup
    )
    lastSchedulerMessage[telegramId] = sent.messageId.long
}

/**
 * Inline keyboard with action buttons for daily reminders.
 */
private fun reminderButtons(lessonId: Int) = InlineKeyboardMarkup(
    listOf(
        listOf(dataInlineButton(S.btnContinueLesson, "lesson_start:$lessonId")),
        listOf(
            dataInlineButton(S.btnPractice, "practice_start"),
            dataInlineButton(S.btnHomework, "homework:$lessonId")
        ),
        listOf(dataInlineButton(S.btnLearnWords, "learn_words")),
        listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")),
        listOf(dataInlineButton(S.btnUnsubscribeReminders, "unsub_reminders"))
    )
)

/**
 * Inline keyboard with action buttons for weekly reports.
 */
private fun weeklyReportButtons(lessonId: Int) = InlineKeyboardMarkup(
    listOf(
        listOf(dataInlineButton(S.btnContinueLesson, "lesson_start:$lessonId")),
        listOf(
            dataInlineButton(S.btnPractice, "practice_start"),
            dataInlineButton(S.btnHomework, "homework:$lessonId")
        ),
        listOf(dataInlineButton(S.btnLearnWords, "learn_words")),
        listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")),
        listOf(dataInlineButton(S.btnUnsubscribeWeekly, "unsub_weekly"))
    )
)

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
                    sendSchedulerMessage(
                        bot, user.telegramId, message, reminderButtons(user.currentLessonId)
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
            errorNotifierScheduler.notify("ReminderSchedulerError", e.message ?: "Unknown", e)
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
    @Suppress("UNUSED_PARAMETER") timeZone: TimeZone
) {
    val users = userService.getAllUsers()
    val now = clock.now()

    var matched = 0
    var sent = 0
    users.forEach { user ->
        try {
            val userTz = try { TimeZone.of(user.timezone) } catch (_: Exception) { TimeZone.of("Europe/Moscow") }
            val local = now.toLocalDateTime(userTz)
            val currentDay = local.date.dayOfWeek
            val currentHour = local.hour

            val pref = reminderPreferenceService.getOrDefault(user.id)
            if (!pref.isEnabled) return@forEach

            val activeDays = pref.daysOfWeek.split(",").mapNotNull { dayCodeToEnum[it.trim()] }
            if (currentDay !in activeDays) return@forEach

            // Match by hour instead of exact minute to avoid skips
            val prefHour = pref.timeLocal.substringBefore(":").toIntOrNull() ?: return@forEach
            if (currentHour != prefHour) return@forEach

            val lastFired = pref.lastFiredAt?.toLocalDateTime(userTz)?.date
            if (lastFired == local.date) return@forEach

            matched++
            sendSchedulerMessage(
                bot, user.telegramId, S.reminderLesson, reminderButtons(user.currentLessonId)
            )
            reminderPreferenceService.markFired(user.id)
            analyticsService.log("reminder_fired", user.id)
            sent++
        } catch (e: Exception) {
            logger.warn("Failed to send scheduled reminder to userId=${user.id}: ${e.message}")
        }
    }

    if (matched > 0) {
        logger.info("Scheduled reminders: matched={}, sent={}", matched, sent)
    }
}

private suspend fun sendWeeklyReports(
    bot: TelegramBot,
    clock: Clock,
    @Suppress("UNUSED_PARAMETER") timeZone: TimeZone
) {
    val users = userService.getAllUsers()
    val now = clock.now()

    users.forEach { user ->
        try {
            val userTz = try { TimeZone.of(user.timezone) } catch (_: Exception) { TimeZone.of("Europe/Moscow") }
            val local = now.toLocalDateTime(userTz)

            if (local.date.dayOfWeek != DayOfWeek.SUNDAY) return@forEach
            if (local.hour < WEEKLY_REPORT_HOUR) return@forEach

            val stats = progressService.getWeeklyStats(user.id)
            val lastReportDate = stats.lastWeeklyReportAt?.toLocalDateTime(userTz)?.date
            if (lastReportDate == local.date) return@forEach

            // User opted out of weekly reports
            if (!stats.weeklyReportsEnabled) {
                progressService.resetWeekly(user.id)
                return@forEach
            }

            // Don't send a report if the user had zero activity this week
            val totalActivity = stats.weeklyLessons + stats.weeklyPractice +
                stats.weeklyReview + stats.weeklyHomework
            if (totalActivity == 0) {
                // Still reset so lastWeeklyReportAt is set (avoids re-checking every minute)
                progressService.resetWeekly(user.id)
                return@forEach
            }

            val report = S.weeklyReport(
                lessons = stats.weeklyLessons,
                practice = stats.weeklyPractice,
                review = stats.weeklyReview,
                homework = stats.weeklyHomework
            )
            sendSchedulerMessage(
                bot, user.telegramId, report, weeklyReportButtons(user.currentLessonId)
            )
            progressService.resetWeekly(user.id)
            analyticsService.log("weekly_report_sent", user.id)
        } catch (e: Exception) {
            logger.warn("Failed to send weekly report to userId=${user.id}: ${e.message}")
        }
    }
}
