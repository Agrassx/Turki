package com.turki.core.database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

private const val CONTENT_SCHEMA = "content"
private const val APP_SCHEMA = "app"
private const val LOGS_SCHEMA = "logs"

object UsersTable : LongIdTable("$APP_SCHEMA.users") {
    val telegramId = long("telegram_id").uniqueIndex()
    val username = varchar("username", 255).nullable()
    val firstName = varchar("first_name", 255)
    val lastName = varchar("last_name", 255).nullable()
    val language = varchar("language", 10).default("ru")
    val subscriptionActive = bool("subscription_active").default(false)
    val subscriptionExpiresAt = timestamp("subscription_expires_at").nullable()
    val currentLessonId = integer("current_lesson_id").default(1)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

object LessonsTable : IntIdTable("$CONTENT_SCHEMA.lessons") {
    val orderIndex = integer("order_index")
    val targetLanguage = varchar("target_language", 10)
    val level = varchar("level", 10).default("A1")
    val contentVersion = varchar("content_version", 32).default("v1")
    val isActive = bool("is_active").default(true)
    val title = varchar("title", 255)
    val description = text("description")
    val content = text("content")
}

object VocabularyTable : IntIdTable("$CONTENT_SCHEMA.vocabulary") {
    val lessonId = reference("lesson_id", LessonsTable)
    val word = varchar("word", 255)
    val translation = varchar("translation", 255)
    val pronunciation = varchar("pronunciation", 255).nullable()
    val example = text("example").nullable()
}

object HomeworksTable : IntIdTable("$CONTENT_SCHEMA.homeworks") {
    val lessonId = reference("lesson_id", LessonsTable)
}

object HomeworkQuestionsTable : IntIdTable("$CONTENT_SCHEMA.homework_questions") {
    val homeworkId = reference("homework_id", HomeworksTable)
    val questionType = varchar("question_type", 50)
    val questionText = text("question_text")
    val options = text("options").default("[]")
    val correctAnswer = text("correct_answer")
}

object HomeworkSubmissionsTable : LongIdTable("$APP_SCHEMA.homework_submissions") {
    val userId = reference("user_id", UsersTable)
    val homeworkId = reference("homework_id", HomeworksTable)
    val answers = text("answers")
    val score = integer("score")
    val maxScore = integer("max_score")
    val submittedAt = timestamp("submitted_at")
}

object RemindersTable : LongIdTable("$APP_SCHEMA.reminders") {
    val userId = reference("user_id", UsersTable)
    val type = varchar("type", 50)
    val scheduledAt = timestamp("scheduled_at")
    val sent = bool("sent").default(false)
    val sentAt = timestamp("sent_at").nullable()
}

object UserStatesTable : LongIdTable("$APP_SCHEMA.user_states") {
    val userId = reference("user_id", UsersTable).uniqueIndex()
    val state = varchar("state", 50)
    val payload = text("payload").default("{}")
    val updatedAt = timestamp("updated_at")
}

object UserProgressTable : LongIdTable("$APP_SCHEMA.user_progress") {
    val userId = reference("user_id", UsersTable).index()
    val lessonId = reference("lesson_id", LessonsTable).index()
    val status = varchar("status", 32)
    val lastExerciseId = integer("last_exercise_id").nullable()
    val contentVersion = varchar("content_version", 32).default("v1")
    val updatedAt = timestamp("updated_at")
    init {
        uniqueIndex(userId, lessonId)
    }
}

object UserDictionaryTable : LongIdTable("$APP_SCHEMA.user_dictionary") {
    val userId = reference("user_id", UsersTable).index()
    val vocabularyId = reference("vocabulary_id", VocabularyTable).index()
    val isFavorite = bool("is_favorite").default(true)
    val tags = text("tags").default("[]")
    val addedAt = timestamp("added_at")
    init {
        uniqueIndex(userId, vocabularyId)
    }
}

object UserCustomDictionaryTable : LongIdTable("$APP_SCHEMA.user_custom_dictionary") {
    val userId = reference("user_id", UsersTable).index()
    val word = varchar("word", 255)
    val translation = varchar("translation", 255)
    val pronunciation = varchar("pronunciation", 255).nullable()
    val example = text("example").nullable()
    val addedAt = timestamp("added_at")
}

object ReviewCardsTable : LongIdTable("$APP_SCHEMA.review_cards") {
    val userId = reference("user_id", UsersTable).index()
    val vocabularyId = reference("vocabulary_id", VocabularyTable).index()
    val stage = integer("stage").default(0)
    val nextReviewAt = timestamp("next_review_at")
    val lastResult = bool("last_result").nullable()
    init {
        uniqueIndex(userId, vocabularyId)
    }
}

object ReminderPreferencesTable : LongIdTable("$APP_SCHEMA.reminder_preferences") {
    val userId = reference("user_id", UsersTable).uniqueIndex()
    val daysOfWeek = varchar("days_of_week", 32)
    val timeLocal = varchar("time_local", 8)
    val isEnabled = bool("is_enabled").default(true)
    val lastFiredAt = timestamp("last_fired_at").nullable()
}

object AnalyticsEventsTable : LongIdTable("$LOGS_SCHEMA.analytics_events") {
    val eventName = varchar("event_name", 64)
    val userId = reference("user_id", UsersTable).index()
    val sessionId = varchar("session_id", 64).nullable()
    val props = text("props").default("{}")
    val createdAt = timestamp("created_at")
}

object UserStatsTable : LongIdTable("$APP_SCHEMA.user_stats") {
    val userId = reference("user_id", UsersTable).uniqueIndex()
    val currentStreak = integer("current_streak").default(0)
    val lastActiveAt = timestamp("last_active_at").nullable()
    val weeklyLessons = integer("weekly_lessons").default(0)
    val weeklyPractice = integer("weekly_practice").default(0)
    val weeklyReview = integer("weekly_review").default(0)
    val weeklyHomework = integer("weekly_homework").default(0)
    val lastWeeklyReportAt = timestamp("last_weekly_report_at").nullable()
}

// Subscription system tables
private const val BILLING_SCHEMA = "billing"

object SubscriptionPlansTable : IntIdTable("$BILLING_SCHEMA.subscription_plans") {
    val code = varchar("code", 32).uniqueIndex()
    val name = varchar("name", 255)
    val description = text("description")
    val priceMonthly = long("price_monthly").default(0)
    val priceYearly = long("price_yearly").default(0)
    val maxLessons = integer("max_lessons").nullable()
    val maxReviewsPerDay = integer("max_reviews_per_day").nullable()
    val maxPracticePerDay = integer("max_practice_per_day").nullable()
    val hasAds = bool("has_ads").default(true)
    val hasOfflineAccess = bool("has_offline_access").default(false)
    val hasPrioritySupport = bool("has_priority_support").default(false)
    val features = text("features").nullable() // JSON array
    val isActive = bool("is_active").default(true)
    val sortOrder = integer("sort_order").default(0)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

object UserSubscriptionsTable : LongIdTable("$BILLING_SCHEMA.user_subscriptions") {
    val userId = reference("user_id", UsersTable).index()
    val planId = reference("plan_id", SubscriptionPlansTable)
    val status = varchar("status", 32)
    val startedAt = timestamp("started_at")
    val expiresAt = timestamp("expires_at").nullable()
    val cancelledAt = timestamp("cancelled_at").nullable()
    val autoRenew = bool("auto_renew").default(true)
    val paymentProvider = varchar("payment_provider", 32).nullable()
    val paymentProviderId = varchar("payment_provider_id", 255).nullable()
    val metadata = text("metadata").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

object PaymentTransactionsTable : LongIdTable("$BILLING_SCHEMA.payment_transactions") {
    val userId = reference("user_id", UsersTable).index()
    val subscriptionId = reference("subscription_id", UserSubscriptionsTable).nullable()
    val amount = long("amount")
    val currency = varchar("currency", 3).default("RUB")
    val status = varchar("status", 32)
    val paymentProvider = varchar("payment_provider", 32)
    val paymentProviderId = varchar("payment_provider_id", 255).nullable()
    val description = text("description").nullable()
    val metadata = text("metadata").nullable()
    val createdAt = timestamp("created_at")
    val completedAt = timestamp("completed_at").nullable()
}

// Metrics tables
object MetricsSnapshotsTable : LongIdTable("$LOGS_SCHEMA.metrics_snapshots") {
    val date = varchar("date", 10).index() // YYYY-MM-DD
    val metricName = varchar("metric_name", 64).index()
    val value = long("value")
    val metadata = text("metadata").nullable() // JSON for additional context
    val createdAt = timestamp("created_at")
}

object ErrorLogsTable : LongIdTable("$LOGS_SCHEMA.error_logs") {
    val errorType = varchar("error_type", 128)
    val message = text("message")
    val stackTrace = text("stack_trace").nullable()
    val userId = long("user_id").nullable().index()
    val context = text("context").nullable() // JSON with additional info
    val createdAt = timestamp("created_at").index()
}
