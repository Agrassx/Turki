package com.turki.bot.service

import com.turki.bot.EnvLoader
import com.turki.core.domain.DailyReport
import com.turki.core.domain.ErrorLog
import com.turki.core.domain.EventNames
import com.turki.core.domain.MetricNames
import com.turki.core.domain.MetricSnapshot
import com.turki.core.repository.AnalyticsRepository
import com.turki.core.repository.MetricsRepository
import com.turki.core.repository.UserRepository
import java.util.Locale
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.message.HTMLParseMode
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Service for collecting metrics and generating reports.
 */
@Suppress("TooManyFunctions")
class MetricsService(
    private val metricsRepository: MetricsRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val userRepository: UserRepository,
    private val clock: Clock = Clock.System
) {
    private val logger = LoggerFactory.getLogger("MetricsService")
    private val statsChatId: Long? = EnvLoader.get("STATS_CHAT_ID")?.toLongOrNull()
    private val json = Json { prettyPrint = true }

    /**
     * Log an error to the database.
     */
    suspend fun logError(
        errorType: String,
        message: String,
        stackTrace: String? = null,
        userId: Long? = null,
        context: Map<String, String>? = null
    ) {
        try {
            metricsRepository.logError(
                ErrorLog(
                    errorType = errorType,
                    message = message,
                    stackTrace = stackTrace,
                    userId = userId,
                    context = context?.let { json.encodeToString(it) },
                    createdAt = clock.now()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to log error: ${e.message}")
        }
    }

    /**
     * Generate daily report with all metrics.
     */
    suspend fun generateDailyReport(): DailyReport {
        val now = clock.now()
        val tz = TimeZone.UTC
        val today = now.toLocalDateTime(tz).date.toString()

        val oneDayAgo = now.minus(1, DateTimeUnit.DAY, tz)
        val oneWeekAgo = now.minus(7, DateTimeUnit.DAY, tz)
        val oneMonthAgo = now.minus(30, DateTimeUnit.DAY, tz)

        // Active users
        val dau = metricsRepository.countActiveUsersSince(oneDayAgo)
        val wau = metricsRepository.countActiveUsersSince(oneWeekAgo)
        val mau = metricsRepository.countActiveUsersSince(oneMonthAgo)

        // New users
        val newUsersToday = metricsRepository.countEventsSince(EventNames.USER_REGISTERED, oneDayAgo)
        val newUsersWeek = metricsRepository.countEventsSince(EventNames.USER_REGISTERED, oneWeekAgo)

        // Total users
        val totalUsers = userRepository.count()

        // Lessons
        val lessonsToday = metricsRepository.countEventsSince(EventNames.LESSON_COMPLETED, oneDayAgo)
        val lessonsWeek = metricsRepository.countEventsSince(EventNames.LESSON_COMPLETED, oneWeekAgo)

        // Homework
        val homeworkToday = metricsRepository.countEventsSince(EventNames.HOMEWORK_COMPLETED, oneDayAgo)
        val homeworkWeek = metricsRepository.countEventsSince(EventNames.HOMEWORK_COMPLETED, oneWeekAgo)

        // Dictionary
        val wordsToday = metricsRepository.countEventsSince(EventNames.WORD_ADDED, oneDayAgo) +
            metricsRepository.countEventsSince(EventNames.CUSTOM_WORD_ADDED, oneDayAgo)
        val wordsWeek = metricsRepository.countEventsSince(EventNames.WORD_ADDED, oneWeekAgo) +
            metricsRepository.countEventsSince(EventNames.CUSTOM_WORD_ADDED, oneWeekAgo)

        // Sessions
        val reviewToday = metricsRepository.countEventsSince(EventNames.REVIEW_STARTED, oneDayAgo)
        val practiceToday = metricsRepository.countEventsSince(EventNames.PRACTICE_STARTED, oneDayAgo)

        // Support
        val supportToday = metricsRepository.countEventsSince(EventNames.SUPPORT_MESSAGE_SENT, oneDayAgo)

        // Errors
        val errorsToday = metricsRepository.countErrorsSince(oneDayAgo)

        // Top commands (simplified)
        val topCommands = metricsRepository.getTopEventsSince(oneDayAgo, 5)

        // Retention D1 (users who registered yesterday and came back today)
        val retentionD1 = calculateRetentionD1()

        // Avg sessions per user
        val avgSessions = if (dau > 0) {
            metricsRepository.countEventsSince(EventNames.SESSION_START, oneDayAgo).toDouble() / dau
        } else 0.0

        return DailyReport(
            date = today,
            dau = dau,
            wau = wau,
            mau = mau,
            newUsersToday = newUsersToday,
            newUsersWeek = newUsersWeek,
            totalUsers = totalUsers,
            lessonsCompletedToday = lessonsToday,
            lessonsCompletedWeek = lessonsWeek,
            homeworkCompletedToday = homeworkToday,
            homeworkCompletedWeek = homeworkWeek,
            wordsAddedToday = wordsToday,
            wordsAddedWeek = wordsWeek,
            reviewSessionsToday = reviewToday,
            practiceSessionsToday = practiceToday,
            supportMessagesToday = supportToday,
            errorsToday = errorsToday,
            topCommands = topCommands,
            retentionDay1 = retentionD1,
            avgSessionsPerUser = avgSessions
        )
    }

    /**
     * Save daily metrics snapshot.
     */
    suspend fun saveDailySnapshot(report: DailyReport) {
        val now = clock.now()
        val date = report.date

        fun snapshot(name: String, value: Long) =
            MetricSnapshot(date = date, metricName = name, value = value, createdAt = now)

        val snapshots = listOf(
            snapshot(MetricNames.DAU, report.dau),
            snapshot(MetricNames.WAU, report.wau),
            snapshot(MetricNames.MAU, report.mau),
            snapshot(MetricNames.NEW_USERS, report.newUsersToday),
            snapshot(MetricNames.TOTAL_USERS, report.totalUsers),
            snapshot(MetricNames.LESSONS_COMPLETED, report.lessonsCompletedToday),
            snapshot(MetricNames.HOMEWORK_COMPLETED, report.homeworkCompletedToday),
            snapshot(MetricNames.WORDS_ADDED, report.wordsAddedToday),
            snapshot(MetricNames.REVIEW_SESSIONS, report.reviewSessionsToday),
            snapshot(MetricNames.PRACTICE_SESSIONS, report.practiceSessionsToday),
            snapshot(MetricNames.ERRORS, report.errorsToday)
        )

        snapshots.forEach { metricsRepository.saveSnapshot(it) }
    }

    /**
     * Send daily report to stats chat.
     */
    suspend fun sendDailyReport(bot: TelegramBot) {
        if (statsChatId == null) {
            logger.warn("STATS_CHAT_ID not configured, skipping daily report")
            return
        }

        try {
            val report = generateDailyReport()
            saveDailySnapshot(report)

            val message = formatReportMessage(report)
            bot.sendMessage(
                chatId = ChatId(RawChatId(statsChatId)),
                text = message,
                parseMode = HTMLParseMode
            )
            logger.info("Daily report sent successfully")
        } catch (e: Exception) {
            logger.error("Failed to send daily report: ${e.message}")
            logError("DailyReportError", e.message ?: "Unknown error", e.stackTraceToString())
        }
    }

    /**
     * Send error alert to stats chat.
     */
    suspend fun sendErrorAlert(bot: TelegramBot, errorType: String, message: String, userId: Long? = null) {
        if (statsChatId == null) return

        try {
            val alertMessage = """
🚨 <b>Ошибка!</b>

<b>Тип:</b> <code>$errorType</code>
<b>Сообщение:</b> $message
${userId?.let { "<b>User ID:</b> <code>$it</code>" } ?: ""}
<b>Время:</b> ${clock.now()}
            """.trimIndent()

            bot.sendMessage(
                chatId = ChatId(RawChatId(statsChatId)),
                text = alertMessage,
                parseMode = HTMLParseMode
            )
        } catch (e: Exception) {
            logger.error("Failed to send error alert: ${e.message}")
        }
    }

    private fun formatReportMessage(report: DailyReport): String {
        return """
📊 <b>Ежедневный отчёт</b>
📅 ${report.date}

👥 <b>Активные пользователи:</b>
• DAU: <b>${report.dau}</b>
• WAU: <b>${report.wau}</b>
• MAU: <b>${report.mau}</b>
• Всего: <b>${report.totalUsers}</b>

🆕 <b>Новые пользователи:</b>
• Сегодня: <b>${report.newUsersToday}</b>
• За неделю: <b>${report.newUsersWeek}</b>

📚 <b>Обучение:</b>
• Уроков завершено: <b>${report.lessonsCompletedToday}</b> (неделя: ${report.lessonsCompletedWeek})
• Домашек сдано: <b>${report.homeworkCompletedToday}</b> (неделя: ${report.homeworkCompletedWeek})

📖 <b>Словарь:</b>
• Слов добавлено: <b>${report.wordsAddedToday}</b> (неделя: ${report.wordsAddedWeek})

🔁 <b>Активность:</b>
• Повторений: <b>${report.reviewSessionsToday}</b>
• Практик: <b>${report.practiceSessionsToday}</b>

📬 <b>Поддержка:</b>
• Обращений: <b>${report.supportMessagesToday}</b>

${if (report.errorsToday > 0) "⚠️ <b>Ошибок за день:</b> ${report.errorsToday}" else "✅ Ошибок нет"}

📈 <b>Метрики:</b>
• Retention D1: <b>${String.format(Locale.US, "%.1f", report.retentionDay1)}%</b>
• Сессий на юзера: <b>${String.format(Locale.US, "%.1f", report.avgSessionsPerUser)}</b>
        """.trimIndent()
    }

    private suspend fun calculateRetentionD1(): Double {
        // Simplified: check how many users who registered 1-2 days ago returned today
        val now = clock.now()
        val tz = TimeZone.UTC
        val oneDayAgo = now.minus(1, DateTimeUnit.DAY, tz)
        val twoDaysAgo = now.minus(2, DateTimeUnit.DAY, tz)

        // Users registered 1-2 days ago
        val registeredYesterday = analyticsRepository.countEventsBetween(
            EventNames.USER_REGISTERED, twoDaysAgo, oneDayAgo
        )

        if (registeredYesterday == 0L) return 0.0

        // Of those, how many had activity today
        val returnedToday = analyticsRepository.countDistinctUsersWithEventsBetween(
            oneDayAgo, now
        )

        return (returnedToday.toDouble() / registeredYesterday) * 100
    }

    fun isConfigured(): Boolean = statsChatId != null
}
