package com.turki.core.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * A snapshot of a metric value for a specific date.
 */
@Serializable
data class MetricSnapshot(
    val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val metricName: String,
    val value: Long,
    val metadata: String? = null,
    val createdAt: Instant
)

/**
 * Error log entry for tracking exceptions and issues.
 */
@Serializable
data class ErrorLog(
    val id: Long = 0,
    val errorType: String,
    val message: String,
    val stackTrace: String? = null,
    val userId: Long? = null,
    val context: String? = null,
    val createdAt: Instant
)

/**
 * Daily metrics report data class.
 */
@Serializable
data class DailyReport(
    val date: String,
    val dau: Long,
    val wau: Long,
    val mau: Long,
    val newUsersToday: Long,
    val newUsersWeek: Long,
    val totalUsers: Long,
    val lessonsCompletedToday: Long,
    val lessonsCompletedWeek: Long,
    val homeworkCompletedToday: Long,
    val homeworkCompletedWeek: Long,
    val wordsAddedToday: Long,
    val wordsAddedWeek: Long,
    val reviewSessionsToday: Long,
    val practiceSessionsToday: Long,
    val supportMessagesToday: Long,
    val errorsToday: Long,
    val topCommands: Map<String, Long>,
    val retentionDay1: Double, // % of users who returned next day
    val avgSessionsPerUser: Double
)

/**
 * Standard metric names for consistency.
 */
object MetricNames {
    const val DAU = "dau"
    const val WAU = "wau"
    const val MAU = "mau"
    const val NEW_USERS = "new_users"
    const val TOTAL_USERS = "total_users"
    const val LESSONS_COMPLETED = "lessons_completed"
    const val HOMEWORK_COMPLETED = "homework_completed"
    const val WORDS_ADDED = "words_added"
    const val REVIEW_SESSIONS = "review_sessions"
    const val PRACTICE_SESSIONS = "practice_sessions"
    const val SUPPORT_MESSAGES = "support_messages"
    const val ERRORS = "errors"
    const val RETENTION_D1 = "retention_d1"
}

/**
 * Standard event names for analytics tracking.
 */
object EventNames {
    // User lifecycle
    const val USER_REGISTERED = "user_registered"
    const val USER_RETURNED = "user_returned"
    const val SESSION_START = "session_start"
    const val DATA_EXPORTED = "data_exported"

    // Learning
    const val LESSON_STARTED = "lesson_started"
    const val LESSON_COMPLETED = "lesson_completed"
    const val LESSONS_LIST_OPENED = "lessons_list_opened"
    const val HOMEWORK_STARTED = "homework_started"
    const val HOMEWORK_COMPLETED = "homework_completed"
    const val HOMEWORK_QUESTION_ANSWERED = "homework_question_answered"

    // Practice & Review
    const val PRACTICE_STARTED = "practice_started"
    const val PRACTICE_COMPLETED = "practice_completed"
    const val EXERCISE_ANSWERED = "exercise_answered"
    const val REVIEW_STARTED = "review_started"
    const val REVIEW_COMPLETED = "review_completed"
    const val REVIEW_QUESTION_ANSWERED = "review_question_answered"

    // Dictionary
    const val WORD_ADDED_TO_DICTIONARY = "word_added_to_dictionary"
    const val WORD_ADDED = "word_added"
    const val WORD_REMOVED = "word_removed"
    const val CUSTOM_WORD_ADDED = "custom_word_added"
    const val DICTIONARY_SEARCH = "dictionary_search"

    // Commands
    const val COMMAND_USED = "command_used"

    // Support
    const val SUPPORT_MESSAGE_SENT = "support_message_sent"

    // Errors
    const val ERROR_OCCURRED = "error_occurred"

    // Engagement
    const val REMINDER_SENT = "reminder_sent"
    const val REMINDER_CLICKED = "reminder_clicked"
    const val REMINDER_CONFIGURED = "reminder_configured"

    // Progress
    const val PROGRESS_VIEWED = "progress_viewed"

    // Subscriptions
    const val SUBSCRIPTION_STARTED = "subscription_started"
    const val SUBSCRIPTION_CANCELLED = "subscription_cancelled"
    const val PAYMENT_COMPLETED = "payment_completed"
}
