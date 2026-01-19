package com.turki.core.database

import com.turki.core.domain.Language
import com.turki.core.domain.User
import com.turki.core.repository.UserRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class UserRepositoryImpl : UserRepository {

    override suspend fun findById(id: Long): User? = DatabaseFactory.dbQuery {
        UsersTable.selectAll().where { UsersTable.id eq id }
            .map(::toUser)
            .singleOrNull()
    }

    override suspend fun findByTelegramId(telegramId: Long): User? = DatabaseFactory.dbQuery {
        UsersTable.selectAll().where { UsersTable.telegramId eq telegramId }
            .map(::toUser)
            .singleOrNull()
    }

    override suspend fun findAll(): List<User> = DatabaseFactory.dbQuery {
        UsersTable.selectAll().map(::toUser)
    }

    override suspend fun findActiveSubscribers(): List<User> = DatabaseFactory.dbQuery {
        UsersTable.selectAll().where { UsersTable.subscriptionActive eq true }
            .map(::toUser)
    }

    override suspend fun create(user: User): User = DatabaseFactory.dbQuery {
        val now = Clock.System.now()
        val id = UsersTable.insert {
            it[telegramId] = user.telegramId
            it[username] = user.username
            it[firstName] = user.firstName
            it[lastName] = user.lastName
            it[language] = user.language.code
            it[subscriptionActive] = user.subscriptionActive
            it[subscriptionExpiresAt] = user.subscriptionExpiresAt
            it[currentLessonId] = user.currentLessonId
            it[createdAt] = now
            it[updatedAt] = now
        }[UsersTable.id].value

        user.copy(id = id, createdAt = now, updatedAt = now)
    }

    override suspend fun update(user: User): User = DatabaseFactory.dbQuery {
        val now = Clock.System.now()
        UsersTable.update({ UsersTable.id eq user.id }) {
            it[username] = user.username
            it[firstName] = user.firstName
            it[lastName] = user.lastName
            it[language] = user.language.code
            it[subscriptionActive] = user.subscriptionActive
            it[subscriptionExpiresAt] = user.subscriptionExpiresAt
            it[currentLessonId] = user.currentLessonId
            it[updatedAt] = now
        }
        user.copy(updatedAt = now)
    }

    override suspend fun updateSubscription(
        userId: Long,
        active: Boolean,
        expiresAt: Instant?
    ): Boolean = DatabaseFactory.dbQuery {
        UsersTable.update({ UsersTable.id eq userId }) {
            it[subscriptionActive] = active
            it[subscriptionExpiresAt] = expiresAt
            it[updatedAt] = Clock.System.now()
        } > 0
    }

    override suspend fun updateCurrentLesson(userId: Long, lessonId: Int): Boolean = DatabaseFactory.dbQuery {
        UsersTable.update({ UsersTable.id eq userId }) {
            it[currentLessonId] = lessonId
            it[updatedAt] = Clock.System.now()
        } > 0
    }

    override suspend fun updateLanguage(userId: Long, language: Language): Boolean = DatabaseFactory.dbQuery {
        UsersTable.update({ UsersTable.id eq userId }) {
            it[UsersTable.language] = language.code
            it[updatedAt] = Clock.System.now()
        } > 0
    }

    override suspend fun delete(id: Long): Boolean = DatabaseFactory.dbQuery {
        UsersTable.deleteWhere { UsersTable.id eq id } > 0
    }

    override suspend fun count(): Long = DatabaseFactory.dbQuery {
        UsersTable.selectAll().count()
    }

    override suspend fun resetProgress(userId: Long): Boolean = DatabaseFactory.dbQuery {
        HomeworkSubmissionsTable.deleteWhere { HomeworkSubmissionsTable.userId eq userId }
        UsersTable.update({ UsersTable.id eq userId }) {
            it[currentLessonId] = 1
            it[updatedAt] = Clock.System.now()
        } > 0
    }

    override suspend fun resetAllProgress(): Boolean = DatabaseFactory.dbQuery {
        HomeworkSubmissionsTable.deleteWhere { Op.TRUE }
        val now = Clock.System.now()
        UsersTable.update({ Op.TRUE }) {
            it[currentLessonId] = 1
            it[updatedAt] = now
        } > 0
    }

    private fun toUser(row: ResultRow): User = User(
        id = row[UsersTable.id].value,
        telegramId = row[UsersTable.telegramId],
        username = row[UsersTable.username],
        firstName = row[UsersTable.firstName],
        lastName = row[UsersTable.lastName],
        language = Language.fromCode(row[UsersTable.language]) ?: Language.RUSSIAN,
        subscriptionActive = row[UsersTable.subscriptionActive],
        subscriptionExpiresAt = row[UsersTable.subscriptionExpiresAt],
        currentLessonId = row[UsersTable.currentLessonId],
        createdAt = row[UsersTable.createdAt],
        updatedAt = row[UsersTable.updatedAt]
    )
}
