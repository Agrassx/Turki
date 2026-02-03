package com.turki.core.database

import com.turki.core.domain.UserProgress
import com.turki.core.repository.UserProgressRepository
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class UserProgressRepositoryImpl : UserProgressRepository {
    override suspend fun findByUserAndLesson(userId: Long, lessonId: Int): UserProgress? = DatabaseFactory.dbQuery {
        UserProgressTable.selectAll()
            .where { (UserProgressTable.userId eq userId) and (UserProgressTable.lessonId eq lessonId) }
            .map(::toProgress)
            .singleOrNull()
    }

    override suspend fun upsert(progress: UserProgress): UserProgress = DatabaseFactory.dbQuery {
        val now = Clock.System.now()
        val updated = UserProgressTable.update({
            (UserProgressTable.userId eq progress.userId) and (UserProgressTable.lessonId eq progress.lessonId)
        }) {
            it[status] = progress.status
            it[lastExerciseId] = progress.lastExerciseId
            it[contentVersion] = progress.contentVersion
            it[updatedAt] = now
        }

        if (updated == 0) {
            UserProgressTable.insert {
                it[userId] = progress.userId
                it[lessonId] = progress.lessonId
                it[status] = progress.status
                it[lastExerciseId] = progress.lastExerciseId
                it[contentVersion] = progress.contentVersion
                it[updatedAt] = now
            }
        }

        progress.copy(updatedAt = now)
    }

    override suspend fun countCompleted(userId: Long): Long = DatabaseFactory.dbQuery {
        UserProgressTable.selectAll()
            .where { (UserProgressTable.userId eq userId) and (UserProgressTable.status eq "COMPLETED") }
            .count()
    }

    override suspend fun listCompletedLessonIds(userId: Long): List<Int> = DatabaseFactory.dbQuery {
        UserProgressTable.selectAll()
            .where { (UserProgressTable.userId eq userId) and (UserProgressTable.status eq "COMPLETED") }
            .map { it[UserProgressTable.lessonId].value }
    }

    override suspend fun deleteByUser(userId: Long): Boolean = DatabaseFactory.dbQuery {
        UserProgressTable.deleteWhere { UserProgressTable.userId eq userId } > 0
    }

    private fun toProgress(row: org.jetbrains.exposed.sql.ResultRow): UserProgress = UserProgress(
        userId = row[UserProgressTable.userId].value,
        lessonId = row[UserProgressTable.lessonId].value,
        status = row[UserProgressTable.status],
        lastExerciseId = row[UserProgressTable.lastExerciseId],
        contentVersion = row[UserProgressTable.contentVersion],
        updatedAt = row[UserProgressTable.updatedAt]
    )
}
