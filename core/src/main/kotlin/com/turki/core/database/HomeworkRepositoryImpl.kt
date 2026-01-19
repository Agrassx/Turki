package com.turki.core.database

import com.turki.core.domain.Homework
import com.turki.core.domain.HomeworkQuestion
import com.turki.core.domain.HomeworkSubmission
import com.turki.core.domain.QuestionType
import com.turki.core.repository.HomeworkRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

class HomeworkRepositoryImpl : HomeworkRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun findById(id: Int): Homework? = DatabaseFactory.dbQuery {
        HomeworksTable.selectAll().where { HomeworksTable.id eq id }
            .map(::toHomework)
            .singleOrNull()
            ?.let { homework ->
                val questions = findQuestions(homework.id)
                homework.copy(questions = questions)
            }
    }

    override suspend fun findByLessonId(lessonId: Int): Homework? = DatabaseFactory.dbQuery {
        HomeworksTable.selectAll().where { HomeworksTable.lessonId eq lessonId }
            .map(::toHomework)
            .singleOrNull()
            ?.let { homework ->
                val questions = findQuestions(homework.id)
                homework.copy(questions = questions)
            }
    }

    override suspend fun findQuestions(homeworkId: Int): List<HomeworkQuestion> = DatabaseFactory.dbQuery {
        HomeworkQuestionsTable.selectAll().where { HomeworkQuestionsTable.homeworkId eq homeworkId }
            .map(::toQuestion)
    }

    override suspend fun create(homework: Homework): Homework = DatabaseFactory.dbQuery {
        val id = HomeworksTable.insert {
            it[lessonId] = homework.lessonId
        }[HomeworksTable.id].value

        homework.questions.forEach { question ->
            HomeworkQuestionsTable.insert {
                it[homeworkId] = id
                it[questionType] = question.questionType.name
                it[questionText] = question.questionText
                it[options] = json.encodeToString(question.options)
                it[correctAnswer] = question.correctAnswer
            }
        }

        homework.copy(id = id)
    }

    override suspend fun createSubmission(submission: HomeworkSubmission): HomeworkSubmission =
        DatabaseFactory.dbQuery {
            val id = HomeworkSubmissionsTable.insert {
                it[userId] = submission.userId
                it[homeworkId] = submission.homeworkId
                it[answers] = json.encodeToString(submission.answers)
                it[score] = submission.score
                it[maxScore] = submission.maxScore
                it[submittedAt] = submission.submittedAt
            }[HomeworkSubmissionsTable.id].value

            submission.copy(id = id)
        }

    override suspend fun findSubmissionsByUser(userId: Long): List<HomeworkSubmission> = DatabaseFactory.dbQuery {
        HomeworkSubmissionsTable.selectAll().where { HomeworkSubmissionsTable.userId eq userId }
            .map(::toSubmission)
    }

    override suspend fun hasUserCompletedHomework(userId: Long, homeworkId: Int): Boolean =
        DatabaseFactory.dbQuery {
            HomeworkSubmissionsTable.selectAll().where {
                (HomeworkSubmissionsTable.userId eq userId) and
                    (HomeworkSubmissionsTable.homeworkId eq homeworkId) and
                    (HomeworkSubmissionsTable.score eq HomeworkSubmissionsTable.maxScore)
            }.count() > 0
        }

    private fun toHomework(row: ResultRow): Homework = Homework(
        id = row[HomeworksTable.id].value,
        lessonId = row[HomeworksTable.lessonId].value,
        questions = emptyList()
    )

    private fun toQuestion(row: ResultRow): HomeworkQuestion {
        val optionsStr = row[HomeworkQuestionsTable.options]
        val options = parseOptions(optionsStr)

        return HomeworkQuestion(
            id = row[HomeworkQuestionsTable.id].value,
            homeworkId = row[HomeworkQuestionsTable.homeworkId].value,
            questionType = QuestionType.valueOf(row[HomeworkQuestionsTable.questionType]),
            questionText = row[HomeworkQuestionsTable.questionText],
            options = options,
            correctAnswer = row[HomeworkQuestionsTable.correctAnswer]
        )
    }

    private fun parseOptions(str: String): List<String> {
        if (str.isBlank()) return emptyList()

        return if (str.startsWith("[")) {
            try {
                json.decodeFromString(str)
            } catch (_: Exception) {
                str.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            }
        } else {
            str.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }

    private fun toSubmission(row: ResultRow): HomeworkSubmission = HomeworkSubmission(
        id = row[HomeworkSubmissionsTable.id].value,
        userId = row[HomeworkSubmissionsTable.userId].value,
        homeworkId = row[HomeworkSubmissionsTable.homeworkId].value,
        answers = json.decodeFromString(row[HomeworkSubmissionsTable.answers]),
        score = row[HomeworkSubmissionsTable.score],
        maxScore = row[HomeworkSubmissionsTable.maxScore],
        submittedAt = row[HomeworkSubmissionsTable.submittedAt]
    )
}
