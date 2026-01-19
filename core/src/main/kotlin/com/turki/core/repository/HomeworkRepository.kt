package com.turki.core.repository

import com.turki.core.domain.Homework
import com.turki.core.domain.HomeworkQuestion
import com.turki.core.domain.HomeworkSubmission

/**
 * Repository interface for homework data access.
 *
 * This interface defines operations for managing homework assignments, questions,
 * and user submissions. Implementations should provide database persistence
 * using Exposed ORM or similar frameworks.
 *
 * All operations are coroutine-based and should be executed within database transactions.
 */
interface HomeworkRepository {
    /**
     * Finds a homework assignment by its database ID.
     *
     * @param id The homework's database ID
     * @return The [Homework] if found, null otherwise
     */
    suspend fun findById(id: Int): Homework?

    /**
     * Finds a homework assignment by its associated lesson ID.
     *
     * @param lessonId The lesson's database ID
     * @return The [Homework] if found, null otherwise
     */
    suspend fun findByLessonId(lessonId: Int): Homework?

    /**
     * Retrieves all questions for a homework assignment.
     *
     * @param homeworkId The homework's database ID
     * @return List of [HomeworkQuestion] objects
     */
    suspend fun findQuestions(homeworkId: Int): List<HomeworkQuestion>

    /**
     * Creates a new homework assignment in the database.
     *
     * @param homework The homework object to create
     * @return The created [Homework] with assigned database ID
     */
    suspend fun create(homework: Homework): Homework

    /**
     * Creates a new homework submission in the database.
     *
     * @param submission The submission object to create
     * @return The created [HomeworkSubmission] with assigned database ID
     */
    suspend fun createSubmission(submission: HomeworkSubmission): HomeworkSubmission

    /**
     * Retrieves all homework submissions for a user.
     *
     * @param userId The user's database ID
     * @return List of [HomeworkSubmission] objects
     */
    suspend fun findSubmissionsByUser(userId: Long): List<HomeworkSubmission>

    /**
     * Checks if a user has completed a specific homework assignment.
     *
     * A homework is considered completed if the user has at least one submission
     * with a perfect score (score == maxScore).
     *
     * @param userId The user's database ID
     * @param homeworkId The homework's database ID
     * @return true if the user has completed the homework, false otherwise
     */
    suspend fun hasUserCompletedHomework(userId: Long, homeworkId: Int): Boolean
}
