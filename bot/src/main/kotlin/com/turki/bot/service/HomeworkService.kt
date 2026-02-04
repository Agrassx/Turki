package com.turki.bot.service

import com.turki.core.domain.Homework
import com.turki.core.domain.HomeworkQuestion
import com.turki.core.domain.HomeworkSubmission
import com.turki.core.domain.QuestionType
import com.turki.core.repository.HomeworkRepository
import com.turki.core.repository.UserRepository
import kotlinx.datetime.Clock

/**
 * Service for managing homework assignments and submissions.
 *
 * This service handles:
 * - Retrieving homework for lessons
 * - Processing homework submissions
 * - Calculating scores
 * - Advancing user progress when homework is completed successfully
 * - Tracking completion status
 *
 * All operations are coroutine-based and thread-safe.
 */
class HomeworkService(
    private val homeworkRepository: HomeworkRepository,
    private val userRepository: UserRepository
) {

    /**
     * Retrieves homework assignment for a specific lesson.
     *
     * @param lessonId The lesson's database ID
     * @return The [Homework] if found, null otherwise
     */
    suspend fun getHomeworkForLesson(lessonId: Int): Homework? =
        homeworkRepository.findByLessonId(lessonId)

    /**
     * Retrieves homework by its database ID.
     *
     * @param homeworkId The homework's database ID
     * @return The [Homework] if found, null otherwise
     */
    suspend fun getHomeworkById(homeworkId: Int): Homework? =
        homeworkRepository.findById(homeworkId)

    /**
     * Retrieves all questions for a homework assignment.
     *
     * @param homeworkId The homework's database ID
     * @return List of [HomeworkQuestion] objects
     */
    suspend fun getHomeworkQuestions(homeworkId: Int): List<HomeworkQuestion> =
        homeworkRepository.findQuestions(homeworkId)

    /**
     * Submits homework answers and calculates the score.
     *
     * This function:
     * - Validates answers against correct answers (case-insensitive)
     * - Calculates the score based on correct answers
     * - Creates a submission record
     * - Advances user to next lesson if all answers are correct
     *
     * @param userId The user's database ID
     * @param homeworkId The homework's database ID
     * @param answers Map of question IDs to user answers
     * @return The created [HomeworkSubmission] with calculated score
     * @throws IllegalArgumentException if homework is not found
     */
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
            if (isAnswerCorrect(question, answers[question.id])) {
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

    /**
     * Checks if a user has already completed a homework assignment.
     *
     * @param userId The user's database ID
     * @param homeworkId The homework's database ID
     * @return true if the user has completed the homework, false otherwise
     */
    suspend fun hasCompletedHomework(userId: Long, homeworkId: Int): Boolean =
        homeworkRepository.hasUserCompletedHomework(userId, homeworkId)

    /**
     * Retrieves all homework submissions for a user.
     *
     * @param userId The user's database ID
     * @return List of [HomeworkSubmission] objects
     */
    suspend fun getUserSubmissions(userId: Long): List<HomeworkSubmission> =
        homeworkRepository.findSubmissionsByUser(userId)

    /**
     * Creates a new homework assignment in the database.
     *
     * @param homework The homework object to create
     * @return The created [Homework] with assigned database ID
     */
    suspend fun createHomework(homework: Homework): Homework =
        homeworkRepository.create(homework)

    fun isAnswerCorrect(question: HomeworkQuestion, answer: String?): Boolean {
        if (answer.isNullOrBlank()) {
            return false
        }
        val normalizedUser = normalizeAnswer(answer)
        val normalizedCorrect = normalizeAnswer(question.correctAnswer)
        if (normalizedUser.isBlank()) {
            return false
        }

        // Exact match first
        if (normalizedUser == normalizedCorrect) {
            return true
        }

        // For text input/translation questions, allow flexible matching
        if (question.questionType == QuestionType.TEXT_INPUT || question.questionType == QuestionType.TRANSLATION) {
            // For "Меня зовут" questions: accept "Benim adım X" as well as "Adım X"
            // Also accept any name if the pattern is correct
            if (normalizedCorrect.startsWith("adım ") || 
                question.questionText.contains("зовут", ignoreCase = true)) {
                // Accept "benim adım X" for "adım X"
                if (normalizedUser.startsWith("benim adım ")) {
                    return true
                }
                // Accept "adım X" for any name (user might use their own name)
                if (normalizedUser.startsWith("adım ")) {
                    return true
                }
            }

            // For questions asking user to start with something, allow prefix matching
            val allowPrefix = question.questionText.contains("начните", ignoreCase = true)
            if (allowPrefix && normalizedUser.startsWith(normalizedCorrect)) {
                return true
            }
        }

        return false
    }

    private fun normalizeAnswer(text: String): String {
        return text.lowercase()
            .replace(Regex("[\\p{P}\\p{S}]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
