package com.turki.bot

import com.turki.bot.service.MetricsService
import dev.inmo.tgbotapi.bot.TelegramBot
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.java.KoinJavaComponent.inject
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("DailyReportScheduler")
private val metricsService: MetricsService by inject(MetricsService::class.java)

private const val POST_REPORT_DELAY_MS = 60_000L
private const val ERROR_RETRY_DELAY_MS = 300_000L

/**
 * Scheduler for sending daily reports.
 * Reports are sent at 09:00 Moscow time (UTC+3).
 */
suspend fun startDailyReportScheduler(
    bot: TelegramBot,
    clock: Clock = Clock.System,
    reportTimeZone: TimeZone = TimeZone.of("Europe/Moscow")
) {
    if (!metricsService.isConfigured()) {
        logger.info("STATS_CHAT_ID not configured, daily reports disabled")
        return
    }

    logger.info("Daily report scheduler started")

    while (true) {
        try {
            val now = clock.now()
            val localNow = now.toLocalDateTime(reportTimeZone)

            // Calculate delay until next 9:00 MSK
            val targetHour = 9
            val hoursUntilTarget = if (localNow.hour < targetHour) {
                targetHour - localNow.hour
            } else {
                24 - localNow.hour + targetHour
            }

            val minutesToWait = hoursUntilTarget * 60 - localNow.minute
            val msToWait = minutesToWait * 60 * 1000L

            logger.info("Next daily report in ${minutesToWait / 60}h ${minutesToWait % 60}m")

            delay(msToWait)

            // Send report
            logger.info("Sending daily report...")
            metricsService.sendDailyReport(bot)

            // Wait a bit to avoid sending multiple reports
            delay(POST_REPORT_DELAY_MS)
        } catch (e: Exception) {
            logger.error("Error in daily report scheduler: ${e.message}")
            metricsService.logError(
                errorType = "SchedulerError",
                message = "Daily report scheduler failed: ${e.message}",
                stackTrace = e.stackTraceToString()
            )
            delay(ERROR_RETRY_DELAY_MS) // Wait 5 minutes before retry
        }
    }
}
