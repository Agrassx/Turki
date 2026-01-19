package com.turki.bot.service

import com.turki.core.domain.Homework
import com.turki.core.domain.HomeworkQuestion
import com.turki.core.domain.HomeworkSubmission
import com.turki.core.repository.HomeworkRepository
import com.turki.core.repository.UserRepository
import kotlin.time.Clock

class HomeworkService(
    private val homeworkRepository: HomeworkRepository,
    private val userRepository: UserRepository
) {

    suspend fun getHomeworkForLesson(lessonId: Int): Homework? =
        homeworkRepository.findByLessonId(lessonId)

    suspend fun getHomeworkQuestions(homeworkId: Int): List<HomeworkQuestion> =
        homeworkRepository.findQuestions(homeworkId)

    suspend fun submitHomework(
        userId: Long,
        homeworkId: Int,
        answers: Map<Int, String>
    ): HomeworkSubmission {
        val homework = homeworkRepository.findById(homeworkId)
            ?: throw IllegalArgumentException("Homework not found")

        val questions = homework.questions
        var score = 0

        questions.forEach { question ->
            val userAnswer = answers[question.id]?.trim()?.lowercase()
            val correctAnswer = question.correctAnswer.trim().lowercase()
            if (userAnswer == correctAnswer) {
                score++
            }
        }

        val submission = HomeworkSubmission(
            id = 0,
            userId = userId,
            homeworkId = homeworkId,
            answers = answers,
            score = score,
            maxScore = questions.size,
            submittedAt = Clock.System.now()
        )

        val savedSubmission = homeworkRepository.createSubmission(submission)

        if (score == questions.size) {
            val user = userRepository.findById(userId)
            if (user != null) {
                userRepository.updateCurrentLesson(userId, user.currentLessonId + 1)
            }
        }

        return savedSubmission
    }

    suspend fun hasCompletedHomework(userId: Long, homeworkId: Int): Boolean =
        homeworkRepository.hasUserCompletedHomework(userId, homeworkId)

    suspend fun getUserSubmissions(userId: Long): List<HomeworkSubmission> =
        homeworkRepository.findSubmissionsByUser(userId)

    suspend fun createHomework(homework: Homework): Homework =
        homeworkRepository.create(homework)
}
