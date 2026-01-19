package com.turki.core.database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object UsersTable : LongIdTable("users") {
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

object LessonsTable : IntIdTable("lessons") {
    val orderIndex = integer("order_index")
    val targetLanguage = varchar("target_language", 10)
    val title = varchar("title", 255)
    val description = text("description")
    val content = text("content")
}

object VocabularyTable : IntIdTable("vocabulary") {
    val lessonId = reference("lesson_id", LessonsTable)
    val word = varchar("word", 255)
    val translation = varchar("translation", 255)
    val pronunciation = varchar("pronunciation", 255).nullable()
    val example = text("example").nullable()
}

object HomeworksTable : IntIdTable("homeworks") {
    val lessonId = reference("lesson_id", LessonsTable)
}

object HomeworkQuestionsTable : IntIdTable("homework_questions") {
    val homeworkId = reference("homework_id", HomeworksTable)
    val questionType = varchar("question_type", 50)
    val questionText = text("question_text")
    val options = text("options").default("[]")
    val correctAnswer = text("correct_answer")
}

object HomeworkSubmissionsTable : LongIdTable("homework_submissions") {
    val userId = reference("user_id", UsersTable)
    val homeworkId = reference("homework_id", HomeworksTable)
    val answers = text("answers")
    val score = integer("score")
    val maxScore = integer("max_score")
    val submittedAt = timestamp("submitted_at")
}

object RemindersTable : LongIdTable("reminders") {
    val userId = reference("user_id", UsersTable)
    val type = varchar("type", 50)
    val scheduledAt = timestamp("scheduled_at")
    val sent = bool("sent").default(false)
    val sentAt = timestamp("sent_at").nullable()
}
