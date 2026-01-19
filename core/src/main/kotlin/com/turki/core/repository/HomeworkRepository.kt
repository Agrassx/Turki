package com.turki.core.repository

import com.turki.core.domain.Homework
import com.turki.core.domain.HomeworkQuestion
import com.turki.core.domain.HomeworkSubmission

interface HomeworkRepository {
    suspend fun findById(id: Int): Homework?
    suspend fun findByLessonId(lessonId: Int): Homework?
    suspend fun findQuestions(homeworkId: Int): List<HomeworkQuestion>
    suspend fun create(homework: Homework): Homework
    suspend fun createSubmission(submission: HomeworkSubmission): HomeworkSubmission
    suspend fun findSubmissionsByUser(userId: Long): List<HomeworkSubmission>
    suspend fun hasUserCompletedHomework(userId: Long, homeworkId: Int): Boolean
}
